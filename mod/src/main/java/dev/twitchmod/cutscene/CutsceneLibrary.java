package dev.twitchmod.cutscene;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 32 cutscene scripts: 8 story, 6 drama, 8 atmosphere, 10 twists and secrets.
 * Expected per run: 12–20, never all of them.
 */
public final class CutsceneLibrary {
    private static final Map<String, CutsceneDefinition> ALL = new LinkedHashMap<>();

    private CutsceneLibrary() {
    }

    private static CutsceneDefinition add(CutsceneDefinition d) {
        ALL.put(d.id, d);
        return d;
    }

    private static CutsceneDefinition add(String id, String type, String title, int sec, String cam, String... lines) {
        CutsceneDefinition d = new CutsceneDefinition(id, type, title, sec, cam);
        for (int i = 0; i < lines.length; i++) d.line(i, lines[i]);
        return add(d);
    }

    static {
        // ---- 8 story ----
        add("story_air_start", CutsceneDefinition.STORY, "Эфир начался", 8, "crane",
                "Модерация в эфире.", "Ставка: три страйка — и мир банят.", "Пять минут на подготовку.");
        add("story_grace_end", CutsceneDefinition.STORY, "Грейс завершён", 5, "zoom",
                "Грейс окончен.", "Правила включены.");
        add("story_stronghold_mark", CutsceneDefinition.STORY, "Метка крепости", 4, "orbit",
                "Сигнал пойман.", "Крепость где-то там.");
        add("story_end_voice", CutsceneDefinition.STORY, "Голос Края", 6, "zoom",
                "Ты слышишь это?", "Пора заканчивать.");
        add("story_moderator_gift", CutsceneDefinition.STORY, "Дар модератора", 4, "static",
                "Модерация вручает подарок.");
        add("story_portal_found", CutsceneDefinition.STORY, "Портал найден", 6, "crane",
                "Портал найден.", "Обратной дороги нет.");
        add("story_before_dragon", CutsceneDefinition.STORY, "Перед драконом", 8, "orbit",
                "Пять минут управления.", "Случайные ивенты выключены.", "Только дракон.");
        add("story_win", CutsceneDefinition.STORY, "Победа", 8, "crane",
                "Дракон повержен.", "Эфир закрыт.");

        // ---- 6 drama ----
        CutsceneDefinition first = add("drama_first_strike", CutsceneDefinition.DRAMA, "Первый страйк", 5, "anvil",
                "Нарушение правила.", "1/3.");
        first.flashing().shaky();
        CutsceneDefinition second = add("drama_second_strike", CutsceneDefinition.DRAMA, "Второй страйк", 6, "anvil",
                "2/3.", "Следующий — бан.");
        second.flashing().shaky();
        CutsceneDefinition ban = add("drama_ban", CutsceneDefinition.DRAMA, "Бан мира", 8, "crane",
                "Третий страйк.", "Забег заблокирован.");
        ban.flashing();
        add("drama_death", CutsceneDefinition.DRAMA, "Смерть и некролог", 6, "zoom",
                "Забег окончен.", "Это не бан. Просто конец.");
        add("drama_armor_low", CutsceneDefinition.DRAMA, "Броня на исходе", 4, "static",
                "Комплект брони неполный.", "30 секунд на восстановление.");
        add("drama_last_chance", CutsceneDefinition.DRAMA, "Последний шанс", 5, "zoom",
                "Один страйк до бана.", "Дыши.");

        // ---- 8 atmosphere ----
        add("atmos_moderator_on_cam", CutsceneDefinition.ATMOSPHERE, "Модератор в кадре", 4, "static",
                "Модератор смотрит.");
        add("atmos_whisper", CutsceneDefinition.ATMOSPHERE, "Шёпот", 4, "static",
                "Ты слышал это?");
        add("atmos_shadow_behind", CutsceneDefinition.ATMOSPHERE, "Тень за спиной", 4, "orbit",
                "За спиной кто-то есть.");
        add("atmos_doppel", CutsceneDefinition.ATMOSPHERE, "Двойник", 5, "zoom",
                "Он повторяет твои движения.");
        add("atmos_echo", CutsceneDefinition.ATMOSPHERE, "Эхо", 3, "static",
                "Эхо твоих шагов.");
        add("atmos_silence", CutsceneDefinition.ATMOSPHERE, "Полная тишина", 4, "static",
                "Тишина.");
        add("atmos_observer_tracks", CutsceneDefinition.ATMOSPHERE, "Следы наблюдателя", 4, "crane",
                "Кто-то ходил здесь.");
        add("atmos_camera_watch", CutsceneDefinition.ATMOSPHERE, "Камера слежения", 4, "zoom",
                "Камера следит за тобой.");

        // ---- 10 twists & secrets ----
        CutsceneDefinition fb1 = add("twist_fake_ban_1", CutsceneDefinition.TWIST, "Фейковый бан I", 5, "anvil",
                "ЗАБЕГ ЗАБАНЕН", "...шутка. Страйки на месте.");
        fb1.flashing();
        CutsceneDefinition fb2 = add("twist_fake_ban_2", CutsceneDefinition.TWIST, "Фейковый бан II", 6, "anvil",
                "Аккаунт заблокирован", "Это была постановка.");
        fb2.flashing();
        CutsceneDefinition fs = add("twist_fake_strike", CutsceneDefinition.TWIST, "Фейковый страйк", 4, "anvil",
                "WARN", "Розыгрыш. Счётчик не тронут.");
        fs.flashing();
        add("twist_fake_win", CutsceneDefinition.TWIST, "Фейковая победа", 5, "crane",
                "Вы победили?", "Дракон всё ещё жив.");
        add("twist_fake_loot", CutsceneDefinition.TWIST, "Фальшивый лут", 4, "static",
                "Сундук пуст. Почти.");
        add("twist_imposter_mod", CutsceneDefinition.TWIST, "Самозванец-модератор", 5, "orbit",
                "Это не модератор.", "Кто это тогда?");
        add("twist_fake_portal", CutsceneDefinition.TWIST, "Фейковый портал", 4, "zoom",
                "Это не тот портал.");
        add("twist_double_bluff", CutsceneDefinition.TWIST, "Двойной обман", 5, "orbit",
                "Страйк?", "Нет. Твист.");
        add("story_final_twist", CutsceneDefinition.TWIST, "Финальный твист", 6, "crane",
                "Правила писались не для тебя.");
        CutsceneDefinition crown = add("twist_moderator_crown", CutsceneDefinition.TWIST, "Корона модератора", 8, "crane",
                "Вы победили.", "0 страйков за весь забег.", "Корона модератора — твоя.");
        crown.flashing();
    }

    public static CutsceneDefinition byId(String id) {
        return ALL.get(id);
    }

    public static List<CutsceneDefinition> all() {
        return new ArrayList<>(ALL.values());
    }

    public static int size() {
        return ALL.size();
    }

    public static Map<String, Integer> countsByType() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (CutsceneDefinition d : ALL.values()) out.merge(d.type, 1, Integer::sum);
        return out;
    }
}
