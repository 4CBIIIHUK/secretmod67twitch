export type EventCat =
  | 'gift'
  | 'story'
  | 'chaos'
  | 'minigame'
  | 'moderation'
  | 'atmosphere'
  | 'secret'
  | 'chat';

export interface Ev {
  id: string;
  cat: EventCat;
  title: string;
  weight: number;
  cd: number;
  dur: number;
  dangerous: boolean;
  objective: string;
  minSec: number;
  scene: string;
}

export const CATEGORY_META: Record<EventCat, { label: string; target: number; job: string }> = {
  gift: { label: 'Подарки', target: 45, job: 'Награда за 2–3 минуты без нарушений' },
  story: { label: 'Сюжет и прогресс', target: 20, job: 'Подсказки, крепость, глаза, путь к финалу' },
  chaos: { label: 'Хаос и экшн', target: 50, job: 'Погони, волны, метеоры, аномалии' },
  minigame: { label: 'Мини-игры', target: 25, job: 'Короткая выполнимая задача' },
  moderation: { label: 'Модерация', target: 20, job: 'Проверки, апелляции, бонусы. Не случайные страйки' },
  atmosphere: { label: 'Атмосфера', target: 30, job: 'Слежка, тишина, туман, глитч' },
  secret: { label: 'Секреты', target: 15, job: 'Редкие намёки и скрытые встречи' },
  chat: { label: 'Чат и выбор', target: 20, job: 'Голосования, риск/награда, слухи' },
};

const giftMethods = [
  'chest_drop|сундук с припасами',
  'direct_items|посылка вслепую',
  'xp_burst|всплеск опыта',
  'tool_care|ремонт инструмента',
  'armor_care|полировка брони',
  'food_basket|корзина еды',
  'ore_vein|жила рядом',
  'enchant|зачарование',
  'potion|зелье от модерации',
  'transport|транспорт',
  'bed_pack|спальник',
  'torch_pack|фонарики',
  'pearls|жемчуг Края',
  'arrows|колчан',
  'shulker|шалкеровый ящик',
];
const tiers = ['common|Обычный|9|180', 'rare|Редкий|5|420', 'legendary|Легендарный|2|900'];

function gifts(): Ev[] {
  const out: Ev[] = [];
  tiers.forEach((t) => {
    const [key, tierTitle, weight, cd] = t.split('|');
    giftMethods.forEach((m) => {
      const [method, label] = m.split('|');
      out.push({
        id: `gift_${key}_${method}`,
        cat: 'gift',
        title: `${tierTitle} подарок: ${label}`,
        weight: Number(weight),
        cd: Number(cd),
        dur: 15,
        dangerous: false,
        objective: 'none',
        minSec: 0,
        scene: '',
      });
    });
  });
  return out;
}

const storyRows = [
  ['story_air_start', 'Эфир начался', 'story_air_start'],
  ['story_grace_end', 'Грейс завершён', 'story_grace_end'],
  ['story_first_tool', 'Первый инструмент', ''],
  ['story_craft_hint', 'Подсказка: верстак', ''],
  ['story_food_supply', 'Провизия от модерации', ''],
  ['story_stronghold_hint', 'Сигнал: крепость', 'story_stronghold_mark'],
  ['story_stronghold_coords', 'Координаты крепости', 'story_stronghold_mark'],
  ['story_blaze_hint', 'Путь к стержням', ''],
  ['story_eyes_task', 'Задача: глаза Эндера', ''],
  ['story_eye_market', 'Глаза на обмен', ''],
  ['story_nether_beacon', 'Маяк в Незере', ''],
  ['story_fortress_reveal', 'Крепость найдена', ''],
  ['story_portal_hint', 'Рамка портала', ''],
  ['story_portal_found', 'Портал найден', 'story_portal_found'],
  ['story_kit_check', 'Проверка комплекта', ''],
  ['story_end_voice', 'Голос Края', 'story_end_voice'],
  ['story_fast_travel', 'Безопасный перенос', ''],
  ['story_dragon_brief', 'Брифинг: дракон', 'story_before_dragon'],
  ['story_teacher', 'Инструктор модерации', ''],
  ['story_final_exam', 'Финальный экзамен', ''],
];

