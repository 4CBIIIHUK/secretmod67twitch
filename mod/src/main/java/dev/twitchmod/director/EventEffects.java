package dev.twitchmod.director;

import dev.twitchmod.TwitchMod;
import dev.twitchmod.log.EditorLog;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.progress.StoryProgress;
import dev.twitchmod.rules.CombatPermissions;
import dev.twitchmod.rules.TempBlockManager;
import dev.twitchmod.state.RunState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Every action type named by the catalogue has exactly one implementation here.
 * Effects are staged, not destructive: meteors hurt the player but never gouge
 * permanent craters, 100 viewers are decorative figures without 100 server AIs,
 * and every leftover is removed by the event context cleanup.
 */
public final class EventEffects {
    private static final Random RNG = new Random();
    private static final Set<UUID> TRACKED = new HashSet<>();

    private interface Handler {
        void run(EventContext ctx, EventDefinition.Action a);
    }

    private static final Map<String, Handler> REGISTRY = new HashMap<>();

    private EventEffects() {
    }

    static {
        reg("spawn_wave", EventEffects::spawnWave);
        reg("chase", EventEffects::spawnWave);
        reg("spawn_passive", EventEffects::spawnPassive);
        reg("spawn_dummy", EventEffects::spawnDummies);
        reg("crowd", EventEffects::crowd);
        reg("doppel", EventEffects::doppel);
        reg("meteor", EventEffects::meteor);
        reg("grant_combat", EventEffects::grantCombat);
        reg("gift", EventEffects::gift);
        reg("reward", EventEffects::reward);
        reg("particles", EventEffects::particles);
        reg("sound", EventEffects::sound);
        reg("status", EventEffects::status);
        reg("weather", EventEffects::weather);
        reg("time", EventEffects::time);
        reg("temp_platform", EventEffects::platform);
        reg("gap", EventEffects::platform);
        reg("beacon", EventEffects::beacon);
        reg("spawn_chest", EventEffects::spawnChest);
        reg("require_item", EventEffects::requireItem);
        reg("world_damage_note", EventEffects::worldDamageNote);
        reg("choice", EventEffects::choice);
        reg("risk_safe", EventEffects::riskSafe);
        reg("rumor", EventEffects::rumor);
        reg("quiz", EventEffects::quiz);
        reg("appeal", EventEffects::appeal);
        reg("probation", EventEffects::probation);
        reg("attention_delta", EventEffects::attentionDelta);
        reg("story", EventEffects::story);
        reg("secret", EventEffects::secret);
        reg("atmosphere", EventEffects::atmosphere);
        reg("silence", EventEffects::silence);
        reg("glitch", EventEffects::glitch);
        reg("fog", EventEffects::fog);
        reg("camera", EventEffects::camera);
        reg("dejavu", EventEffects::dejavu);
        reg("roulette", EventEffects::roulette);
        reg("decoy", EventEffects::decoy);
        reg("fake_explosion", EventEffects::fakeExplosion);
        reg("gust", EventEffects::gust);
        reg("rockfall", EventEffects::rockfall);
        reg("ore_vein", EventEffects::oreVein);
    }

    private static void reg(String type, Handler h) {
        REGISTRY.put(type, h);
    }

    public static boolean isKnown(String type) {
        return REGISTRY.containsKey(type);
    }

    public static boolean isTracked(Entity e) {
        return e != null && TRACKED.contains(e.getUuid());
    }

    /** Called by the action guard when a permitted hit lands on an event entity. */
    public static void noteCombatHit(Entity target) {
        if (isTracked(target)) EventMetrics.onCombatHit();
    }

    public static void clearTracked() {
        TRACKED.clear();
    }

    // ----------------------------------------------------------------- start
    public static void start(EventContext ctx) {
        for (EventDefinition.Action a : ctx.def.actions) fire(ctx, a);
    }

    public static void fire(EventContext ctx, EventDefinition.Action a) {
        Handler h = REGISTRY.get(a.type);
        if (h == null) {
            TwitchMod.LOGGER.warn("[twitchmod] no effect '{}'", a.type);
            return;
        }
        try {
            h.run(ctx, a);
        } catch (Exception e) {
            TwitchMod.LOGGER.warn("[twitchmod] effect '{}' failed: {}", a.type, e.toString());
        }
    }

