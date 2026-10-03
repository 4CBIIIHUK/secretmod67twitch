package dev.twitchmod.rules;

import dev.twitchmod.state.RunState;
import dev.twitchmod.state.RunState.CombatPermission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;

import java.util.Iterator;

/**
 * Temporary combat permissions. Nothing is hidden: every grant is pushed to the
 * HUD with its label and remaining seconds, before the fight starts.
 */
public final class CombatPermissions {
    private CombatPermissions() {
    }

    public static void tick(RunState s) {
        Iterator<CombatPermission> it = s.combatPermissions.iterator();
        while (it.hasNext()) {
            if (it.next().expireTick <= s.activeTicks) it.remove();
        }
    }

    public static void grant(RunState s, String targetId, String label, int seconds, boolean selfDefenseOnly) {
        s.combatPermissions.removeIf(p -> p.target.equals(targetId));
        s.combatPermissions.add(new CombatPermission(targetId, label, s.activeTicks + seconds * 20L, selfDefenseOnly));
    }

    public static void clear(RunState s) {
        s.combatPermissions.clear();
    }

    public static CombatPermission activeFor(RunState s, Entity target) {
        String id = entityTypeId(target);
        for (CombatPermission p : s.combatPermissions) {
            if (p.target.equals("any") || p.target.equals(id)) return p;
        }
        return null;
    }

    public static String entityTypeId(Entity e) {
        Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
        return id == null ? "" : id.toString();
    }

    /**
     * Combat is legal during grace, against the dragon/end crystals, in
     * self-defence against endermen in the End, and inside announced windows.
     */
    public static boolean isAllowed(RunState s, World world, PlayerEntity player, Entity target) {
        if (s == null) return true;
        // Decorative figures of the mod are not mobs.
        if (target instanceof ArmorStandEntity || target instanceof AbstractDecorationEntity) return true;
        if (s.phase == dev.twitchmod.state.Phase.INTRO || s.phase == dev.twitchmod.state.Phase.GRACE) return true;
        if (target instanceof EnderDragonEntity) return true;
        if (entityTypeId(target).equals("minecraft:end_crystal")) return true;
        if (world instanceof ServerWorld && world.getRegistryKey().getValue().toString().equals("minecraft:the_end")
                && target instanceof EndermanEntity) return true;
        return activeFor(s, target) != null;
    }
}
