package dev.twitchmod.director;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalogue: exactly 225 records in eight categories (spec §6).
 *
 * <p>Records are data, not code paths: each one has a unique id, weight,
 * cooldown, telegraph, duration, conditions, actions, success/fail handling,
 * cleanup and an optional cutscene. The loader validates all of that in
 * {@link #validate()}.</p>
 */
public final class EventCatalog {
    private static final List<EventDefinition> ALL = new ArrayList<>();
    private static final Map<String, EventDefinition> BY_ID = new LinkedHashMap<>();
    private static final List<String> PROBLEMS = new ArrayList<>();

    private EventCatalog() {
    }

    static {
        buildGifts();
        buildStory();
        buildChaos();
        buildMinigames();
        buildModeration();
        buildAtmosphere();
        buildSecrets();
        buildChat();
        validate();
    }

    // ------------------------------------------------------------------ api
    public static List<EventDefinition> all() {
        return ALL;
    }

    public static int size() {
        return ALL.size();
    }

    public static EventDefinition byId(String id) {
        return BY_ID.get(id);
    }

    public static List<String> problems() {
        return PROBLEMS;
    }

    public static Map<EventCategory, Integer> counts() {
        Map<EventCategory, Integer> out = new LinkedHashMap<>();
        for (EventCategory c : EventCategory.values()) out.put(c, 0);
        for (EventDefinition d : ALL) out.merge(d.category, 1, Integer::sum);
        return out;
    }

    // -------------------------------------------------------------- helpers
    private static EventDefinition add(EventDefinition d) {
        if (BY_ID.containsKey(d.id)) {
            PROBLEMS.add("duplicate id " + d.id);
            return d;
        }
        ALL.add(d);
        BY_ID.put(d.id, d);
        return d;
    }

    private static Ev ev(String id, EventCategory cat, int weight, int cooldownSec, int durationSec) {
        Ev d = new Ev(id, cat);
        d.weight = weight;
        d.cooldownSec = cooldownSec;
        d.durationSec = durationSec;
        return d;
    }

    /** Short builder so 225 records stay readable. */
    public static class Ev extends EventDefinition {
        public Ev(String id, EventCategory cat) {
            this.id = id;
            this.category = cat;
            this.title = id;
        }

        public Ev act(String type, int a, int b, String s) {
            Action[] next = new Action[this.actions.length + 1];
            System.arraycopy(this.actions, 0, next, 0, this.actions.length);
            next[this.actions.length] = new Action(type, a, b, s);
            this.actions = next;
            return this;
        }

        public Ev ok(String type, int a, int b, String s) {
            Action[] next = new Action[this.onSuccess.length + 1];
            System.arraycopy(this.onSuccess, 0, next, 0, this.onSuccess.length);
            next[this.onSuccess.length] = new Action(type, a, b, s);
            this.onSuccess = next;
            return this;
        }

        public Ev bad(String type, int a, int b, String s) {
            Action[] next = new Action[this.onFail.length + 1];
            System.arraycopy(this.onFail, 0, next, 0, this.onFail.length);
            next[this.onFail.length] = new Action(type, a, b, s);
            this.onFail = next;
            return this;
        }

        public Ev title(String t, String d) {
            this.title = t;
            this.desc = d;
            this.descKey = "tm.ev." + id + ".d";
            return this;
        }

        public Ev cond(int minHealth, boolean escape, boolean dangerous) {
            this.minHealth = minHealth;
            this.needsSafeEscape = escape;
            this.dangerous = dangerous;
            return this;
        }

        public Ev objective(String kind, int amount, String target) {
            this.objective = kind;
            this.objectiveAmount = amount;
            this.objectiveTarget = target;
            return this;
        }

        public Ev reward(String table) {
            this.rewardTable = table;
            return this;
        }

        public Ev scene(String cutsceneId) {
            this.cutsceneId = cutsceneId;
            return this;
        }

        public Ev tele(int telegraphSec) {
            this.telegraphSec = telegraphSec;
            return this;
        }

        public Ev minSec(int s) {
            this.minActiveSec = s;
            return this;
        }

        public Ev permit() {
            this.grantsCombat = true;
            return this;
        }
    }

    // -------------------------------------------------------------- 45 gifts
    private static void buildGifts() {
        String[] methods = {"chest_drop", "direct_items", "xp_burst", "tool_care", "armor_care", "food_basket",
                "ore_vein", "enchant", "potion", "transport", "bed_pack", "torch_pack", "pearls", "arrows", "shulker"};
        String[] methodTitles = {"сундук с припасами", "посылка вслепую", "всплеск опыта", "ремонт инструмента",
                "полировка брони", "корзина еды", "жила рядом", "зачарование", "зелье от модерации",
                "транспорт", "спальник", "фонарики", "жемчуг Края", "колчан", "шалкеровый ящик"};
        String[] tiers = {"common", "rare", "legendary"};
        String[] tierTitles = {"Обычный подарок", "Редкий подарок", "Легендарный подарок"};
        int[] weights = {9, 5, 2};
        int[] cds = {180, 420, 900};
        for (int t = 0; t < 3; t++) {
            for (int m = 0; m < methods.length; m++) {
                Ev d = ev("gift_" + tiers[t] + "_" + methods[m], EventCategory.GIFT, weights[t], cds[t], 15);
                d.title(tierTitles[t] + ": " + methodTitles[m],
                        "Награда за " + (t == 0 ? "ровную игру" : t == 1 ? "чистые испытания" : "долгую игру без страйков"))
                        .act("gift", t, m, "")
                        .ok("reward", t, 0, "gift_" + tiers[t] + "_" + methods[m])
                        .title(tierTitles[t] + ": " + methodTitles[m], methodTitles[m]);
                add(d.reward("gift_" + tiers[t]));
            }
        }
    }

    // ------------------------------------------------------------- 20 story
    private static void buildStory() {
        String[][] rows = {
                {"story_air_start", "Эфир начался", "Студия, ставка, правила", "cutscene:story_air_start"},
                {"story_grace_end", "Грейс завершён", "Правила включены", "cutscene:story_grace_end"},
                {"story_first_tool", "Первый инструмент", "Проверка инвентаря", ""},
                {"story_craft_hint", "Подсказка: верстак", "Куда двигаться дальше", ""},
                {"story_food_supply", "Провизия от модерации", "Еда на дорогу", ""},
                {"story_stronghold_hint", "Сигнал: крепость", "Первый содержательный намёк", ""},
                {"story_stronghold_coords", "Координаты крепости", "Точная точка", ""},
                {"story_blaze_hint", "Путь к стержням", "Где взять огненные стержни", ""},
                {"story_eyes_task", "Задача: глаза Эндера", "Достать недостающие глаза", ""},
                {"story_eye_market", "Глаза на обмен", "Обмен у модерации", ""},
                {"story_nether_beacon", "Маяк в Незере", "Метка портала", ""},
                {"story_fortress_reveal", "Крепость найдена", "Обнаружена структура", ""},
                {"story_portal_hint", "Рамка портала", "Куда ставить глаза", ""},
                {"story_portal_found", "Портал найден", "Катсцена подхода", "cutscene:story_portal_found"},
                {"story_kit_check", "Проверка комплекта", "Минимально жизнеспособный набор", ""},
                {"story_end_voice", "Голос Края", "Атмосферная сцена", "cutscene:story_end_voice"},
                {"story_fast_travel", "Безопасный перенос", "Добровольный телепорт к крепости", ""},
                {"story_dragon_brief", "Брифинг: дракон", "Как стоять и куда бить", ""},
                {"story_teacher", "Инструктор модерации", "Разбор ошибок", ""},
                {"story_final_exam", "Финальный экзамен", "Ошибка стоит бонуса, не страйка", ""},
        };
        for (String[] r : rows) {
            Ev d = ev(r[0], EventCategory.STORY, 2, 0, 8);
            d.title(r[1], r[2]);
            if (!r[3].isEmpty()) d.scene(r[3].replace("cutscene:", ""));
            add(d.act("story", 0, 0, r[0]).minSec(0));
        }
        BY_ID.get("story_stronghold_hint").minActiveSec = 1800;
        BY_ID.get("story_stronghold_coords").minActiveSec = 2400;
        BY_ID.get("story_eyes_task").minActiveSec = 3300;
        BY_ID.get("story_fast_travel").minActiveSec = 4200;
    }

    // ------------------------------------------------------------- 50 chaos
    private static void buildChaos() {
        String[][] mobs = {
                {"zombie", "minecraft:zombie", "6"}, {"zombie_pack", "minecraft:zombie", "10"},
                {"husk", "minecraft:husk", "8"}, {"drowned", "minecraft:drowned", "8"},
                {"skeleton", "minecraft:skeleton", "6"}, {"stray", "minecraft:stray", "5"},
                {"spider", "minecraft:spider", "6"}, {"cave_spider", "minecraft:cave_spider", "5"},
                {"silverfish", "minecraft:silverfish", "8"}, {"slime", "minecraft:slime", "5"},
                {"pillager", "minecraft:pillager", "4"}, {"vindicator", "minecraft:vindicator", "3"},
                {"witch", "minecraft:witch", "2"}, {"magma_cube", "minecraft:magma_cube", "4"},
                {"zombified", "minecraft:zombified_piglin", "5"}, {"phantom", "minecraft:phantom", "3"},
        };
        int[] mobDur = {25, 30, 25, 25, 25, 25, 25, 20, 20, 25, 25, 25, 25, 25, 25, 30};
        for (int i = 0; i < mobs.length; i++) {
            Ev d = ev("chaos_wave_" + mobs[i][0], EventCategory.CHAOS, 6, 600, mobDur[i]);
            d.title("Волна: " + mobs[i][0], "Выжить до конца волны").cond(10, true, true)
                    .objective("survive", 0, "").tele(5).permit()
                    .act("spawn_wave", Integer.parseInt(mobs[i][2]), 0, mobs[i][1])
                    .act("grant_combat", mobDur[i] + 8, 0, mobs[i][1])
                    .ok("reward", 0, 0, "common").bad("attention_delta", -10, 0, "");
            add(d);
        }
        int[][] meteors = {{4, 20, 40}, {6, 24, 30}, {10, 28, 25}, {12, 32, 20}};
        String[] meteorNames = {"лёгкий", "обычный", "плотный", "долгий"};
        for (int i = 0; i < meteors.length; i++) {
            Ev d = ev("chaos_meteor_" + meteorNames[i], EventCategory.CHAOS, 5, 700, meteors[i][1]);
            d.title("Метеоритный дождь: " + meteorNames[i], "Точки падения видны заранее").cond(12, true, true)
                    .objective("survive", 0, "").tele(6)
                    .act("meteor", meteors[i][0], meteors[i][2], "")
                    .ok("reward", 0, 0, "common").bad("attention_delta", -8, 0, "");
            add(d);
        }
        int[][] crowds = {{100, 8, 30}, {60, 5, 20}, {100, 12, 45}};
        for (int i = 0; i < crowds.length; i++) {
            Ev d = ev("chaos_crowd_" + i, EventCategory.CHAOS, 4, 900, crowds[i][2]);
            d.title(crowds[i][0] + " зрителей ворвались в мир", "Честный HUD: " + crowds[i][0] + " зрителей / " + crowds[i][1] + " охотников")
                    .cond(14, true, true).objective("survive", 0, "").tele(5).permit()
                    .act("crowd", crowds[i][0], crowds[i][1], "")
                    .act("grant_combat", crowds[i][2] + 10, 0, "minecraft:zombie")
                    .ok("reward", 1, 0, "rare").bad("attention_delta", -12, 0, "");
            add(d);
        }
        int[] doppels = {30, 45, 60};
        for (int i = 0; i < doppels.length; i++) {
            Ev d = ev("chaos_doppelganger_" + doppels[i], EventCategory.CHAOS, 3, 1200, doppels[i]);
            d.title("Двойник", "Повторяет твои действия с задержкой. Бить без разрешения нельзя.")
                    .cond(14, false, false).objective("survive", 0, "").tele(4).scene("atmos_doppel")
                    .act("doppel", doppels[i], 0, "")
                    .ok("reward", 0, 0, "rare");
            add(d);
        }
        String[][] chases = {
                {"elite", "minecraft:vindicator", "25"}, {"pack", "minecraft:zombie", "30"},
                {"spider", "minecraft:spider", "25"}, {"armored", "minecraft:zombie", "35"}
        };
        for (String[] c : chases) {
            Ev d = ev("chaos_chase_" + c[0], EventCategory.CHAOS, 5, 800, Integer.parseInt(c[2]));
            d.title("Погоня: " + c[0], "Уйти или оторваться").cond(12, true, true)
                    .objective("survive", 0, "").tele(4).permit()
                    .act("chase", 3, 0, c[1]).act("grant_combat", Integer.parseInt(c[2]) + 5, 0, c[1])
                    .ok("reward", 0, 0, "common").bad("attention_delta", -10, 0, "");
            add(d);
        }
        String[][] anomalies = {
                {"low_gravity", "Медленная гравитация", "status", "minecraft:slow_falling"},
                {"rockfall", "Камнепад", "rockfall", ""},
                {"night_fall", "Резкая ночь", "time", "18000"},
                {"sound_trap", "Звуковая ловушка", "sound", "minecraft:entity.enderman.scream"},
                {"gust", "Порыв ветра", "gust", ""},
                {"void_breath", "Дыхание пустоты", "status", "minecraft:darkness"}
        };
        for (String[] a : anomalies) {
            Ev d = ev("chaos_anomaly_" + a[0], EventCategory.CHAOS, 4, 700, 25);
            d.title("Аномалия: " + a[1], "Безопасная природная сцена").cond(8, false, false).tele(3)
                    .act(a[2], 25, 0, a[3]).ok("reward", 0, 0, "common");
            add(d);
        }
        String[][] arenas = {{"small", "5"}, {"mid", "8"}, {"big", "12"}};
        for (String[] a : arenas) {
            Ev d = ev("chaos_arena_" + a[0], EventCategory.CHAOS, 3, 1000, 40);
            d.title("Арена " + a[0], "Временная площадка и волна").cond(16, true, true)
                    .objective("survive", 0, "").tele(6).permit()
                    .act("temp_platform", Integer.parseInt(a[1]), 45, "")
                    .act("spawn_wave", Integer.parseInt(a[1]), 0, "minecraft:zombie")
                    .act("grant_combat", 50, 0, "minecraft:zombie")
                    .ok("reward", 1, 0, "rare").bad("attention_delta", -10, 0, "");
            add(d);
        }
        String[][] passive = {{"cows", "minecraft:cow", "18"}, {"chickens", "minecraft:chicken", "30"}};
        for (String[] p : passive) {
            Ev d = ev("chaos_stampede_" + p[0], EventCategory.CHAOS, 3, 900, 20);
            d.title("Стадо: " + p[0], "Безопасный, но громкий хаос").act("spawn_passive", Integer.parseInt(p[2]), 0, p[1]);
            add(d);
        }
        String[][] skies = {{"fog_storm", "minecraft:particle.ash", "atmos_camera_watch"},
                {"black_rain", "minecraft:particle.campfire_cosy_smoke", "atmos_moderator_on_cam"}};
        for (String[] s : skies) {
            Ev d = ev("chaos_sky_" + s[0], EventCategory.CHAOS, 3, 800, 30);
            d.title("Небо: " + s[0], "Атмосферная помеха").act("particles", 60, 0, s[1]).act("weather", 30, 0, "rain")
                    .scene(s[2]);
            add(d);
        }
        String[][] hunters = {{"armored_zombie", "minecraft:zombie", "1"}, {"speed_skeleton", "minecraft:skeleton", "2"},
                {"wither_duo", "minecraft:wither_skeleton", "2"}};
        for (String[] h : hunters) {
            Ev d = ev("chaos_hunter_" + h[0], EventCategory.CHAOS, 4, 850, 30);
            d.title("Охотник: " + h[0], "Один сильный противник").cond(16, true, true).tele(5).permit()
                    .objective("hit", Integer.parseInt(h[2]), h[1])
                    .act("spawn_wave", Integer.parseInt(h[2]), 1, h[1]).act("grant_combat", 35, 0, h[1])
                    .ok("reward", 1, 0, "rare").bad("attention_delta", -8, 0, "");
            add(d);
        }
        String[][] misc = {
                {"speedrun_roulette", "Рулетка эффектов", "roulette", ""},
                {"creeper_bait", "Приманка для крипера", "decoy", "twist_double_bluff"},
                {"mirror_time", "Дежавю во времени", "time", "atmos_echo"},
                {"tnt_scare", "Ложный подрыв", "fake_explosion", "twist_fake_strike"}
        };
        for (String[] m : misc) {
            Ev d = ev("chaos_misc_" + m[0], EventCategory.CHAOS, 3, 750, 20);
            d.title(m[1], "Короткий хаос без страйков").act(m[2], 20, 0, "");
            if (!m[3].isEmpty()) d.scene(m[3]);
            add(d);
        }
    }

    // --------------------------------------------------------- 25 minigames
    private static void buildMinigames() {
        for (int i = 0; i < 3; i++) {
            int r = 12 + i * 10;
            Ev d = ev("mini_reach_" + r, EventCategory.MINIGAME, 5, 700, 45);
            d.title("Добеги до метки (" + r + " блоков)", "Метка видна столбом частиц")
                    .objective("reach", r, "").cond(10, true, true).tele(5)
                    .act("beacon", r, 45, "").ok("reward", 0, 0, "common").bad("attention_delta", -8, 0, "");
            add(d);
        }
        String[][] collect = {{"minecraft:apple", "3"}, {"minecraft:coal", "6"}, {"minecraft:cobblestone", "12"}};
        for (String[] c : collect) {
            Ev d = ev("mini_collect_" + c[0].replace("minecraft:", ""), EventCategory.MINIGAME, 5, 700, 60);
            d.title("Собери: " + c[0].replace("minecraft:", "") + " x" + c[1], "Проверяется наличие ресурса до старта")
                    .objective("collect", Integer.parseInt(c[1]), c[0]).tele(4)
                    .act("require_item", Integer.parseInt(c[1]), 0, c[0])
                    .ok("reward", 0, 0, "common").bad("attention_delta", -6, 0, "");
            add(d);
        }
        String[][] eat = {{"minecraft:apple", "2"}, {"minecraft:bread", "3"}};
        for (String[] e : eat) {
            Ev d = ev("mini_eat_" + e[0].replace("minecraft:", ""), EventCategory.MINIGAME, 4, 800, 40);
            d.title("Съешь: " + e[0].replace("minecraft:", "") + " x" + e[1], "Предлагается только если еда доступна")
                    .objective("eat", Integer.parseInt(e[1]), e[0]).tele(3)
                    .act("require_item", Integer.parseInt(e[1]), 0, e[0])
                    .ok("reward", 0, 0, "common").bad("attention_delta", -5, 0, "");
            add(d);
        }
        for (int i = 0; i < 2; i++) {
            Ev d = ev("mini_still_" + (8 + i * 4), EventCategory.MINIGAME, 3, 800, 8 + i * 4);
            d.title("Не двигайся " + (8 + i * 4) + " сек", "AFK-испытание, время идёт")
                    .objective("stand_still", 0, "").tele(4).act("particles", 20, 0, "minecraft:particle.enchant")
                    .ok("reward", 0, 0, "common").bad("attention_delta", -5, 0, "");
            add(d);
        }
        for (int i = 0; i < 3; i++) {
            Ev d = ev("mini_survive_" + (15 + i * 10), EventCategory.MINIGAME, 4, 800, 15 + i * 10);
            d.title("Выдержи " + (15 + i * 10) + " сек под давлением", "Разрешение на бой выдано заранее")
                    .objective("survive", 0, "").cond(12, true, true).tele(5).permit()
                    .act("spawn_wave", 4 + i * 2, 0, "minecraft:zombie").act("grant_combat", 20 + i * 10, 0, "minecraft:zombie")
                    .ok("reward", 0, 0, "common").bad("attention_delta", -8, 0, "");
            add(d);
        }
        for (int i = 0; i < 3; i++) {
            int n = 8 + i * 6;
            Ev d = ev("mini_break_" + n, EventCategory.MINIGAME, 4, 700, 50);
            d.title("Разрушь " + n + " блоков", "Учитывается лимит урона миру")
                    .objective("break", n, "").tele(4)
                    .act("world_damage_note", n, 0, "").ok("reward", 0, 0, "common").bad("attention_delta", -6, 0, "");
            add(d);
        }
        for (int i = 0; i < 3; i++) {
            Ev d = ev("mini_targets_" + (3 + i), EventCategory.MINIGAME, 4, 800, 40);
            d.title("Порази цели: " + (3 + i), "Разрешение на бой действует только на цели")
                    .objective("hit", 3 + i, "minigame").tele(5).permit()
                    .act("spawn_dummy", 3 + i, 0, "").act("grant_combat", 45, 0, "minigame")
                    .ok("reward", 0, 0, "common").bad("attention_delta", -6, 0, "");
            add(d);
        }
        for (int i = 0; i < 2; i++) {
            Ev d = ev("mini_bridge_" + (10 + i * 10), EventCategory.MINIGAME, 3, 900, 50 + i * 20);
            d.title("Мост через провал", "Временные блоки исчезают после ивента")
                    .objective("reach", 10 + i * 10, "").tele(5)
                    .act("gap", 10 + i * 10, 60, "").ok("reward", 1, 0, "rare").bad("attention_delta", -6, 0, "");
            add(d);
        }
        for (int i = 0; i < 2; i++) {
            Ev d = ev("mini_find_chest_" + i, EventCategory.MINIGAME, 3, 900, 60);
            d.title("Найди сундук мода", "Обычный сундук, не запечатанный")
                    .objective("reach", 20, "").tele(5).act("spawn_chest", 20, 60, "")
                    .ok("reward", 1, 0, "rare").bad("attention_delta", -5, 0, "");
            add(d);
        }
        for (int i = 0; i < 2; i++) {
            Ev d = ev("mini_dodge_" + (8 + i * 4), EventCategory.MINIGAME, 4, 700, 25 + i * 10);
            d.title("Увернись от метеоров", "Точки падения видны заранее").cond(12, true, true).tele(5)
                    .act("meteor", 4 + i * 2, 25, "").ok("reward", 0, 0, "common").bad("attention_delta", -6, 0, "");
            add(d);
        }
    }

    // -------------------------------------------------------- 20 moderation
    private static void buildModeration() {
        String[][] rows = {
                {"mod_check_rules", "Проверка правил", "Викторина: лишение бонуса, не страйка", "quiz"},
                {"mod_check_tool", "Проверка инструментов", "Каким инструментом ломать", "quiz"},
                {"mod_check_combat", "Проверка боя", "Когда бить можно", "quiz"},
                {"mod_inspector", "Инспектор в мире", "Визуальная проверка", "atmosphere"},
                {"mod_amnesty", "Апелляция", "Добровольное испытание, минус один текущий страйк", "appeal"},
                {"mod_probation", "Испытательный срок", "Бонус за 3 минуты без нарушений", "probation"},
                {"mod_clean_bonus", "Бонус за чистую игру", "Награда без страйков", "reward"},
                {"mod_fake_strike", "Фейковый страйк", "Розыгрыш, счётчик не трогается", "fake_strike"},
                {"mod_fake_ban_short", "Фейковый бан I", "Раскрывается через 3 секунды", "fake_ban"},
                {"mod_fake_ban_long", "Фейковый бан II", "Постановка, затем раскрытие", "fake_ban"},
                {"mod_rules_reminder", "Напоминание правил", "Короткий экран правил", "rules"},
                {"mod_stats", "Сводка для зрителя", "Страйки, комбо, урон миру", "stats"},
                {"mod_crown_preview", "Превью короны", "Что даёт идеальный забег", "crown"},
                {"mod_audit_chest", "Аудит инвентаря", "Проверка запрещённых предметов", "audit"},
                {"mod_witness", "Самозванец-модератор", "Твист: не настоящий модератор", "witness"},
                {"mod_timeout", "Предупреждение о таймауте", "Близко к третьему страйку", "warn"},
                {"mod_cheat_scan", "Скан обхода", "Проверка только описанных случаев", "scan"},
                {"mod_review", "Разбор эпизода", "Повтор момента с комментарием", "replay"},
                {"mod_bonus_exam", "Экзамен на бонус", "Ошибка стоит бонуса, не страйка", "quiz"},
                {"mod_strike_review", "Разбор страйка", "Показ причины и правила", "review"},
        };
        for (String[] r : rows) {
            Ev d = ev(r[0], EventCategory.MODERATION, 3, 900, 20);
            d.title(r[1], r[2]).act(r[3], 20, 0, "");
            if (r[0].equals("mod_amnesty")) d.minSec(1);
            add(d);
        }
    }

    // ------------------------------------------------------- 30 atmosphere
    private static void buildAtmosphere() {
        String[][] rows = {
                {"atmos_surveillance", "Слежка", "Камера в кадре"},
                {"atmos_whisper", "Шёпот", "Приглушённые голоса"},
                {"atmos_shadow", "Тень за спиной", "Силуэт без урона"},
                {"atmos_echo", "Эхо", "Повтор шагов"},
                {"atmos_silence", "Полная тишина", "Игровые звуки приглушены"},
                {"atmos_tracks", "Следы наблюдателя", "Пометки на земле"},
                {"atmos_camera", "Камера слежения", "Лёгкий наезд камеры"},
                {"atmos_glitch", "Глитч", "Помехи, счётчик страйков читается"},
                {"atmos_fog_dense", "Густой туман", "Обзор падает"},
                {"atmos_fog_light", "Лёгкая дымка", "Мягкая сцена"},
                {"atmos_heartbeat", "Сердцебиение", "Звуковой пульс"},
                {"atmos_eye", "Глаз в темноте", "Наблюдатель"},
                {"atmos_spotlight", "Прожектор", "Свет над игроком"},
                {"atmos_rain", "Дождь", "Погода"},
                {"atmos_thunder", "Гром вдали", "Звук без урона"},
                {"atmos_dust", "Пылинки", "Частицы"},
                {"atmos_ender_flutter", "Флаттер Края", "Частицы Края"},
                {"atmos_static", "Телевизионные помехи", "Визуальный шум"},
                {"atmos_daytime", "Смена времени", "Мягкий переход света"},
                {"atmos_red_moon", "Красная луна", "Параноидальный эпизод"},
                {"atmos_cold_breath", "Холодное дыхание", "Пар"},
                {"atmos_drone", "Дрон-наблюдатель", "Летающая камера"},
                {"atmos_lens", "Блик", "Оптический эффект"},
                {"atmos_vignette", "Виньетка", "Края темнеют"},
                {"atmos_slow_zoom", "Медленный наезд", "Кинематографично"},
                {"atmos_title_card", "Титр", "Короткая плашка"},
                {"atmos_sound_low", "Приглушение", "Звук ниже"},
                {"atmos_quiet_rain", "Тихий дождь", "Спокойная сцена"},
                {"atmos_breath", "Задержка дыхания", "Тихо и напряжённо"},
                {"atmos_dejavu", "Дежавю", "Повтор сцены"},
        };
        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            Ev d = ev(r[0], EventCategory.ATMOSPHERE, 5, 600, 12 + (i % 4) * 4);
            d.title(r[1], r[2]);
            switch (r[0]) {
                case "atmos_silence", "atmos_breath" -> d.act("silence", d.durationSec, 0, "");
                case "atmos_glitch", "atmos_static" -> d.act("glitch", d.durationSec, 0, "");
                case "atmos_fog_dense" -> d.act("fog", d.durationSec, 3, "");
                case "atmos_fog_light" -> d.act("fog", d.durationSec, 1, "");
                case "atmos_rain", "atmos_quiet_rain" -> d.act("weather", d.durationSec, 0, "rain");
                case "atmos_thunder" -> d.act("sound", 1, 0, "minecraft:entity.lightning_bolt.thunder");
                case "atmos_whisper", "atmos_heartbeat", "atmos_cold_breath", "atmos_sound_low" ->
                        d.act("sound", 1, 0, "minecraft:ambient.cave");
                case "atmos_slow_zoom", "atmos_camera", "atmos_drone", "atmos_lens", "atmos_vignette" ->
                        d.act("camera", d.durationSec, 0, "");
                case "atmos_title_card" -> d.scene("atmos_moderator_on_cam");
                case "atmos_shadow" -> d.scene("atmos_shadow_behind");
                case "atmos_dejavu" -> d.act("dejavu", d.durationSec, 0, "");
                case "atmos_spotlight", "atmos_eye", "atmos_dust", "atmos_ender_flutter", "atmos_red_moon",
                     "atmos_tracks", "atmos_daytime", "atmos_echo", "atmos_surveillance" ->
                        d.act("particles", 40, d.durationSec, "minecraft:particle.enchant");
                default -> d.act("atmosphere", d.durationSec, 0, r[0]);
            }
            add(d);
        }
    }

    // ---------------------------------------------------------- 15 secrets
    private static void buildSecrets() {
        String[][] rows = {
                {"secret_fan_letter", "Письмо фаната", "Записка в мире"},
                {"secret_fan_meeting", "Редкий поклонник", "Один зритель вместо толпы"},
                {"secret_room_beacon", "Тайная комната", "Маяк-подсказка"},
                {"secret_mystery_chest", "Загадочный сундук", "Не запечатанный"},
                {"secret_crown_hint", "Намёк на корону", "Условие идеального забега"},
                {"secret_glitched_message", "Глитч-сообщение", "Обрывок эфира"},
                {"secret_old_broadcast", "Старый эфир", "Архивная запись"},
                {"secret_ghost_player", "Игрок-призрак", "Визуальная фигура"},
                {"secret_twin_signal", "Сигнал двойника", "Намёк на двойника"},
                {"secret_garden", "Тайный сад", "Мирные цветы"},
                {"secret_code_word", "Кодовое слово", "Пароль модератора"},
                {"secret_disc", "Пластинка", "Пасхалка"},
                {"secret_endermite", "Эндермит-шпион", "Мелкий шпион"},
                {"secret_whisperer_gift", "Дар шёпота", "Редкий предмет"},
                {"secret_final_twist_preview", "Превью финального твиста", "Тизер", "story_final_twist"},
        };
        for (String[] r : rows) {
            Ev d = ev(r[0], EventCategory.SECRET, 1, 1800, 15);
            d.title(r[1], r[2]).act("secret", 15, 0, r[0]).ok("reward", 1, 0, "rare");
            if (r.length > 3) d.scene(r[3]);
            add(d);
        }
    }

    // ------------------------------------------------------------- 20 chat
    private static void buildChat() {
        String[][] votes = {
                {"chat_vote_gear", "Чат выбирает: броня или еда", "Полный сет или запас еды"},
                {"chat_vote_weather", "Чат выбирает: дождь или ясно", "Погода на 60 секунд"},
                {"chat_vote_night", "Чат выбирает: ночь или день", "Время суток"},
                {"chat_vote_mobs", "Чат выбирает: волна или тишина", "Волна или спокойствие"},
                {"chat_vote_loot", "Чат выбирает: редкий лут или опыт", "Риск против стабильности"},
                {"chat_vote_route", "Чат выбирает: короткий путь или безопасный", "Маршрут"},
                {"chat_vote_music", "Чат выбирает: музыка или тишина", "Атмосфера"},
                {"chat_vote_meteor", "Чат выбирает: метеоры или туман", "Аномалия"},
        };
        for (String[] v : votes) {
            Ev d = ev(v[0], EventCategory.CHAT, 5, 500, 20);
            d.title(v[1], v[2]).act("choice", 20, 0, v[0]).scene("twist_chat_choice");
            add(d);
        }
        String[][] risks = {{"chat_risk_low", "скромная"}, {"chat_risk_mid", "средняя"}, {"chat_risk_high", "высокая"}};
        for (String[] r : risks) {
            Ev d = ev(r[0], EventCategory.CHAT, 4, 700, 20);
            d.title("Риск или безопасность", "Сложное испытание с " + r[1] + " наградой или спокойный вариант")
                    .act("risk_safe", 20, 0, r[0]);
            add(d);
        }
        String[][] rumors = {{"chat_rumor_stronghold", "слух о крепости"}, {"chat_rumor_dragon", "слух о драконе"},
                {"chat_rumor_fake", "ложный слух"}};
        for (String[] r : rumors) {
            Ev d = ev(r[0], EventCategory.CHAT, 3, 600, 12);
            d.title("Слух: " + r[1], "Зрители обсуждают — правда или нет").act("rumor", 12, 0, r[0]);
            add(d);
        }
        String[][] suggestions = {{"chat_suggest_tool", "совет по инструменту"}, {"chat_suggest_food", "совет по еде"},
                {"chat_suggest_route", "совет по маршруту"}};
        for (String[] s : suggestions) {
            Ev d = ev(s[0], EventCategory.CHAT, 3, 600, 10);
            d.title("Предложение зрителей", s[1]).act("rumor", 10, 1, s[0]);
            add(d);
        }
        add(ev("chat_bribe", EventCategory.CHAT, 2, 900, 20).title("Подкуп чата", "Награда за риск — отказ без страйка")
                .act("risk_safe", 20, 1, "chat_bribe"));
        add(ev("chat_poll_mood", EventCategory.CHAT, 4, 400, 12).title("Опрос настроения", "Чат показывает отношение")
                .act("choice", 12, 1, "chat_poll_mood"));
    }

    // ------------------------------------------------------------ validate
    private static void validate() {
        for (EventDefinition d : ALL) {
            if (d.weight <= 0) PROBLEMS.add("weight<=0: " + d.id);
            if (d.durationSec <= 0) PROBLEMS.add("duration<=0: " + d.id);
            if (d.cooldownSec < 0) PROBLEMS.add("cooldown<0: " + d.id);
            if (d.title == null || d.title.isEmpty()) PROBLEMS.add("title missing: " + d.id);
            if (d.actions.length == 0) PROBLEMS.add("no actions: " + d.id);
            for (EventDefinition.Action a : d.actions) {
                if (!EventEffects.isKnown(a.type)) PROBLEMS.add("unknown effect '" + a.type + "': " + d.id);
            }
            if (!d.cutsceneId.isEmpty() && dev.twitchmod.cutscene.CutsceneLibrary.byId(d.cutsceneId) == null) {
                PROBLEMS.add("missing cutscene: " + d.id);
            }
            if ("collect".equals(d.objective) || "eat".equals(d.objective)) {
                if (d.objectiveTarget.isEmpty()) PROBLEMS.add("objective target missing: " + d.id);
            }
        }
        Map<EventCategory, Integer> c = counts();
        for (EventCategory cat : EventCategory.values()) {
            if (c.get(cat) != cat.targetCount) {
                PROBLEMS.add("category " + cat.key + " has " + c.get(cat) + ", expected " + cat.targetCount);
            }
        }
        if (ALL.size() != 225) PROBLEMS.add("catalogue size is " + ALL.size() + ", expected 225");
        int cinematic = 0;
        for (EventDefinition d : ALL) if (!d.cutsceneId.isEmpty()) cinematic++;
        if (cinematic < 30) PROBLEMS.add("cinematic variants: " + cinematic + ", expected >=30");
        if (!PROBLEMS.isEmpty()) {
            for (String p : PROBLEMS) System.out.println("[twitchmod][catalogue] " + p);
        }
    }

    /** CSV registry export — acceptance criterion #1. */
    public static String exportCsv() {
        StringBuilder sb = new StringBuilder("id,category,weight,cooldown_sec,telegraph_sec,duration_sec,dangerous,objective,min_active_sec,cutscene,title\n");
        for (EventDefinition d : ALL) {
            sb.append(String.join(",",
                    d.id, d.category.key, String.valueOf(d.weight), String.valueOf(d.cooldownSec),
                    String.valueOf(d.telegraphSec), String.valueOf(d.durationSec), String.valueOf(d.dangerous),
                    d.objective, String.valueOf(d.minActiveSec), d.cutsceneId.isEmpty() ? "-" : d.cutsceneId,
                    d.title.replace(',', ';'))).append('\n');
        }
        return sb.toString();
    }
}
String.valueOf(d.minActiveSec), d.cutsceneId.isEmpty() ? "-" : d.cutsceneId,
                    d.title.replace(',', ';'))).append('\n');
        }
        return sb.toString();
    }
}
,', ';'))).append('\n');
        }
        return sb.toString();
    }
}
-" : d.cutsceneId,
                    d.title.replace(',', ';'))).append('\n');
        }
        return sb.toString();
    }
}