const chaosMobs = [
  'zombie|6|25', 'zombie_pack|10|30', 'husk|8|25', 'drowned|8|25', 'skeleton|6|25',
  'stray|5|25', 'spider|6|25', 'cave_spider|5|20', 'silverfish|8|20', 'slime|5|25',
  'pillager|4|25', 'vindicator|3|25', 'witch|2|25', 'magma_cube|4|25',
  'zombified|5|25', 'phantom|3|30',
];

function chaos(): Ev[] {
  const out: Ev[] = [];
  chaosMobs.forEach((m) => {
    const [name, count, dur] = m.split('|');
    out.push({
      id: `chaos_wave_${name}`, cat: 'chaos', title: `Волна: ${name} ×${count}`,
      weight: 6, cd: 600, dur: Number(dur), dangerous: true, objective: 'survive', minSec: 0, scene: '',
    });
  });
  [['лёгкий', 4, 20, 40], ['обычный', 6, 24, 30], ['плотный', 10, 28, 25], ['долгий', 12, 32, 20]].forEach(
    ([name, count, dur, cd]) => {
      out.push({
        id: `chaos_meteor_${name}`, cat: 'chaos', title: `Метеоритный дождь: ${name} (${count} ударов)`,
        weight: 5, cd: cd as number, dur: dur as number, dangerous: true, objective: 'survive', minSec: 0, scene: '',
      });
    },
  );
  ([[100, 8, 30], [60, 5, 20], [100, 12, 45]] as const).forEach(([viewers, hunters, dur], i) => {
    out.push({
      id: `chaos_crowd_${i}`, cat: 'chaos', title: `${viewers} зрителей ворвались в мир (${hunters} охотников)`,
      weight: 4, cd: 900, dur, dangerous: true, objective: 'survive', minSec: 0, scene: '',
    });
  });
  [30, 45, 60].forEach((d) => out.push({
    id: `chaos_doppelganger_${d}`, cat: 'chaos', title: `Двойник, задержка ${d} с`,
    weight: 3, cd: 1200, dur: d, dangerous: false, objective: 'survive', minSec: 0, scene: 'atmos_doppel',
  }));
  [['elite', 'виникатор', 25], ['pack', 'стая зомби', 30], ['spider', 'пауки', 25], ['armored', 'бронированный', 35]].forEach(
    ([key, label, dur]) => out.push({
      id: `chaos_chase_${key}`, cat: 'chaos', title: `Погоня: ${label}`,
      weight: 5, cd: 800, dur: dur as number, dangerous: true, objective: 'survive', minSec: 0, scene: '',
    }),
  );
  [['low_gravity', 'Медленная гравитация'], ['rockfall', 'Камнепад'], ['night_fall', 'Резкая ночь'],
    ['sound_trap', 'Звуковая ловушка'], ['gust', 'Порыв ветра'], ['void_breath', 'Дыхание пустоты']].forEach(
    ([key, label]) => out.push({
      id: `chaos_anomaly_${key}`, cat: 'chaos', title: `Аномалия: ${label}`,
      weight: 4, cd: 700, dur: 25, dangerous: false, objective: 'none', minSec: 0, scene: '',
    }),
  );
  [['small', 5], ['mid', 8], ['big', 12]].forEach(([key, r]) => out.push({
    id: `chaos_arena_${key}`, cat: 'chaos', title: `Арена ${key} (радиус ${r})`,
    weight: 3, cd: 1000, dur: 40, dangerous: true, objective: 'survive', minSec: 0, scene: '',
  }));
  [['cows', 'коровы', 18], ['chickens', 'курицы', 30]].forEach(([key, label, n]) => out.push({
    id: `chaos_stampede_${key}`, cat: 'chaos', title: `Стадо: ${label} ×${n}`,
    weight: 3, cd: 900, dur: 20, dangerous: false, objective: 'none', minSec: 0, scene: '',
  }));
  ['fog_storm', 'thunder_roll', 'black_rain', 'static_sky'].forEach((key) => out.push({
    id: `chaos_sky_${key}`, cat: 'chaos', title: `Небо: ${key}`,
    weight: 3, cd: 800, dur: 30, dangerous: false, objective: 'none', minSec: 0, scene: '',
  }));
  [['armored_zombie', 1], ['speed_skeleton', 2], ['wither_duo', 2]].forEach(([key, n]) => out.push({
    id: `chaos_hunter_${key}`, cat: 'chaos', title: `Охотник: ${key} ×${n}`,
    weight: 4, cd: 850, dur: 30, dangerous: true, objective: 'hit', minSec: 0, scene: '',
  }));
  [['speedrun_roulette', 'Рулетка эффектов'], ['creeper_bait', 'Приманка для крипера'],
    ['mirror_time', 'Дежавю во времени'], ['phantom_night', 'Ночь фантомов'],
    ['ender_flutter', 'Рой частиц Края'], ['tnt_scare', 'Ложный подрыв']].forEach(([key, label]) => out.push({
    id: `chaos_misc_${key}`, cat: 'chaos', title: label,
    weight: 3, cd: 750, dur: 20, dangerous: false, objective: 'none', minSec: 0, scene: '',
  }));
  return out;
}

