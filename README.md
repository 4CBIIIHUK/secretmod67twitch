# Поставка: «Minecraft, но по правилам Твича»

```
mod/      Fabric-проект мода (готов к сборке: build.gradle, gradle.properties, исходники, ресурсы, локализация)
src/      исходники сайта-консоли поставки (реестр 225 событий, 32 катсцены, HUD-превью, приёмка)
```

## Что делать

```bash
cd mod
gradle wrapper --gradle-version 8.6
./gradlew build          # build/libs/twitchmod-v1.1-1.20.1.jar
```

 jar кладётся в `mods/` рядом с Fabric API (Minecraft Java 1.20.1, одиночный мир).

## Что реализовано в моде

| Слой | Файлы |
|---|---|
| Истинное состояние забега (0/3 и всего страйков отдельно) | `state/RunState`, `state/StateManager` (JSON в `<мир>/twitchmod/run.json`) |
| Правила и отмена нарушений до ущерба | `rules/ActionGuard`, `ToolClass`, `CombatPermissions`, `WorldDamageTracker`, `TempBlockManager` |
| Грейс 5:00, кожаный сет на 4:30, отсчёт 15 с, окно 30 с на броню | `run/GraceManager` |
| Страйки, варны, 5 исходов, вердикт прицела, запрет креатива | `run/RunLifecycle` |
| 225 событий в 8 категориях + валидатор каталога | `director/EventCatalog`, `EventDirector`, `EventEffects`, `Scheduler` |
| 32 катсцены с таймлайном и защитой от фейков | `cutscene/*` |
| Гарантии сюжета 30 / 40–45 / 55–60 / 70–80 минут | `progress/StoryProgress` |
| Внимание и комбо | `attention/*` |
| HUD, субтитры, экраны выбора и бана, кинокамера | `client/*`, `mixin/CameraMixin` |
| Лог для монтажёра, экспорт реестра, команды | `log/EditorLog`, `command/ModCommands` |

Подробности сборки, мульти-версии и правила — в `mod/README.md`, критерии приёмки — в `mod/docs/ACCEPTANCE.md`.

Логотипы Twitch и чужие звуки донатов не используются: модерация в игре вымышленная,
это правила испытания, а не правила платформы.
