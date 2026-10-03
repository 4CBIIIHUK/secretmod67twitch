import { useMemo, useState } from 'react';
import {
  ANCHORS,
  CATEGORY_META,
  CUTSCENES,
  EVENTS,
  EVENT_COUNTS,
  FILE_TREE,
  SAMPLE_LOG,
  TOTAL_EVENTS,
  type EventCat,
} from './data/catalog';

const PURPLE = '#9146FF';

function Section({
  id,
  kicker,
  title,
  children,
}: {
  id: string;
  kicker: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section id={id} className="scroll-mt-20 border-t border-white/10 py-16">
      <p className="font-mono text-xs uppercase tracking-[0.25em]" style={{ color: PURPLE }}>
        {kicker}
      </p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight text-white sm:text-4xl">{title}</h2>
      <div className="mt-8">{children}</div>
    </section>
  );
}

function HudMock() {
  const [strikes, setStrikes] = useState(0);
  const [attention, setAttention] = useState(78);
  const [permit, setPermit] = useState(false);
  const [verdict, setVerdict] = useState<'ok' | 'tool' | 'limit'>('ok');

  return (
    <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
      <div className="relative aspect-video overflow-hidden rounded-2xl border border-white/10 bg-[radial-gradient(circle_at_30%_20%,#241a3d,#0a0a12_60%)]">
        <div className="absolute inset-0 opacity-30 [background-image:linear-gradient(#ffffff08_1px,transparent_1px),linear-gradient(90deg,#ffffff08_1px,transparent_1px)] [background-size:28px_28px]" />
        {/* top-left: attention */}
        <div className="absolute left-4 top-4 font-mono text-[11px] text-white/80">
          <div className="text-[11px] uppercase tracking-widest" style={{ color: PURPLE }}>
            Внимание
          </div>
          <div className="mt-1 h-2 w-32 bg-black/60">
            <div className="h-2" style={{ width: `${attention}%`, background: attention < 25 ? '#FF4444' : PURPLE }} />
          </div>
          <div className="mt-2 text-white/60">Урон миру: 96/160</div>
          <div className="text-orange-300">Грейс закончится через 42 с</div>
        </div>
        {/* top-right: strikes */}
        <div className="absolute right-4 top-4 text-right font-mono text-[11px] text-white/80">
          <div className="text-white">Страйки: {strikes}/3</div>
          <div className="mt-1 flex justify-end gap-1">
            {[0, 1, 2].map((i) => (
              <span
                key={i}
                className="flex h-5 w-5 items-center justify-center border text-[10px]"
                style={{
                  borderColor: i < strikes ? '#FF4444' : 'rgba(255,255,255,.25)',
                  color: i < strikes ? '#FF4444' : 'rgba(255,255,255,.35)',
                  background: i < strikes ? 'rgba(255,68,68,.15)' : 'transparent',
                }}
              >
                {i < strikes ? 'X' : '-'}
              </span>
            ))}
          </div>
          <div className="mt-1" style={{ color: PURPLE }}>
            Комбо: 4
          </div>
          <div className="text-emerald-300">Идеальный забег сохранён</div>
        </div>
        {/* bottom-right: timers */}
        <div className="absolute bottom-4 right-4 text-right font-mono text-[11px] text-orange-300">
          <div>ГРЕЙС 0:42</div>
          <div className="mt-1 text-white/60">Следующее событие: 18 с</div>
        </div>
        {/* center crosshair */}
        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 text-center">
          <div className="mx-auto h-4 w-4 border border-white/70" />
          <div
            className="mt-3 inline-block border px-2 py-1 font-mono text-[11px]"
            style={{
              borderColor: verdict === 'ok' ? '#44DD88' : '#FF4444',
              color: verdict === 'ok' ? '#44DD88' : '#FF4444',
              background: 'rgba(0,0,0,.65)',
            }}
          >
            {verdict === 'ok' ? 'МОЖНО' : verdict === 'tool' ? '! СТРАЙК: неверный инструмент' : '! СТРАЙК: лимит урона миру'}
          </div>
        </div>
        {permit && (
          <div className="absolute bottom-16 left-1/2 -translate-x-1/2 border border-emerald-400/60 bg-black/70 px-3 py-1 font-mono text-[11px] text-emerald-300">
            БОЙ РАЗРЕШЁН: волна зомби — осталось 24 с
          </div>
        )}
      </div>

      <div className="space-y-3 rounded-2xl border border-white/10 bg-white/[0.02] p-5">
        <p className="text-sm text-white/70">Приоритет HUD из §10 ТЗ: зритель всегда видит настоящие страйки, текущую цель и приближение события.</p>
        <div className="grid grid-cols-2 gap-2 font-mono text-xs">
          <button onClick={() => setStrikes((s) => (s + 1) % 4)} className="border border-white/15 px-3 py-2 text-left text-white/80 transition hover:border-white/40">
            страйк → {strikes}/3
          </button>
          <button onClick={() => setAttention((a) => (a <= 10 ? 92 : a - 22))} className="border border-white/15 px-3 py-2 text-left text-white/80 transition hover:border-white/40">
            внимание → {attention}
          </button>
          <button onClick={() => setPermit((p) => !p)} className="border border-white/15 px-3 py-2 text-left text-white/80 transition hover:border-white/40">
            разрешение на бой
          </button>
          <button
            onClick={() => setVerdict(verdict === 'ok' ? 'tool' : verdict === 'tool' ? 'limit' : 'ok')}
            className="border border-white/15 px-3 py-2 text-left text-white/80 transition hover:border-white/40"
          >
            вердикт прицела
          </button>
        </div>
        <p className="font-mono text-[11px] text-white/40">
          Цвет никогда не единственный носитель смысла: рядом текст и иконки (X / −, «МОЖНО» / «! СТРАЙК»).
        </p>
      </div>
    </div>
  );
}