function minigames(): Ev[] {
  const out: Ev[] = [];
  [12, 22, 32].forEach((r) => out.push({
    id: `mini_reach_${r}`, cat: 'minigame', title: `Добеги до метки (${r} блоков)`,
    weight: 5, cd: 700, dur: 45, dangerous: false, objective: `reach ${r}`, minSec: 0, scene: '',
  }));
  [['minecraft:apple', 3], ['minecraft:coal', 6], ['minecraft:cobblestone', 12]].forEach(([item, n]) => out.push({
    id: `mini_collect_${(item as string).replace('minecraft:', '')}`, cat: 'minigame',
    title: `Собери ${(item as string).replace('minecraft:', '')} ×${n}`,
    weight: 5, cd: 700, dur: 60, dangerous: false, objective: `collect ${n}`, minSec: 0, scene: '',
  }));
  [['minecraft:apple', 2], ['minecraft:bread', 3]].forEach(([item, n]) => out.push({
    id: `mini_eat_${(item as string).replace('minecraft:', '')}`, cat: 'minigame',
    title: `Съешь ${(item as string).replace('minecraft:', '')} ×${n}`,
    weight: 4, cd: 800, dur: 40, dangerous: false, objective: `eat ${n}`, minSec: 0, scene: '',
  }));
  [8, 12].forEach((s) => out.push({
    id: `mini_still_${s}`, cat: 'minigame', title: `Не двигайся ${s} с`,
    weight: 3, cd: 800, dur: s, dangerous: false, objective: 'stand_still', minSec: 0, scene: '',
  }));
  [15, 25, 35].forEach((s) => out.push({
    id: `mini_survive_${s}`, cat: 'minigame', title: `Выдержи ${s} с под давлением`,
    weight: 4, cd: 800, dur: s, dangerous: true, objective: 'survive', minSec: 0, scene: '',
  }));
  [8, 14, 20].forEach((n) => out.push({
    id: `mini_break_${n}`, cat: 'minigame', title: `Разрушь ${n} блоков (в лимите урона миру)`,
    weight: 4, cd: 700, dur: 50, dangerous: false, objective: `break ${n}`, minSec: 0, scene: '',
  }));
  [3, 4, 5].forEach((n) => out.push({
    id: `mini_targets_${n}`, cat: 'minigame', title: `Порази цели ×${n} (бой разрешён только на цели)`,
    weight: 4, cd: 800, dur: 40, dangerous: false, objective: `hit ${n}`, minSec: 0, scene: '',
  }));
  [10, 20].forEach((n) => out.push({
    id: `mini_bridge_${n}`, cat: 'minigame', title: `Мост через провал (${n} блоков)`,
    weight: 3, cd: 900, dur: 50, dangerous: false, objective: `reach ${n}`, minSec: 0, scene: '',
  }));
  [0, 1].forEach((i) => out.push({
    id: `mini_find_chest_${i}`, cat: 'minigame', title: 'Найди сундук мода (не запечатанный)',
    weight: 3, cd: 900, dur: 60, dangerous: false, objective: 'reach 20', minSec: 0, scene: '',
  }));
  [8, 12].forEach((n) => out.push({
    id: `mini_dodge_${n}`, cat: 'minigame', title: `Увернись от метеоров (${n})`,
    weight: 4, cd: 700, dur: 25, dangerous: true, objective: 'survive', minSec: 0, scene: '',
  }));
  return out;
}

