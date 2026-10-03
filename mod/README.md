# twitchmod — «Майнкрафт, но по правилам Твича»

Мод для **Minecraft Java Edition 1.20.1 (Fabric, одиночный мир)**: вымышленная модерация,
грейс 5 минут, 4 правила, страйки, 225 событий в 8 категориях, 32 катсцены,
5-минутный финальный рывок на дракона и 5 исходов забега.

Логотипы Twitch, чужие звуки донатов и другие защищённые материалы не используются.

---

## 1. Сборка

Требуется JDK 17 и Gradle 8.4+ (или `gradle wrapper` один раз, чтобы появился `gradlew`).

```bash
cd mod
gradle wrapper --gradle-version 8.6      # создаёт gradlew / gradlew.bat
./gradlew build                          # -> build/libs/twitchmod-v1.1-1.20.1.jar
```

Готовый jar кладётся в `mods/` вместе с Fabric API.

### Матрица версий

Логика изолирована от маппингов в небольших адаптерах
(`rules/ToolClass`, `net/ModNet`, `director/EventEffects$asId`), поэтому перенос на другую
линию — это правка `gradle.properties` + точечные правки адаптеров:

```bash
./gradlew build -Pminecraft_version=1.20.4 -Pyarn_mappings=1.20.4+build.3 -Pfabric_version=0.97.2+1.20.4
```

Имена артефактов уже соответствуют формату `twitchmod-v1.1-<версия>.jar`
(см. `build.gradle`: `version = "${mod_version}-${minecraft_version}"`).

---

## 2. Что внутри

```
src/main/java/dev/twitchmod/
  TwitchMod.java            точка входа, порядок инициализации
  TwitchModConfig.java      сложность, доступность, звук, эффекты (config/twitchmod/config.json)
  state/                    RunState (истинное состояние забега), Phase, RunOutcome, StateManager (JSON в мире)
  rules/                    ActionGuard, ToolClass, CombatPermissions, WorldDamageTracker, TempBlockManager
  run/                      RunLifecycle, GraceManager, EndRushManager, RunStateHolder
  director/                 EventDirector, EventCatalog (225), EventEffects, Scheduler, Choices, EventMetrics
  cutscene/                 CutsceneLibrary (32), CutsceneDefinition, CutsceneDirector
  progress/                 StoryProgress — гарантии 30 / 40–45 / 55–60 / 70–80 минут
  attention/                AttentionManager, ComboManager
  net/                      ModNet (S2C/C2S), Bufs, Msg
  command/                  ModCommands — /twitchmod ...
  log/                      EditorLog — CSV/JSONL для монтажёра
  client/                   ClientState, ClientNet, hud/TwitchHud, ui/*, cutscene/ClientCutscenePlayer
  mixin/CameraMixin         кинематографическая камера (только на время сцены)
src/main/resources/
  fabric.mod.json, twitchmod.mixins.json
  assets/twitchmod/lang/ru_ru.json, en_us.json, icon.png
```

---

## 3. Правила (то, что видит игрок)

> **Полная броня. Правильный инструмент. Не бей мобов. Береги мир.**
> Нарушение = **WARN + страйк**. Три страйка = бан забега.

| Действие | Мод |
|---|---|
| Дерево | топором; неверный инструмент — страйк, блок не разрушается |
| Камень / руда / обсидиан / блоки Незера и Края | киркой |
| Земля / песок / гравий | лопатой |
| Трава, цветы, культуры, листва, слой снега | рукой |
| Свои поставленные блоки | любым удобным инструментом, но в счётчик урона миру идут |
| Удар по мобу | вне окна на бой — страйк, удар отменяется |
| Запечатанный сундук структуры | попытка открыть/сломать — страйк, содержимое не выдаётся |
| Поджог | 1 страйк |
| TNT / поджог крипера игроком | 2 страйка, два последовательных WARN |
| Команды/креатив | забег `INVALID_FOR_LEADERBOARD` (детектируются только описанные случаи) |

Все запреты работают **до** нанесения ущерба: блок остаётся на месте, удар не доходит,
сундук не открывается. Повторная попытка в течение 3 секунд не создаёт серию страйков.

Урон миру: 160 блоков за 10 активных минут, предупреждение на 120, лимит отменяет разрушение
и даёт **один** страйк на весь период исчерпания.

---

## 4. Команды

```
/twitchmod status                    состояние забега и каталога
/twitchmod start | reset             запустить эфир / новый забег
/twitchmod strike "<reason>"         настоящий страйк (тест приёмки)
/twitchmod amnesty                   амнистия: -1 текущий страйк, история остаётся
/twitchmod grace 300                 длительность грейса
/twitchmod event <id>                запустить событие из каталога
/twitchmod events                    реестр: 225 записей, счётчики по категориям, проблемы валидатора
/twitchmod cutscene <id> | cutscenes
/twitchmod seal                      пометить сундуки рядом как запечатанные (демо правила)
/twitchmod broll on|off              B-roll просмотр: забег помечается незачётным
/twitchmod export registry|log       CSV в logs/twitchmod/
```

---

## 5. Данные на диске

| Путь | Что там |
|---|---|
| `<мир>/twitchmod/run.json` | истинное состояние забега, восстанавливается после перезапуска |
| `config/twitchmod/config.json` | сложность, доступность, звук, лимиты |
| `logs/twitchmod/editor_log_*.csv` | лог монтажёра: события, страйки, катсцены, исходы, REAL/FAKE |
| `logs/twitchmod/event_registry_*.csv` | проверяемый реестр 225 событий |

---

## 6. Производительность

* «100 зрителей» — бронированные стойки без ИИ + 8 настоящих охотников, HUD пишет честное число.
* Все временные блоки и мобы удаляются: по TTL, при смене измерения, при бане/смерти и при сохранении.
* `TwitchModConfig.reducedMotion / reducedFlashes / subtitles / volume*` — доступность.
* `defaultRequire = 0` в `twitchmod.mixins.json`: сбой миксина камеры не ломает забег.

## 7. Совместимость

Базовая цель — Fabric 1.20.1. Sodium/Iris проверяются отдельно; OptiFine и Forge-порт
не подразумеваются автоматически (см. `docs/ACCEPTANCE.md`).