export default function App() {
  const [cat, setCat] = useState<EventCat | 'all'>('all');
  const [query, setQuery] = useState('');

  const filtered = useMemo(
    () =>
      EVENTS.filter(
        (e) =>
          (cat === 'all' || e.cat === cat) &&
          (query.trim() === '' || (e.id + e.title).toLowerCase().includes(query.toLowerCase())),
      ),
    [cat, query],
  );

  return (
    <div className="min-h-screen bg-[#08070d] text-white antialiased">
      <div className="pointer-events-none fixed inset-0 opacity-[0.55] [background:radial-gradient(900px_500px_at_15%_-10%,#9146ff33,transparent),radial-gradient(700px_400px_at_85%_10%,#ff444422,transparent)]" />

      <header className="sticky top-0 z-30 border-b border-white/10 bg-[#08070d]/85 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-5 py-3">
          <div className="flex h-9 w-9 items-center justify-center border font-mono text-xs" style={{ borderColor: PURPLE, color: PURPLE }}>
            3/3
          </div>
          <div className="mr-auto">
            <p className="text-sm font-semibold leading-tight">Minecraft, но по правилам Твича</p>
            <p className="font-mono text-[11px] text-white/40">twitchmod v1.1 · Fabric 1.20.1</p>
          </div>
          <nav className="hidden gap-5 font-mono text-xs text-white/50 md:flex">
            <a href="#build" className="hover:text-white">Сборка</a>
            <a href="#catalog" className="hover:text-white">Каталог</a>
            <a href="#cutscenes" className="hover:text-white">Катсцены</a>
            <a href="#hud" className="hover:text-white">HUD</a>
            <a href="#accept" className="hover:text-white">Приёмка</a>
            <a href="#files" className="hover:text-white">Файлы</a>
          </nav>
        </div>
      </header>

      <main className="relative mx-auto max-w-6xl px-5 pb-24">
        {/* hero */}
        <div className="pt-16 sm:pt-24">
          <p className="font-mono text-xs uppercase tracking-[0.3em]" style={{ color: PURPLE }}>
            поставка мода · папка mod/
          </p>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold leading-[1.05] tracking-tight sm:text-6xl">
            Пять минут на подготовку. Потом <span style={{ color: PURPLE }}>каждое нарушение — варн и страйк</span>.
          </h1>
          <p className="mt-6 max-w-2xl text-lg text-white/60">
            Готовый к сборке Fabric-проект: серверная логика правил и забега, директор на {TOTAL_EVENTS} событий,
            32 катсцены, HUD, субтитры, настройки доступности и лог для монтажёра.
          </p>
          <div className="mt-8 grid gap-3 sm:grid-cols-4">
            {[
              [`${TOTAL_EVENTS}`, 'событий в 8 категориях'],
              ['32', 'катсцены, 12–20 за забег'],
              ['0→3', 'страйка до бана мира'],
              ['5:00', 'грейс и 5:00 на дракона'],
            ].map(([big, small]) => (
              <div key={small} className="border border-white/10 bg-white/[0.03] p-4">
                <p className="font-mono text-2xl" style={{ color: PURPLE }}>{big}</p>
                <p className="mt-1 text-xs text-white/50">{small}</p>
              </div>
            ))}
          </div>
          <div className="mt-8 flex flex-wrap gap-3">
            <a href="#build" className="bg-white px-5 py-3 font-mono text-sm text-black transition hover:bg-white/85">
              Как собрать jar
            </a>
            <a href="#catalog" className="border border-white/20 px-5 py-3 font-mono text-sm text-white/80 transition hover:border-white/50">
              Смотреть реестр событий
            </a>
          </div>
        </div>

        <Section id="build" kicker="сборка" title="Проект собирается одной командой">
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="space-y-4">
              <pre className="overflow-x-auto border border-white/10 bg-black/50 p-4 font-mono text-[12px] leading-relaxed text-white/80">
{`cd mod
gradle wrapper --gradle-version 8.6     # один раз
./gradlew build
# -> build/libs/twitchmod-v1.1-1.20.1.jar`}
              </pre>
              <p className="text-sm text-white/60">
                Нужен JDK 17 и Gradle 8.4+. Готовый jar кладётся в <span className="font-mono text-white/80">mods/</span> вместе с
                Fabric API. Имя артефакта уже в формате{' '}
                <span className="font-mono text-white/80">twitchmod-v1.1-&lt;версия&gt;.jar</span>.
              </p>
              <p className="border-l-2 pl-3 text-sm text-white/50" style={{ borderColor: PURPLE }}>
                Я не могу запустить Gradle внутри этой среды — поэтому в <span className="font-mono">mod/</span> лежит полный
                исходный проект, собранный по всем правилам Fabric/Loom: одна команда даёт jar.
              </p>
            </div>
            <div className="space-y-3">
              <p className="font-mono text-xs uppercase tracking-widest text-white/40">матрица версий</p>
              {[
                ['1.20.1', 'базовая цель, тестируется по умолчанию'],
                ['1.20.4', 'правка gradle.properties + адаптеры'],
                ['1.21.4 / 1.21.11', 'Identifier.of, PACKET_CODECS в адаптерах'],
                ['1.16.5 / Forge', 'отдельный порт, не подразумевается автоматически'],
              ].map(([v, note]) => (
                <div key={v} className="flex items-baseline gap-3 border border-white/10 bg-white/[0.02] px-4 py-3">
                  <span className="w-32 shrink-0 font-mono text-sm text-white">{v}</span>
                  <span className="text-xs text-white/50">{note}</span>
                </div>
              ))}
              <pre className="overflow-x-auto border border-white/10 bg-black/40 p-3 font-mono text-[11px] text-white/60">
{`./gradlew build -Pminecraft_version=1.20.4 \\
  -Pyarn_mappings=1.20.4+build.3 \\
  -Pfabric_version=0.97.2+1.20.4`}
              </pre>
            </div>
          </div>
        </Section>

        <Section id="systems" kicker="механики" title="Системы, которые формируют шоу">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ['Директор событий', 'выбор раз в 30–90 активных секунд, один опасный ивент, максимум два хаоса подряд, проверка ресурсов и пути отхода'],
              ['Проверка правил', 'нарушение отменяется до ущерба: блок остаётся, удар не доходит, сундук не открывается'],
              ['Гарантии сюжета', '30 мин намёк, 40–45 координаты, 55–60 глаза, 70–80 добровольный перенос'],
              ['Шкала внимания', 'ноль внимания даёт уведомление и событие, но никогда не страйк'],
              ['Комбо', '+1 за чистое испытание, бонус каждые пять, настоящий страйк сбрасывает'],
              ['Разрешения на бой', 'видимое окно с целью и таймером до начала испытания'],
              ['Катсцены', '32 сценария, 3–8 с, субтитры, reduced motion, камера возвращается корректно'],
              ['Финал и исходы', '5 минут на дракона, победа / идеал / бан / смерть / эфир завершён'],
            ].map(([t, d]) => (
              <div key={t} className="border border-white/10 bg-white/[0.02] p-4 transition hover:border-white/25">
                <p className="font-mono text-sm text-white">{t}</p>
                <p className="mt-2 text-xs leading-relaxed text-white/50">{d}</p>
              </div>
            ))}
          </div>
        </Section>

        <Section id="catalog" kicker="реестр" title={`${TOTAL_EVENTS} событий, проверяемых валидатором`}>
          <div className="mb-6 flex flex-wrap gap-2">
            <button
              onClick={() => setCat('all')}
              className="border px-3 py-2 font-mono text-xs transition"
              style={{
                borderColor: cat === 'all' ? PURPLE : 'rgba(255,255,255,.15)',
                color: cat === 'all' ? '#fff' : 'rgba(255,255,255,.6)',
                background: cat === 'all' ? 'rgba(145,70,255,.12)' : 'transparent',
              }}
            >
              все {TOTAL_EVENTS}
            </button>
            {EVENT_COUNTS.map((c) => (
              <button
                key={c.cat}
                onClick={() => setCat(c.cat)}
                className="border px-3 py-2 font-mono text-xs transition"
                style={{
                  borderColor: cat === c.cat ? PURPLE : 'rgba(255,255,255,.15)',
                  color: cat === c.cat ? '#fff' : 'rgba(255,255,255,.6)',
                  background: cat === c.cat ? 'rgba(145,70,255,.12)' : 'transparent',
                }}
              >
                {c.label} {c.count}/{c.target}
              </button>
            ))}
          </div>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="поиск по id или названию…"
            className="mb-4 w-full border border-white/10 bg-black/40 px-4 py-2 font-mono text-sm text-white outline-none placeholder:text-white/30 focus:border-white/40"
          />
          <div className="max-h-[420px] overflow-y-auto border border-white/10">
            <table className="w-full text-left font-mono text-[11px]">
              <thead className="sticky top-0 bg-[#0d0c15] text-white/40">
                <tr>
                  <th className="px-3 py-2">id</th>
                  <th className="px-3 py-2">название</th>
                  <th className="px-3 py-2">кат.</th>
                  <th className="px-3 py-2">вес</th>
                  <th className="px-3 py-2">кулдаун</th>
                  <th className="px-3 py-2">длит.</th>
                  <th className="px-3 py-2">цель</th>
                  <th className="px-3 py-2">мин. с</th>
                  <th className="px-3 py-2">катсцена</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((e) => (
                  <tr key={e.id} className="border-t border-white/5 hover:bg-white/[0.03]">
                    <td className="whitespace-nowrap px-3 py-1.5 text-white/80">{e.id}</td>
                    <td className="px-3 py-1.5 text-white/60">{e.title}</td>
                    <td className="px-3 py-1.5" style={{ color: e.dangerous ? '#FF4444' : 'rgba(255,255,255,.45)' }}>
                      {CATEGORY_META[e.cat].label}
                    </td>
                    <td className="px-3 py-1.5 text-white/40">{e.weight}</td>
                    <td className="px-3 py-1.5 text-white/40">{e.cd}s</td>
                    <td className="px-3 py-1.5 text-white/40">{e.dur}s</td>
                    <td className="px-3 py-1.5 text-white/40">{e.objective}</td>
                    <td className="px-3 py-1.5" style={{ color: e.minSec ? PURPLE : 'rgba(255,255,255,.3)' }}>
                      {e.minSec || '—'}
                    </td>
                    <td className="px-3 py-1.5 text-white/40">{e.scene || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-3 font-mono text-[11px] text-white/40">
            {filtered.length} записей показано · тот же список экспортируется командой{' '}
            <span className="text-white/70">/twitchmod export registry</span> → logs/twitchmod/event_registry_*.csv
          </p>

          <div className="mt-10 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {ANCHORS.map((a) => (
              <div key={a.badge} className="flex h-full flex-col border border-white/10 bg-white/[0.02] p-4">
                <p className="font-mono text-[10px] uppercase tracking-widest" style={{ color: PURPLE }}>{a.badge}</p>
                <p className="mt-2 text-sm font-semibold text-white">{a.title}</p>
                <p className="mt-2 text-xs leading-relaxed text-white/50">{a.body}</p>
              </div>
            ))}
          </div>
        </Section>

        <Section id="cutscenes" kicker="катсцены" title="32 сценария, отдельный слой каталога">
          <div className="grid gap-4 lg:grid-cols-2">
            {(['Сюжетные', 'Драматические', 'Атмосферные', 'Твисты и секреты'] as const).map((type) => (
              <div key={type} className="border border-white/10 bg-white/[0.02] p-4">
                <p className="font-mono text-xs uppercase tracking-widest" style={{ color: PURPLE }}>
                  {type} · {CUTSCENES.filter((c) => c.type === type).length}
                </p>
                <ul className="mt-3 space-y-1.5">
                  {CUTSCENES.filter((c) => c.type === type).map((c) => (
                    <li key={c.id} className="flex items-baseline gap-2 text-xs">
                      <span className="font-mono text-white/80">{c.title}</span>
                      <span className="ml-auto shrink-0 font-mono text-white/35">{c.dur}s · {c.cam}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {[
              ['Настоящий страйк', 'красная вспышка, причина, 1/3 → 2/3 и «Следующий — бан»'],
              ['Настоящий бан', 'камера отъезжает, наковальня, красная сетка, кнопки [Выйти] [Наблюдать]'],
              ['Фейковый бан', 'раскрытие ≤ 3 с, не запускается при 2 настоящих страйках, в бою и в Крае'],
            ].map(([t, d]) => (
              <div key={t} className="border-l-2 pl-3" style={{ borderColor: PURPLE }}>
                <p className="text-sm font-semibold">{t}</p>
                <p className="mt-1 text-xs text-white/50">{d}</p>
              </div>
            ))}
          </div>
        </Section>

        <Section id="hud" kicker="интерфейс" title="HUD, который читается за секунду">
          <HudMock />
        </Section>

        <Section id="log" kicker="монтаж" title="Лог, по которому режется ролик">
          <div className="overflow-x-auto border border-white/10">
            <table className="w-full text-left font-mono text-[11px]">
              <thead className="bg-[#0d0c15] text-white/40">
                <tr>
                  <th className="px-3 py-2">time</th>
                  <th className="px-3 py-2">kind</th>
                  <th className="px-3 py-2">id</th>
                  <th className="px-3 py-2">outcome</th>
                  <th className="px-3 py-2">notification</th>
                  <th className="px-3 py-2">strikes</th>
                  <th className="px-3 py-2">active_s</th>
                  <th className="px-3 py-2">detail</th>
                </tr>
              </thead>
              <tbody>
                {SAMPLE_LOG.map((row, i) => (
                  <tr key={i} className="border-t border-white/5">
                    {row.map((cell, j) => (
                      <td
                        key={j}
                        className="whitespace-nowrap px-3 py-1.5"
                        style={{ color: cell === 'FAKE' ? '#FFAA33' : cell === 'REAL' ? '#44DD88' : 'rgba(255,255,255,.7)' }}
                      >
                        {cell || '—'}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-3 text-sm text-white/50">
            Экспорт CSV и JSONL, режим B-roll помечает забег как незачётный, кадры из разных попыток не смешиваются.
          </p>
        </Section>

        <Section id="accept" kicker="приёмка" title="Критерии полной версии и их проверка">
          <div className="grid gap-3 lg:grid-cols-2">
            {[
              ['1. Реестр 225 событий и 32 катсцен', 'EventCatalog.validate() сверяет счётчики категорий, ссылки на катсцены и типы эффектов; /twitchmod events печатает проблемы'],
              ['2. До дракона можно дойти самому', 'StoryProgress: подсказки только после 30 минут, глаза к 55–60, перенос к 70–80'],
              ['3. Прохождения с 0, 1, 2, 3 страйками', '/twitchmod strike "<reason>" и /twitchmod amnesty для четырёх финалов'],
              ['4. Ивент не выдаёт страйк', 'единственная точка RunLifecycle.strike(); вызывается только из ActionGuard и GraceManager'],
              ['5. Читы не «полностью обнаружимы»', 'детектируются только креатив/спектатор и B-roll → статус INVALID_FOR_LEADERBOARD'],
              ['6. Очистка и сохранение', 'TempBlockManager + EventDirector.cleanupAll + StateManager.sanitizeForSave, мир не удаляется'],
              ['7. Понятность ставки за 15 секунд', 'вступление, HUD со страйками и катсцена первого страйка'],
              ['8. Производительность', '100 зрителей = стойки без ИИ + 8 охотников; цель 60 FPS, ≥45 FPS в тяжёлой сцене, ≥18 TPS'],
            ].map(([t, d]) => (
              <div key={t} className="border border-white/10 bg-white/[0.02] p-4">
                <p className="text-sm font-semibold text-white">{t}</p>
                <p className="mt-2 text-xs leading-relaxed text-white/50">{d}</p>
              </div>
            ))}
          </div>
        </Section>

        <Section id="files" kicker="поставка" title="Что лежит в папке mod/">
          <div className="divide-y divide-white/5 border border-white/10">
            {FILE_TREE.map((f) => (
              <div key={f.path} className="flex flex-wrap items-baseline gap-x-4 gap-y-1 px-4 py-2.5 hover:bg-white/[0.03]">
                <span className="font-mono text-[11px] text-white/85">{f.path}</span>
                <span className="ml-auto text-right text-[11px] text-white/40">{f.note}</span>
              </div>
            ))}
          </div>
          <div className="mt-6 border-l-2 pl-4 text-sm text-white/60" style={{ borderColor: PURPLE }}>
            <p>
              Скачайте папку <span className="font-mono text-white">mod/</span> целиком — внутри готовый Gradle-проект
              (исходники, ресурсы, локализация, миксины, документация приёмки). Сборка jar выполняется локально одной
              командой, потому что Loom тянет Minecraft и маппинги из сети при первом запуске.
            </p>
          </div>
        </Section>
      </main>

      <footer className="border-t border-white/10 px-5 py-10">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-4 font-mono text-[11px] text-white/35">
          <span>twitchmod v1.1 · Fabric 1.20.1 · {TOTAL_EVENTS} событий · 32 катсцены</span>
          <span className="ml-auto">Модерация в игре вымышленная: это правила испытания, а не правила платформы.</span>
        </div>
      </footer>
    </div>
  );
}