const moderationRows = [
  ['mod_check_rules', 'Проверка правил'], ['mod_check_tool', 'Проверка инструментов'],
  ['mod_check_combat', 'Проверка боя'], ['mod_inspector', 'Инспектор в мире'],
  ['mod_amnesty', 'Апелляция (минус один текущий страйк)'], ['mod_probation', 'Испытательный срок'],
  ['mod_clean_bonus', 'Бонус за чистую игру'], ['mod_fake_strike', 'Фейковый страйк (счётчик не трогается)'],
  ['mod_fake_ban_short', 'Фейковый бан I (раскрытие через 3 с)'], ['mod_fake_ban_long', 'Фейковый бан II'],
  ['mod_rules_reminder', 'Напоминание правил'], ['mod_stats', 'Сводка для зрителя'],
  ['mod_crown_preview', 'Превью короны'], ['mod_audit_chest', 'Аудит инвентаря'],
  ['mod_witness', 'Самозванец-модератор'], ['mod_timeout', 'Предупреждение о таймауте'],
  ['mod_cheat_scan', 'Скан обхода (только описанные случаи)'], ['mod_review', 'Разбор эпизода'],
  ['mod_bonus_exam', 'Экзамен на бонус (ошибка = минус бонус, не страйк)'],
  ['mod_strike_review', 'Разбор страйка'],
];

const atmosphereRows = [
  ['atmos_surveillance', 'Слежка'], ['atmos_whisper', 'Шёпот'], ['atmos_shadow', 'Тень за спиной'],
  ['atmos_echo', 'Эхо'], ['atmos_silence', 'Полная тишина'], ['atmos_tracks', 'Следы наблюдателя'],
  ['atmos_camera', 'Камера слежения'], ['atmos_glitch', 'Глитч (счётчик читается)'],
  ['atmos_fog_dense', 'Густой туман'], ['atmos_fog_light', 'Лёгкая дымка'], ['atmos_heartbeat', 'Сердцебиение'],
  ['atmos_eye', 'Глаз в темноте'], ['atmos_spotlight', 'Прожектор'], ['atmos_rain', 'Дождь'],
  ['atmos_thunder', 'Гром вдали'], ['atmos_dust', 'Пылинки'], ['atmos_ender_flutter', 'Флаттер Края'],
  ['atmos_static', 'Телевизионные помехи'], ['atmos_daytime', 'Смена времени'], ['atmos_red_moon', 'Красная луна'],
  ['atmos_cold_breath', 'Холодное дыхание'], ['atmos_drone', 'Дрон-наблюдатель'], ['atmos_lens', 'Блик'],
  ['atmos_vignette', 'Виньетка'], ['atmos_slow_zoom', 'Медленный наезд'], ['atmos_title_card', 'Титр'],
  ['atmos_sound_low', 'Приглушение'], ['atmos_quiet_rain', 'Тихий дождь'], ['atmos_breath', 'Задержка дыхания'],
  ['atmos_dejavu', 'Дежавю'],
];

const secretRows = [
  ['secret_fan_letter', 'Письмо фаната'], ['secret_fan_meeting', 'Редкий поклонник'],
  ['secret_room_beacon', 'Тайная комната'], ['secret_mystery_chest', 'Загадочный сундук'],
  ['secret_crown_hint', 'Намёк на корону'], ['secret_glitched_message', 'Глитч-сообщение'],
  ['secret_old_broadcast', 'Старый эфир'], ['secret_ghost_player', 'Игрок-призрак'],
  ['secret_twin_signal', 'Сигнал двойника'], ['secret_garden', 'Тайный сад'],
  ['secret_code_word', 'Кодовое слово'], ['secret_disc', 'Пластинка'],
  ['secret_endermite', 'Эндермит-шпион'], ['secret_whisperer_gift', 'Дар шёпота'],
  ['secret_final_twist_preview', 'Превью финального твиста|story_final_twist'],
];