    // ------------------------------------------------------------------ mobs
    private static void spawnWave(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        String mobId = a.s.isEmpty() ? "minecraft:zombie" : a.s;
        int count = Math.max(1, a.a);
        for (int i = 0; i < count; i++) {
            BlockPos at = ground(w, ctx.player, 6 + RNG.nextInt(6));
            if (at == null) continue;
            Entity e = spawn(w, mobId, at);
            if (e == null) continue;
            if (a.b > 0 && e instanceof LivingEntity le) {
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 1));
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60, 0));
            }
            if (e instanceof MobEntity mob) {
                mob.setPersistent();
                mob.setTarget(ctx.player);
            }
            ctx.spawned.add(e);
            TRACKED.add(e.getUuid());
        }
        banner(ctx.player, 0, "tm.event.start", 3, 0);
    }

    private static void spawnPassive(EventContext ctx, EventDefinition.Action a) {
        for (int i = 0; i < a.a; i++) {
            BlockPos at = ground(ctx.world, ctx.player, 4 + RNG.nextInt(8));
            if (at == null) continue;
            Entity e = spawn(ctx.world, a.s, at);
            if (e != null) ctx.spawned.add(e);
        }
    }

    private static void spawnDummies(EventContext ctx, EventDefinition.Action a) {
        for (int i = 0; i < a.a; i++) {
            BlockPos at = ground(ctx.world, ctx.player, 5 + RNG.nextInt(4));
            if (at == null) continue;
            ArmorStandEntity stand = new ArmorStandEntity(ctx.world, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Text.literal("ЦЕЛЬ " + (i + 1)));
            ctx.world.spawnEntity(stand);
            ctx.spawned.add(stand);
            TRACKED.add(stand.getUuid());
        }
        banner(ctx.player, 2, "tm.crosshair.mobok", ctx.def.durationSec + 10, 0);
    }

    /** 100 viewers: decorative figures only, the hunter count is shown honestly. */
    private static void crowd(EventContext ctx, EventDefinition.Action a) {
        int viewers = a.a;
        int hunters = a.b;
        ServerWorld w = ctx.world;
        int decor = Math.max(0, viewers - hunters);
        for (int i = 0; i < decor; i++) {
            double ang = (Math.PI * 2 * i) / decor;
            double r = 8 + (i % 5) * 1.6D;
            double x = ctx.player.getX() + Math.cos(ang) * r;
            double z = ctx.player.getZ() + Math.sin(ang) * r;
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z);
            ArmorStandEntity stand = new ArmorStandEntity(w, x, y, z);
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            stand.setShowArms(true);
            stand.setYaw((float) (Math.toDegrees(-ang) + 90));
            stand.setCustomName(Text.literal("ЗРИТЕЛЬ"));
            w.spawnEntity(stand);
            ctx.spawned.add(stand);
        }
        for (int i = 0; i < hunters; i++) {
            BlockPos at = ground(w, ctx.player, 7 + RNG.nextInt(6));
            if (at == null) continue;
            Entity e = spawn(w, "minecraft:zombie", at);
            if (e instanceof MobEntity mob) {
                mob.setPersistent();
                mob.setTarget(ctx.player);
            }
            if (e != null) {
                ctx.spawned.add(e);
                TRACKED.add(e.getUuid());
            }
        }
        banner(ctx.player, 0, "tm.crowd.hud", ctx.def.durationSec, viewers, hunters);
    }

    private static void doppel(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        ArmorStandEntity stand = new ArmorStandEntity(w, ctx.player.getX(), ctx.player.getY(), ctx.player.getZ());
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setCustomName(Text.literal("ДВОЙНИК"));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack copy = ctx.player.getEquippedStack(slot).copy();
            if (!copy.isEmpty()) stand.equipStack(slot, copy);
        }
        w.spawnEntity(stand);
        ctx.spawned.add(stand);
        Deque<Vec3d> history = new ArrayDeque<>();
        int samples = 7; // 1.5 s replay lag
        Scheduler.repeat(ctx, 5, () -> {
            history.addLast(ctx.player.getPos());
            while (history.size() > samples) {
                Vec3d replay = history.pollFirst();
                if (replay != null) {
                    stand.updatePositionAndAngles(replay.x, replay.y, replay.z, ctx.player.getYaw(), 0f);
                }
            }
            w.spawnParticles(ParticleTypes.PORTAL, stand.getX(), stand.getY() + 1, stand.getZ(), 4, 0.2, 0.4, 0.2, 0.01);
        });
        Scheduler.after(ctx, Math.max(40, a.a * 20 - 20), () -> {
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, stand.getX(), stand.getY() + 1, stand.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            w.playSound(null, ctx.player.getBlockPos(), sound("entity.enderman.teleport"), SoundCategory.MASTER, 1f, 1.4f);
            stand.discard();
            ctx.spawned.remove(stand);
        });
    }

    private static void meteor(EventContext ctx, EventDefinition.Action a) {
        int count = Math.max(1, a.a);
        int interval = Math.max(4, a.b);
        ServerWorld w = ctx.world;
        int telegraph = Math.max(8, ctx.def.telegraphSec * 20);
        for (int i = 0; i < count; i++) {
            double ang = RNG.nextDouble() * Math.PI * 2;
            double r = 5 + RNG.nextDouble() * 10;
            final double x = ctx.player.getX() + Math.cos(ang) * r;
            final double z = ctx.player.getZ() + Math.sin(ang) * r;
            final int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z);
            final int at = i * interval;
            // Telegraph: landing points are always visible before any hit.
            for (int k = 1; k <= telegraph / 3; k++) {
                final int step = k;
                Scheduler.after(ctx, at + step * 3, () -> {
                    for (int h = 1; h < 14; h++) {
                        w.spawnParticles(ParticleTypes.FLAME, x, y + h * 1.4D, z, 2, 0.08, 0.08, 0.08, 0);
                    }
                    w.spawnParticles(ParticleTypes.END_ROD, x, y + 0.4, z, 5, 0.6, 0.1, 0.6, 0.01);
                });
            }
            Scheduler.after(ctx, at + telegraph, () -> impact(ctx, w, x, y, z));
        }
    }

    private static void impact(EventContext ctx, ServerWorld w, double x, int y, double z) {
        w.playSound(null, new BlockPos((int) x, y, (int) z), sound("entity.generic.explode"), SoundCategory.MASTER, 1f, 1.1f);
        w.spawnParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, 2, 0.2, 0.2, 0.2, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos((int) x + dx, y, (int) z + dz);
                BlockState st = w.getBlockState(p);
                if (st.isAir() || !st.isOpaqueFullCube(w, p)) {
                    TempBlockManager.place(w, p, Blocks.BLACKSTONE.getDefaultState(), 12);
                }
            }
        }
        Vec3d hit = new Vec3d(x, y, z);
        if (ctx.player.getPos().squaredDistanceTo(hit) < 9.0D && !ctx.player.isCreative()) {
            ctx.player.damage(w.getDamageSources().generic(), 3.0F);
            ctx.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0));
        }
        for (Entity e : ctx.aliveSpawned()) {
            if (e instanceof LivingEntity le && le.squaredDistanceTo(hit) < 9.0D) {
                le.damage(w.getDamageSources().generic(), 6.0F);
            }
        }
    }

    // ----------------------------------------------------------------- perks
    private static void grantCombat(EventContext ctx, EventDefinition.Action a) {
        String target = a.s.isEmpty() ? "any" : a.s;
        CombatPermissions.grant(ctx.state, target, ctx.def.title, a.a, false);
        ctx.combatPermitted = true;
        banner(ctx.player, 2, "tm.crosshair.mobok", Math.min(6, a.a), 0);
    }

    private static void gift(EventContext ctx, EventDefinition.Action a) {
        deliver(ctx, Rewards.roll(a.a, a.b), a.a);
        EditorLog.reward(ctx.def.id, ctx.def.rewardTable, ctx.state.activeSeconds());
    }

    private static void reward(EventContext ctx, EventDefinition.Action a) {
        deliver(ctx, Rewards.roll(a.a, RNG.nextInt(15)), a.a);
        EditorLog.reward(ctx.def.id, ctx.def.rewardTable, ctx.state.activeSeconds());
    }

    private static void deliver(EventContext ctx, List<ItemStack> items, int tier) {
        if (items == null || items.isEmpty()) return;
        switch (Math.floorMod(tier, 3)) {
            case 0 -> {
                for (ItemStack st : items) ctx.player.getInventory().insertStack(st.copy());
                ctx.player.playerScreenHandler.sendContentUpdates();
            }
            case 1 -> {
                for (ItemStack st : items) {
                    ItemEntity e = new ItemEntity(ctx.world, ctx.player.getX(), ctx.player.getY() + 0.8, ctx.player.getZ(), st.copy());
                    ctx.world.spawnEntity(e);
                    ctx.spawned.add(e);
                }
            }
            default -> {
                for (ItemStack st : items) ctx.player.getInventory().insertStack(st.copy());
                ctx.player.getInventory().insertStack(new ItemStack(Items.GOLDEN_APPLE, 1));
                ctx.world.playSound(null, ctx.player.getBlockPos(), sound("entity.player.levelup"), SoundCategory.MASTER, 0.8f, 1.2f);
            }
        }
        ctx.player.playerScreenHandler.sendContentUpdates();
    }

    private static void particles(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        int n = Math.max(5, Math.min(40, a.a));
        Scheduler.repeat(ctx, 8, () -> w.spawnParticles(ParticleTypes.ENCHANT, ctx.player.getX(),
                ctx.player.getY() + 1.2, ctx.player.getZ(), n, 1.4, 1.0, 1.4, 0.02));
    }

    private static void sound(EventContext ctx, EventDefinition.Action a) {
        ctx.world.playSound(null, ctx.player.getBlockPos(), sound(a.s), SoundCategory.MASTER, 1f, 1f);
    }

    private static void status(EventContext ctx, EventDefinition.Action a) {
        RegistryEntry<StatusEffect> entry = entry(a.s.isEmpty() ? "minecraft:slow_falling" : a.s);
        if (entry != null) ctx.player.addStatusEffect(new StatusEffectInstance(entry, Math.max(20, a.a * 20), 0));
    }

    private static void weather(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        if (a.s.endsWith("clear")) {
            w.setWeather(0, 120 * 20, false, false);
        } else {
            w.setWeather(0, Math.max(20, a.a) * 20, true, true);
        }
    }

    private static void time(EventContext ctx, EventDefinition.Action a) {
        try {
            ctx.world.setTimeOfDay(Long.parseLong(a.s));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void platform(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        int r = Math.max(2, a.a);
        BlockPos base = ctx.player.getBlockPos().down();
        int ttl = Math.max(10, a.b);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                BlockPos p = base.add(dx, 0, dz);
                if (w.getBlockState(p).isAir()) {
                    TempBlockManager.place(w, p, Blocks.SMOOTH_STONE.getDefaultState(), ttl);
                }
            }
        }
    }

    private static void beacon(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        Vec3d target = ctx.targetPos == null ? ctx.player.getPos() : ctx.targetPos;
        int ttl = Math.max(20, a.b);
        Scheduler.repeat(ctx, 10, () -> {
            for (int y = 0; y < 22; y++) {
                w.spawnParticles(ParticleTypes.END_ROD, target.x, target.y + y, target.z, 3, 0.15, 0.15, 0.15, 0);
            }
            if (ctx.state.activeTicks > ctx.startTick + ttl) Scheduler.cancel(ctx);
        });
    }

    private static void spawnChest(EventContext ctx, EventDefinition.Action a) {
        BlockPos at = ground(ctx.world, ctx.player, Math.max(5, a.a));
        if (at == null) return;
        ctx.world.setBlockState(at, Blocks.CHEST.getDefaultState());
        if (ctx.world.getBlockEntity(at) instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (ItemStack st : Rewards.roll(0, 2)) {
                chest.setStack(Math.min(slot++, chest.size() - 1), st);
            }
            chest.markDirty();
        }
    }

    private static void requireItem(EventContext ctx, EventDefinition.Action a) {
        Item item = Registries.ITEM.get(Identifier.of(a.s));
        int have = 0;
        for (ItemStack st : ctx.player.getInventory().main) {
            if (st.getItem() == item) have += st.getCount();
        }
        if (have < a.a) {
            ctx.player.getInventory().insertStack(new ItemStack(item, a.a - have));
            ctx.player.playerScreenHandler.sendContentUpdates();
        }
    }

    private static void worldDamageNote(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 1, "tm.worlddamage.warn", 4, a.a);
    }

    // --------------------------------------------------------------- choices
    private static void choice(EventContext ctx, EventDefinition.Action a) {
        String id = a.s.isEmpty() ? ctx.def.id : a.s;
        List<String> options = new ArrayList<>();
        List<Integer> percents = new ArrayList<>();
        if (a.b == 1) {
            options.add("tm.choice.opt.hype");
            options.add("tm.choice.opt.calm");
            percents.add(60);
            percents.add(40);
        } else {
            fillVoteOptions(id, options, percents);
        }
        ModNet.sendChoice(ctx.player, id, ctx.def.title, true, Math.max(8, a.a - 4), options, percents);
        Choices.offer(id, index -> {
            applyVote(ctx, id, index);
            ctx.succeed();
        });
        Scheduler.after(ctx, Math.max(8, a.a) * 20, () -> {
            if (ctx.result != EventContext.RUNNING) return;
            banner(ctx.player, 1, "tm.choice.timeout", 3, 0);
            applyVote(ctx, id, 1);
            ctx.succeed();
        });
    }

    private static void fillVoteOptions(String id, List<String> options, List<Integer> percents) {
        switch (id) {
            case "chat_vote_gear" -> {
                options.add("tm.choice.opt.armor");
                options.add("tm.choice.opt.food");
                percents.add(52);
                percents.add(48);
            }
            case "chat_vote_weather", "chat_vote_night" -> {
                options.add("tm.choice.opt.clear");
                options.add("tm.choice.opt.storm");
                percents.add(65);
                percents.add(35);
            }
            case "chat_vote_mobs" -> {
                options.add("tm.choice.opt.wave");
                options.add("tm.choice.opt.silence");
                percents.add(70);
                percents.add(30);
            }
            case "chat_vote_loot" -> {
                options.add("tm.choice.opt.rare");
                options.add("tm.choice.opt.xp");
                percents.add(45);
                percents.add(55);
            }
            case "chat_vote_route" -> {
                options.add("tm.choice.opt.short");
                options.add("tm.choice.opt.safe");
                percents.add(58);
                percents.add(42);
            }
            case "chat_vote_music" -> {
                options.add("tm.choice.opt.music");
                options.add("tm.choice.opt.silence");
                percents.add(62);
                percents.add(38);
            }
            default -> {
                options.add("tm.choice.opt.meteors");
                options.add("tm.choice.opt.fog");
                percents.add(50);
                percents.add(50);
            }
        }
    }

    private static void applyVote(EventContext ctx, String id, int index) {
        ServerWorld w = ctx.world;
        switch (id) {
            case "chat_vote_gear" -> deliver(ctx, index == 0 ? Rewards.armorSet() : Rewards.foodBasket(), 0);
            case "chat_vote_weather", "chat_vote_night" -> {
                if (index == 0) w.setWeather(0, 120 * 20, false, false);
                else w.setWeather(0, 60 * 20, true, true);
            }
            case "chat_vote_mobs" -> {
                if (index == 0) {
                    CombatPermissions.grant(ctx.state, "minecraft:zombie", ctx.def.title, 40, false);
                    for (int i = 0; i < 5; i++) {
                        BlockPos at = ground(w, ctx.player, 8);
                        Entity e = at == null ? null : spawn(w, "minecraft:zombie", at);
                        if (e != null) {
                            ctx.spawned.add(e);
                            TRACKED.add(e.getUuid());
                        }
                    }
                } else {
                    w.playSound(null, ctx.player.getBlockPos(), sound("block.note_block.pling"), SoundCategory.MASTER, 1f, 1f);
                }
            }
            case "chat_vote_loot" -> deliver(ctx, index == 0 ? Rewards.roll(1, RNG.nextInt(15)) : Rewards.roll(0, 2), 0);
            case "chat_vote_route" -> banner(ctx.player, 1, "tm.story.stronghold", 5, index == 0 ? 300 : 800);
            case "chat_vote_music" -> banner(ctx.player, 1, index == 0 ? "tm.choice.opt.music" : "tm.choice.opt.silence", 3, 0);
            default -> {
                if (index == 0) meteor(ctx, new EventDefinition.Action("meteor", 5, 20, ""));
                else fog(ctx, new EventDefinition.Action("fog", 30, 2, ""));
            }
        }
    }

    private static void riskSafe(EventContext ctx, EventDefinition.Action a) {
        ModNet.sendChoice(ctx.player, ctx.def.id, ctx.def.title, false, 15,
                List.of("tm.choice.opt.risk", "tm.choice.opt.safe"), List.of(50, 50));
        Choices.offer(ctx.def.id, index -> {
            if (index == 0) {
                CombatPermissions.grant(ctx.state, "any", ctx.def.title, 50, false);
                for (int i = 0; i < 6; i++) {
                    BlockPos at = ground(ctx.world, ctx.player, 8);
                    Entity e = at == null ? null : spawn(ctx.world, "minecraft:zombie", at);
                    if (e != null) {
                        ctx.spawned.add(e);
                        TRACKED.add(e.getUuid());
                    }
                }
                banner(ctx.player, 0, "tm.choice.opt.risk", 3, 0);
                Scheduler.after(ctx, 40 * 20, () -> deliver(ctx, Rewards.roll(2, 3), 1));
            } else {
                deliver(ctx, Rewards.roll(0, RNG.nextInt(15)), 0);
                banner(ctx.player, 1, "tm.choice.opt.safe", 3, 0);
                ctx.succeed();
            }
        });
    }

    private static void rumor(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, a.b == 1 ? 2 : 3, "tm." + a.s, 6, 0);
    }

    private static void quiz(EventContext ctx, EventDefinition.Action a) {
        ModNet.sendChoice(ctx.player, ctx.def.id, ctx.def.title, false, 20,
                List.of("tm.quiz.a", "tm.quiz.b", "tm.quiz.c"), List.of(33, 33, 34));
        Choices.offer(ctx.def.id, index -> {
            if (index == 0) {
                deliver(ctx, Rewards.roll(0, 4), 0);
                banner(ctx.player, 2, "tm.event.success", 4, 0);
            } else {
                // A mistake costs the bonus only — never a strike.
                attentionDelta(ctx, new EventDefinition.Action("attention_delta", -10, 0, ""));
                banner(ctx.player, 3, "tm.event.fail", 4, 0);
            }
            ctx.succeed();
        });
    }

    private static void appeal(EventContext ctx, EventDefinition.Action a) {
        RunState s = ctx.state;
        if (s.strikesCurrent < 2) {
            banner(ctx.player, 1, "tm.mod.appeal.unavailable", 4, s.strikesCurrent);
            ctx.succeed();
            return;
        }
        ModNet.sendChoice(ctx.player, ctx.def.id, ctx.def.title, false, 15,
                List.of("tm.choice.opt.yes", "tm.choice.opt.no"), List.of(50, 50));
        Choices.offer(ctx.def.id, index -> {
            if (index != 0) {
                ctx.succeed();
                return;
            }
            CombatPermissions.grant(s, "any", "Апелляция", 55, false);
            for (int i = 0; i < 6; i++) {
                BlockPos at = ground(ctx.world, ctx.player, 9);
                Entity e = at == null ? null : spawn(ctx.world, "minecraft:zombie", at);
                if (e != null) {
                    ctx.spawned.add(e);
                    TRACKED.add(e.getUuid());
                }
            }
            banner(ctx.player, 0, "tm.mod.appeal.trial", 5, 45);
            Scheduler.after(ctx, 45 * 20, () -> {
                if (ctx.result == EventContext.SUCCESS && s.strikesCurrent > 0) {
                    s.strikesCurrent--;
                    s.amnestiesUsed++;
                    banner(ctx.player, 2, "tm.mod.appeal.granted", 6, s.strikesCurrent);
                }
                ctx.succeed();
            });
        });
    }

    private static void probation(EventContext ctx, EventDefinition.Action a) {
        ctx.state.probationActive = true;
        ctx.state.probationEndTick = ctx.state.activeTicks + Math.max(20, a.a) * 20L;
        banner(ctx.player, 1, "tm.mod.probation", 5, a.a);
    }

    private static void attentionDelta(EventContext ctx, EventDefinition.Action a) {
        ctx.state.attention = clamp(ctx.state.attention + a.a);
    }

    private static void story(EventContext ctx, EventDefinition.Action a) {
        StoryProgress.handle(ctx.player, ctx.world, a.s);
    }

    private static void secret(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 3, "tm." + a.s, 8, 0);
        ctx.world.playSound(null, ctx.player.getBlockPos(), sound("block.amethyst_block.chime"), SoundCategory.MASTER, 1f, 0.6f);
    }

    // ------------------------------------------------------------ atmosphere
    private static void atmosphere(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 1, "tm.atmos." + a.s, Math.min(6, a.a), 0);
    }

    private static void silence(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 1, "tm.atmos_silence", Math.min(6, a.a), 0);
    }

    private static void glitch(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 3, "tm.atmos_glitch", Math.min(6, a.a), 0);
    }

    private static void fog(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 1, "tm.atmos_fog_dense", Math.min(6, a.a), a.b);
    }

    private static void camera(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 1, "tm.atmos_camera", Math.min(6, a.a), 0);
    }

    private static void dejavu(EventContext ctx, EventDefinition.Action a) {
        banner(ctx.player, 3, "tm.atmos_dejavu", Math.min(6, a.a), 0);
    }

    private static void roulette(EventContext ctx, EventDefinition.Action a) {
        List<RegistryEntry<StatusEffect>> pool = List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST,
                StatusEffects.SLOW_FALLING, StatusEffects.FIRE_RESISTANCE, StatusEffects.NIGHT_VISION, StatusEffects.WEAKNESS);
        ctx.player.addStatusEffect(new StatusEffectInstance(pool.get(RNG.nextInt(pool.size())), Math.max(20, a.a * 20), 0));
    }

    private static void decoy(EventContext ctx, EventDefinition.Action a) {
        ctx.world.playSound(null, ctx.player.getBlockPos(), sound("entity.creeper.primed"), SoundCategory.MASTER, 1f, 1.3f);
        Scheduler.after(ctx, 25, () -> ctx.world.playSound(null, ctx.player.getBlockPos(),
                sound("entity.creeper.hurt"), SoundCategory.MASTER, 1f, 0.7f));
    }

    private static void fakeExplosion(EventContext ctx, EventDefinition.Action a) {
        // Strength 0: sound and smoke only, no block damage, no player damage.
        ctx.world.createExplosion(null, ctx.player.getX(), ctx.player.getY(), ctx.player.getZ(), 0.0F, false,
                ServerWorld.ExplosionSourceType.NONE);
        ctx.world.spawnParticles(ParticleTypes.POOF, ctx.player.getX(), ctx.player.getY() + 1, ctx.player.getZ(), 30, 1.2, 0.6, 1.2, 0.02);
    }

    private static void gust(EventContext ctx, EventDefinition.Action a) {
        Vec3d dir = ctx.player.getRotationVec(1.0F).normalize().multiply(-1.2D);
        ctx.player.addVelocity(dir.x, 0.4D, dir.z);
        ctx.player.velocityModified = true;
    }

    private static void rockfall(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        for (int i = 0; i < 8; i++) {
            BlockPos at = ground(w, ctx.player, 3 + RNG.nextInt(6));
            if (at == null) continue;
            TempBlockManager.place(w, at.up(), Blocks.COBBLESTONE.getDefaultState(), 15);
            w.playSound(null, at, sound("block.stone.place"), SoundCategory.MASTER, 1f, 0.8f);
        }
    }

    private static void oreVein(EventContext ctx, EventDefinition.Action a) {
        ServerWorld w = ctx.world;
        int placed = 0;
        for (int i = 0; i < 40 && placed < 4; i++) {
            BlockPos p = ctx.player.getBlockPos().add(RNG.nextInt(9) - 4, RNG.nextInt(5) - 3, RNG.nextInt(9) - 4);
            if (w.getBlockState(p).isOf(Blocks.STONE)) {
                w.setBlockState(p, RNG.nextBoolean() ? Blocks.IRON_ORE.getDefaultState() : Blocks.COAL_ORE.getDefaultState(), 3);
                placed++;
            }
        }
        if (placed == 0) deliver(ctx, Rewards.roll(0, 6), 0);
    }

    // --------------------------------------------------------------- helpers
    private static BlockPos ground(ServerWorld w, ServerPlayerEntity player, int radius) {
        for (int tries = 0; tries < 12; tries++) {
            double ang = RNG.nextDouble() * Math.PI * 2;
            double r = radius * (0.6 + RNG.nextDouble() * 0.7);
            int x = (int) (player.getX() + Math.cos(ang) * r);
            int z = (int) (player.getZ() + Math.sin(ang) * r);
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (w.getBlockState(at.down()).isAir() || w.getBlockState(at.down()).isLiquid()) continue;
            if (w.getBlockState(at).isLiquid()) continue;
            return at;
        }
        return null;
    }

    private static Entity spawn(ServerWorld w, String id, BlockPos at) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(asId(id));
        if (type == null) return null;
        Entity e = type.create(w);
        if (e == null) return null;
        e.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
        w.spawnEntity(e);
        return e;
    }

    private static void banner(ServerPlayerEntity p, int kind, String key, int seconds, int... args) {
        ModNet.sendBanner(p, kind, key, seconds, args);
    }

    /** Sound events are created by id so no vanilla constant type can break the build. */
    private static SoundEvent sound(String path) {
        String id = path == null || path.isEmpty() ? "minecraft:ambient.cave" : (path.contains(":") ? path : "minecraft:" + path);
        return SoundEvent.of(Identifier.of(id));
    }

    private static RegistryEntry<StatusEffect> entry(String id) {
        Identifier key = Identifier.of(id.contains(":") ? id : "minecraft:" + id);
        return Registries.STATUS_EFFECT.getEntry(key).orElse(null);
    }

    public static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /** Reward tables: tier 0 common, 1 rare, 2 legendary, 15 delivery variants. */
    static final class Rewards {
        static List<ItemStack> roll(int tier, int method) {
            int m = Math.floorMod(method, 15);
            return switch (tier) {
                case 0 -> common(m);
                case 1 -> rare(m);
                default -> legendary(m);
            };
        }

        static List<ItemStack> common(int m) {
            return switch (m) {
                case 0 -> List.of(new ItemStack(Items.BREAD, 6), new ItemStack(Items.TORCH, 16));
                case 1 -> List.of(new ItemStack(Items.COOKIE, 12), new ItemStack(Items.STICK, 8));
                case 2 -> List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
                case 3 -> List.of(new ItemStack(Items.IRON_INGOT, 3));
                case 4 -> List.of(new ItemStack(Items.LEATHER, 4));
                case 5 -> List.of(new ItemStack(Items.COOKED_BEEF, 8));
                case 6 -> List.of(new ItemStack(Items.COAL, 10));
                case 7 -> List.of(new ItemStack(Items.LAPIS_LAZULI, 6));
                case 8 -> List.of(new ItemStack(Items.POTION, 1));
                case 9 -> List.of(new ItemStack(Items.MINECART, 1));
                case 10 -> List.of(new ItemStack(Items.OAK_PLANKS, 16));
                case 11 -> List.of(new ItemStack(Items.TORCH, 32));
                case 12 -> List.of(new ItemStack(Items.ENDER_PEARL, 1));
                case 13 -> List.of(new ItemStack(Items.ARROW, 24));
                default -> List.of(new ItemStack(Items.BUNDLE, 1));
            };
        }

        static List<ItemStack> rare(int m) {
            return switch (m) {
                case 0 -> List.of(new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_BOOTS));
                case 1 -> List.of(new ItemStack(Items.IRON_INGOT, 12), new ItemStack(Items.GOLD_INGOT, 4));
                case 2 -> List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 24));
                case 3 -> List.of(new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SWORD));
                case 4 -> List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_LEGGINGS));
                case 5 -> List.of(new ItemStack(Items.GOLDEN_CARROT, 12));
                case 6 -> List.of(new ItemStack(Items.DIAMOND, 2));
                case 7 -> List.of(new ItemStack(Items.ENCHANTING_TABLE));
                case 8 -> List.of(new ItemStack(Items.POTION, 2));
                case 9 -> List.of(new ItemStack(Items.SADDLE));
                case 10 -> List.of(new ItemStack(Items.WHITE_BED), new ItemStack(Items.OAK_LOG, 16));
                case 11 -> List.of(new ItemStack(Items.LANTERN, 12));
                case 12 -> List.of(new ItemStack(Items.ENDER_PEARL, 4));
                case 13 -> List.of(new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 32));
                default -> List.of(new ItemStack(Items.SHULKER_BOX));
            };
        }

        static List<ItemStack> legendary(int m) {
            return switch (m) {
                case 0 -> List.of(new ItemStack(Items.DIAMOND_CHESTPLATE));
                case 1 -> List.of(new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.EMERALD, 8));
                case 2 -> List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 48));
                case 3 -> List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SWORD));
                case 4 -> armorSet();
                case 5 -> List.of(new ItemStack(Items.GOLDEN_APPLE, 4));
                case 6 -> List.of(new ItemStack(Items.ENDER_EYE, 3), new ItemStack(Items.BLAZE_ROD, 2));
                case 7 -> List.of(new ItemStack(Items.ANVIL), new ItemStack(Items.ENCHANTED_BOOK, 2));
                case 8 -> List.of(new ItemStack(Items.POTION, 3));
                case 9 -> List.of(new ItemStack(Items.ELYTRA));
                case 10 -> List.of(new ItemStack(Items.OBSIDIAN, 8), new ItemStack(Items.CRYING_OBSIDIAN, 2));
                case 11 -> List.of(new ItemStack(Items.SEA_LANTERN, 16));
                case 12 -> List.of(new ItemStack(Items.ENDER_PEARL, 8));
                case 13 -> List.of(new ItemStack(Items.CROSSBOW), new ItemStack(Items.ARROW, 64));
                default -> List.of(new ItemStack(Items.NETHERITE_INGOT));
            };
        }

        static List<ItemStack> armorSet() {
            return List.of(new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE),
                    new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS));
        }

        static List<ItemStack> foodBasket() {
            return List.of(new ItemStack(Items.COOKED_BEEF, 16), new ItemStack(Items.BREAD, 12), new ItemStack(Items.APPLE, 6));
        }
    }
}
BREAD, 12), new ItemStack(Items.APPLE, 6));
        }
    }
}