function chat(): Ev[] {
  const out: Ev[] = [];
  [['chat_vote_gear', 'Чат выбирает: броня или еда'], ['chat_vote_weather', 'Чат выбирает: дождь или ясно'],
    ['chat_vote_night', 'Чат выбирает: ночь или день'], ['chat_vote_mobs', 'Чат выбирает: волна или тишина'],
    ['chat_vote_loot', 'Чат выбирает: редкий лут или опыт'],
    ['chat_vote_route', 'Чат выбирает: короткий путь или безопасный'],
    ['chat_vote_music', 'Чат выбирает: музыка или тишина'],
    ['chat_vote_meteor', 'Чат выбирает: метеоры или туман'],
    ['chat_vote_alarm', 'Чат выбирает: верить тревоге или нет']].forEach(([id, title]) => out.push({
    id, cat: 'chat', title, weight: 5, cd: 500, dur: 20, dangerous: false, objective: 'choice', minSec: 0,
    scene: 'twist_chat_choice',
  }));
  ['chat_risk_low', 'chat_risk_mid', 'chat_risk_high'].forEach((id) => out.push({
    id, cat: 'chat', title: 'Риск или безопасность', weight: 4, cd: 700, dur: 20, dangerous: false,
    objective: 'risk/safe', minSec: 0, scene: '',
  }));
  ['chat_rumor_stronghold', 'chat_rumor_dragon', 'chat_rumor_fake'].forEach((id) => out.push({
    id, cat: 'chat', title: 'Слух: зрители обсуждают', weight: 3, cd: 600, dur: 12, dangerous: false,
    objective: 'none', minSec: 0, scene: '',
  }));
  ['chat_suggest_tool', 'chat_suggest_food', 'chat_suggest_route'].forEach((id) => out.push({
    id, cat: 'chat', title: 'Предложение зрителей', weight: 3, cd: 600, dur: 10, dangerous: false,
    objective: 'none', minSec: 0, scene: '',
  }));
  out.push({
    id: 'chat_bribe', cat: 'chat', title: 'Подкуп чата (отказ без страйка)', weight: 2, cd: 900, dur: 20,
    dangerous: false, objective: 'risk/safe', minSec: 0, scene: '',
  });
  out.push({
    id: 'chat_poll_mood', cat: 'chat', title: 'Опрос настроения чата', weight: 4, cd: 400, dur: 12,
    dangerous: false, objective: 'choice', minSec: 0, scene: '',
  });
  return out;
}

function events(): Ev[] {
  const list: Ev[] = [
    ...gifts(),
    ...storyRows.map(([id, title, scene]) => ({
      id,
      cat: 'story' as EventCat,
      title,
      weight: 2,
      cd: 0,
      dur: 8,
      dangerous: false,
      objective: id === 'story_eyes_task' ? 'give eyes' : id === 'story_fast_travel' ? 'offer' : 'hint',
      minSec: id === 'story_stronghold_hint' ? 1800 : id === 'story_stronghold_coords' ? 2400
        : id === 'story_eyes_task' ? 3300 : id === 'story_fast_travel' ? 4200 : 0,
      scene,
    })),
    ...chaos(),
    ...minigames(),
    ...moderationRows.map(([id, title]) => ({
      id, cat: 'moderation' as EventCat, title, weight: 3, cd: 900, dur: 20, dangerous: false,
      objective: id.startsWith('mod_check') || id === 'mod_bonus_exam' ? 'quiz' : 'moderation', minSec: 0, scene: '',
    })),
    ...atmosphereRows.map(([id, title]) => ({
      id, cat: 'atmosphere' as EventCat, title, weight: 5, cd: 600, dur: 14, dangerous: false,
      objective: 'none', minSec: 0,
      scene: id === 'atmos_title_card' ? 'atmos_moderator_on_cam' : id === 'atmos_shadow' ? 'atmos_shadow_behind' : '',
    })),
    ...secretRows.map((row) => {
      const [id, raw] = row;
      const [title, scene] = raw.split('|');
      return {
        id, cat: 'secret' as EventCat, title, weight: 1, cd: 1800, dur: 15, dangerous: false,
        objective: 'secret', minSec: 0, scene: scene ?? '',
      };
    }),
    ...chat(),
  ];
  return list;
}

export const EVENTS: Ev[] = events();

export const EVENT_COUNTS = (Object.keys(CATEGORY_META) as EventCat[]).map((c) => ({
  cat: c,
  ...CATEGORY_META[c],
  count: EVENTS.filter((e) => e.cat === c).length,
}));

export const TOTAL_EVENTS = EVENTS.length;

export interface Cut {
  id: string;
  type: 'Сюжетные' | 'Драматические' | 'Атмосферные' | 'Твисты и секреты';
  title: string;
  dur: number;
  cam: string;
}

const CUT_ROWS: [string, Cut['type'], string, number, string][] = [
  ['story_air_start', 'Сюжетные', 'Эфир начался', 8, 'crane'],
  ['story_grace_end', 'Сюжетные', 'Грейс завершён', 5, 'zoom'],
  ['story_stronghold_mark', 'Сюжетные', 'Метка крепости', 4, 'orbit'],
  ['story_end_voice', 'Сюжетные', 'Голос Края', 6, 'zoom'],
  ['story_moderator_gift', 'Сюжетные', 'Дар модератора', 4, 'static'],
  ['story_portal_found', 'Сюжетные', 'Портал найден', 6, 'crane'],
  ['story_before_dragon', 'Сюжетные', 'Перед драконом', 8, 'orbit'],
  ['story_win', 'Сюжетные', 'Победа', 8, 'crane'],
  ['drama_first_strike', 'Драматические', 'Первый страйк — красная вспышка, 1/3', 5, 'anvil'],
  ['drama_second_strike', 'Драматические', 'Второй страйк — силуэт наковальни, 2/3', 6, 'anvil'],
  ['drama_ban', 'Драматические', 'Бан мира — [Выйти] / [Наблюдать]', 8, 'crane'],
  ['drama_death', 'Драматические', 'Смерть и некролог', 6, 'zoom'],
  ['drama_armor_low', 'Драматические', 'Броня на исходе', 4, 'static'],
  ['drama_last_chance', 'Драматические', 'Последний шанс', 5, 'zoom'],
  ['atmos_moderator_on_cam', 'Атмосферные', 'Модератор в кадре', 4, 'static'],
  ['atmos_whisper', 'Атмосферные', 'Шёпот', 4, 'static'],
  ['atmos_shadow_behind', 'Атмосферные', 'Тень за спиной', 4, 'orbit'],
  ['atmos_doppel', 'Атмосферные', 'Двойник', 5, 'zoom'],
  ['atmos_echo', 'Атмосферные', 'Эхо', 3, 'static'],
  ['atmos_silence', 'Атмосферные', 'Полная тишина', 4, 'static'],
  ['atmos_observer_tracks', 'Атмосферные', 'Следы наблюдателя', 4, 'crane'],
  ['atmos_camera_watch', 'Атмосферные', 'Камера слежения', 4, 'zoom'],
  ['twist_fake_ban_1', 'Твисты и секреты', 'Фейковый бан I (раскрытие ≤ 3 с)', 5, 'anvil'],
  ['twist_fake_ban_2', 'Твисты и секреты', 'Фейковый бан II', 6, 'anvil'],
  ['twist_fake_strike', 'Твисты и секреты', 'Фейковый страйк', 4, 'anvil'],
  ['twist_fake_win', 'Твисты и секреты', 'Фейковая победа', 5, 'crane'],
  ['twist_fake_loot', 'Твисты и секреты', 'Фальшивый лут', 4, 'static'],
  ['twist_imposter_mod', 'Твисты и секреты', 'Самозванец-модератор', 5, 'orbit'],
  ['twist_fake_portal', 'Твисты и секреты', 'Фейковый портал', 4, 'zoom'],
  ['twist_double_bluff', 'Твисты и секреты', 'Двойной обман', 5, 'orbit'],
  ['story_final_twist', 'Твисты и секреты', 'Финальный твист', 6, 'crane'],
  ['twist_moderator_crown', 'Твисты и секреты', 'Корона модератора (0 страйков)', 8, 'crane'],
];

export const CUTSCENES: Cut[] = CUT_ROWS.map(([id, type, title, dur, cam]) => ({ id, type, title, dur, cam }));

export const FILE_TREE: { path: string; note: string }[] = [
  { path: 'mod/build.gradle', note: 'loom 1.6, имя артефакта twitchmod-v1.1-<mc>' },
  { path: 'mod/gradle.properties', note: '1.20.1 / yarn 1.20.1+build.10 / loader 0.15.11 / fabric api 0.92.2' },
  { path: 'mod/settings.gradle', note: 'репозитории Fabric' },
  { path: 'mod/gradle/wrapper/gradle-wrapper.properties', note: 'Gradle 8.6' },
  { path: 'mod/src/main/resources/fabric.mod.json', note: 'entrypoints main + client, mixins' },
  { path: 'mod/src/main/resources/twitchmod.mixins.json', note: 'CameraMixin, defaultRequire = 0' },
  { path: 'mod/src/main/resources/assets/twitchmod/lang/ru_ru.json', note: 'русские тексты и субтитры' },
  { path: 'mod/src/main/resources/assets/twitchmod/lang/en_us.json', note: 'архитектура допускает локализацию' },
  { path: 'mod/src/main/java/dev/twitchmod/state/RunState.java', note: 'истинное состояние: 0/3 и всего страйков отдельно' },
  { path: 'mod/src/main/java/dev/twitchmod/state/StateManager.java', note: 'JSON в <мир>/twitchmod/run.json' },
  { path: 'mod/src/main/java/dev/twitchmod/rules/ActionGuard.java', note: 'отмена нарушения до ущерба' },
  { path: 'mod/src/main/java/dev/twitchmod/rules/ToolClass.java', note: 'топор/кирка/лопата/рука' },
  { path: 'mod/src/main/java/dev/twitchmod/rules/CombatPermissions.java', note: 'видимые окна на бой' },
  { path: 'mod/src/main/java/dev/twitchmod/rules/WorldDamageTracker.java', note: '160 блоков / 10 минут' },
  { path: 'mod/src/main/java/dev/twitchmod/rules/TempBlockManager.java', note: 'временные блоки ивентов' },
  { path: 'mod/src/main/java/dev/twitchmod/run/RunLifecycle.java', note: 'тик, страйки, 5 исходов, вердикт прицела' },
  { path: 'mod/src/main/java/dev/twitchmod/run/GraceManager.java', note: 'грейс, кожаный сет на 4:30, 15-сек отсчёт' },
  { path: 'mod/src/main/java/dev/twitchmod/run/EndRushManager.java', note: '5 минут на дракона' },
  { path: 'mod/src/main/java/dev/twitchmod/director/EventCatalog.java', note: '225 записей + валидатор' },
  { path: 'mod/src/main/java/dev/twitchmod/director/EventDirector.java', note: 'выбор раз в 30–90 с и ограничения' },
  { path: 'mod/src/main/java/dev/twitchmod/director/EventEffects.java', note: '41 тип действий, постановочные эффекты' },
  { path: 'mod/src/main/java/dev/twitchmod/cutscene/CutsceneLibrary.java', note: '32 сценария' },
  { path: 'mod/src/main/java/dev/twitchmod/progress/StoryProgress.java', note: 'гарантии 30/40/55/70 минут' },
  { path: 'mod/src/main/java/dev/twitchmod/attention/AttentionManager.java', note: 'внимание: 0 → уведомление, не страйк' },
  { path: 'mod/src/main/java/dev/twitchmod/log/EditorLog.java', note: 'CSV/JSONL для монтажёра' },
  { path: 'mod/src/main/java/dev/twitchmod/command/ModCommands.java', note: '/twitchmod ...' },
  { path: 'mod/src/main/java/dev/twitchmod/client/hud/TwitchHud.java', note: 'HUD по приоритетам из §10' },
  { path: 'mod/src/main/java/dev/twitchmod/client/cutscene/ClientCutscenePlayer.java', note: 'letterbox, субтитры, reduced motion' },
  { path: 'mod/src/main/java/dev/twitchmod/mixin/CameraMixin.java', note: 'кинокамера только на время сцены' },
  { path: 'mod/docs/ACCEPTANCE.md', note: '7 критериев приёмки + тесты' },
];

export const ANCHORS = [
  {
    title: '100 зрителей ворвались в мир',
    body: '100 фигур на экране, из них до 8 — настоящие преследователи. HUD честно пишет «100 зрителей / 8 охотников», задача — выжить 30 секунд.',
    badge: 'chaos_crowd_0',
  },
  {
    title: 'Метеоритный дождь',
    body: 'Точки падения и путь отхода видны заранее, кратеры временные и восстанавливаются, в смертельной ловушке ивент не запускается.',
    badge: 'chaos_meteor_обычный',
  },
  {
    title: 'Двойник',
    body: 'Фигура с обликом игрока повторяет записанные движения с задержкой 1,5 с, не атакует, исчезает с эффектом. Бить без разрешения нельзя — это видно при наведении.',
    badge: 'chaos_doppelganger_45',
  },
  {
    title: 'Чат выбирает',
    body: 'Два исхода, проценты, результат. Надпись «ЧАТ МОДА — СИМУЛЯЦИЯ» обязательна и нарисована всегда.',
    badge: 'chat_vote_gear',
  },
  {
    title: 'Риск или безопасность',
    body: 'Сложное испытание с хорошей наградой или спокойный вариант со скромной. Отказ не создаёт страйк.',
    badge: 'chat_risk_mid',
  },
  {
    title: 'Апелляция',
    body: 'Доступна при двух текущих страйках: добровольное испытание снимает один текущий страйк, но не возвращает право на идеальную концовку.',
    badge: 'mod_amnesty',
  },
  {
    title: 'Мини-босс / охотник',
    body: 'Разрешение на бой выдаётся до появления противника. Провал за 60 секунд лишает награды или одного сердца, но не карает за разрешённые удары.',
    badge: 'chaos_hunter_wither_duo',
  },
  {
    title: 'Фейковый бан',
    body: 'Постановочная катсцена раскрывается максимум через 3 секунды. Счётчик настоящих страйков остаётся прежним.',
    badge: 'twist_fake_ban_1',
  },
];

export const SAMPLE_LOG = [
  ['19:04:11.204', 'PHASE', 'GRACE', 'START', 'REAL', 0, 0, ''],
  ['19:07:22.884', 'EVENT', 'gift_common_chest_drop', 'START', 'REAL', 0, 191, 'gift'],
  ['19:07:38.101', 'EVENT', 'gift_common_chest_drop', 'SUCCESS', 'REAL', 0, 206, ''],
  ['19:09:02.556', 'CUTSCENE', 'drama_first_strike', 'START', 'REAL', 1, 291, ''],
  ['19:09:07.601', 'CUTSCENE', 'drama_first_strike', 'END', 'REAL', 1, 296, ''],
  ['19:14:40.012', 'EVENT', 'chaos_crowd_0', 'START', 'REAL', 1, 628, 'chaos'],
  ['19:15:10.330', 'EVENT', 'chaos_crowd_0', 'SUCCESS', 'REAL', 1, 658, '100 зрителей / 8 охотников'],
  ['19:21:14.770', 'EVENT', 'twist_fake_ban_1', 'START', 'FAKE', 1, 1022, 'twist'],
  ['19:21:17.990', 'EVENT', 'twist_fake_ban_1', 'REVEAL', 'FAKE', 1, 1026, 'strikes unchanged'],
  ['19:38:52.140', 'STORY', 'fast_travel', 'USED', 'REAL', 2, 2320, ''],
  ['19:52:03.905', 'PHASE', 'END_RUSH', 'START', 'REAL', 2, 3151, ''],
  ['19:57:11.442', 'OUTCOME', 'WIN', 'END', 'REAL', 2, 3459, 'strikes_total=2'],
];
