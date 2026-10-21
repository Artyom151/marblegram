# Marblegram Desktop — полный редизайн tdesktop в Material 3 Expressive

> Спецификация и пошаговый план перевода **Telegram Desktop (tdesktop, C++/Qt 6, lib_ui)** на дизайн-систему
> **Material 3 Expressive (M3E)**. Документ — парный к Android-редизайну Marblegram (`org.telegram.marble.*`)
> и опирается на «M3 Expressive — все компоненты (Compose Material 3 1.5.0-beta01)».
>
> Версия документа: 1.0 · 2026-10-17 · Целевая ветка tdesktop: `dev` (6.x)

---

## Оглавление

0. [Цели, принципы, не-цели](#0-цели-принципы-не-цели)
1. [Архитектура tdesktop: что и где менять](#1-архитектура-tdesktop-что-и-где-менять)
2. [Сборка и инфраструктура форка](#2-сборка-и-инфраструктура-форка)
3. [Модуль `marble/` — ядро дизайн-системы](#3-модуль-marble--ядро-дизайн-системы)
4. [Цвет: M3 Color System и Material You на десктопе](#4-цвет-m3-color-system-и-material-you-на-десктопе)
5. [Типографика](#5-типографика)
6. [Формы (Shape) и MaterialShapes](#6-формы-shape-и-materialshapes)
7. [Motion: пружины и токены анимаций](#7-motion-пружины-и-токены-анимаций)
8. [Elevation, тональные поверхности, тени, блюр](#8-elevation-тональные-поверхности-тени-блюр)
9. [Иконки](#9-иконки)
10. [Состояния и ripple](#10-состояния-и-ripple)
11. [Компоненты — полный каталог](#11-компоненты--полный-каталог)
12. [Экраны — постраничный редизайн](#12-экраны--постраничный-редизайн)
13. [Окно, заголовок, системная интеграция](#13-окно-заголовок-системная-интеграция)
14. [Логотип и иконки приложения](#14-логотип-и-иконки-приложения)
15. [Настройки Marblegram](#15-настройки-marblegram)
16. [Функции Marblegram (паритет с Android)](#16-функции-marblegram-паритет-с-android)
17. [Плагины (Python/Lua) на десктопе](#17-плагины-pythonlua-на-десктопе)
18. [Доступность](#18-доступность)
19. [Производительность](#19-производительность)
20. [Тестирование и QA](#20-тестирование-и-qa)
21. [Дорожная карта и чек-листы](#21-дорожная-карта-и-чек-листы)
22. [Приложения: таблицы токенов, маппинг, глоссарий](#22-приложения)

---

## 0. Цели, принципы, не-цели

### 0.1 Цели

| # | Цель | Критерий готовности |
|---|------|---------------------|
| G1 | Весь UI tdesktop использует токены M3E (цвет, шрифт, форма, движение) | 0 «сырых» цветов/радиусов вне `marble/` и `.style` |
| G2 | Динамический цвет (Material You) из обоев / акцента ОС / seed-цвета | Палитра пересчитывается < 16 мс, без перезапуска |
| G3 | Выразительные индикаторы: `LoadingIndicator`, волнистые `LinearWavyProgress`, `CircularWavyProgress` | Все спиннеры/прогрессы заменены |
| G4 | Выразительные формы: 35 `MaterialShapes` + морфинг | Аватар-формы, FAB, индикатор загрузки, кнопки |
| G5 | Пружинная анимация (spring physics) вместо easing-кривых | `Ui::Animations::Simple` → `marble::Spring` в интерактивных элементах |
| G6 | Паритет функций с Marblegram Android | Призрачный режим, история правок/удалений, перевод, ИИ, плагины |
| G7 | Сохранить скорость tdesktop | Холодный старт ±5%, 60/120 fps скролл, RAM +≤30 МБ |

### 0.2 Принципы M3E применительно к десктопу

1. **Expressive, но не шумно.** Выразительность — в формах, пружинах и контрасте размеров, а не в лишних декорациях.
2. **Тон вместо тени.** Иерархия поверхностей строится через `surfaceContainer*`, тени — только для «плавающих» слоёв (меню, диалоги, FAB).
3. **Форма несёт смысл.** Выбранное = более круглое (пилюля), нажатое = «сжатое» (меньший радиус), формы морфятся.
4. **Плотность десктопа.** Используем M3 density `-1`/`-2` для списков и панелей; touch-target 40 dp → на десктопе 32–36 px с hover-состоянием.
5. **Клавиатура — первый класс.** Каждое состояние `focused` имеет видимое кольцо фокуса (3 px, `secondary`).
6. **Один источник правды.** Все токены — в `marble/tokens/*.style` + `MarbleTheme` (C++), генерация `colors.palette` автоматическая.

### 0.3 Не-цели

- Переписывать UI на QML/Qt Quick — **нет**, остаёмся на QWidget + собственной отрисовке lib_ui.
- Менять протокол, MTProto, хранение — **нет** (кроме локальной БД Marblegram для истории).
- Обходить платные функции (Premium и т. п.) — **нет**, вне рамок проекта.
- Поддерживать Windows 7/8 и macOS < 11 в новой теме — не гарантируется (fallback на стандартную).

---

## 1. Архитектура tdesktop: что и где менять

### 1.1 Структура репозитория

```
tdesktop/
├─ Telegram/
│  ├─ SourceFiles/              ← приложение
│  │  ├─ boxes/                 ← модальные «боксы» (диалоги)
│  │  ├─ calls/                 ← звонки, групповые звонки
│  │  ├─ chat_helpers/          ← поле ввода, эмодзи/стикер-панели, tabbed selector
│  │  ├─ dialogs/               ← список чатов, поиск, ячейки диалогов
│  │  ├─ history/               ← лента сообщений, view/*, media/*
│  │  ├─ info/                  ← профили, shared media
│  │  ├─ intro/                 ← экран входа
│  │  ├─ media/                 ← плееры, viewer (просмотрщик медиа)
│  │  ├─ settings/              ← настройки
│  │  ├─ window/                ← главное окно, тема, sidebar фильтров
│  │  ├─ ui/                    ← app-level UI (chat/*, controls/*, effects/*)
│  │  └─ *.style                ← стили модулей
│  ├─ lib_ui/                   ← (submodule) базовые виджеты, анимации, стили, палитра
│  │  └─ ui/
│  │     ├─ colors.palette      ← ВСЕ цветовые ключи
│  │     ├─ basic.style         ← шрифты, базовые размеры
│  │     ├─ widgets/            ← buttons, checkbox, input_fields, popup_menu, scroll_area...
│  │     ├─ effects/            ← ripple, radial_animation, animations
│  │     ├─ layers/             ← box_content, layer_widget, generic_box
│  │     ├─ paint/              ← arcs, blob, images
│  │     └─ style/              ← style_core, codegen-рантайм
│  ├─ codegen/ (lib)            ← codegen_style, codegen_emoji (генерация из .style)
│  ├─ Resources/                ← иконки (png @1x/@2x/@3x, svg), шрифты, langs, art/
│  └─ CMakeLists.txt
└─ cmake/
```

### 1.2 Как устроены стили

- Файлы `*.style` компилируются `codegen_style` в C++ (`styles/style_*.h/.cpp`), доступ — `st::имя`.
- Типы: `color`, `font`, `icon`, `margins`, `size`, `point`, `align`, `int`, `double`, а также `struct` (например, `FlatButton`, `RoundButton`, `IconButton`, `InputField`, `Menu`, `PopupMenu`, `Checkbox`, `Toggle`, `MediaSlider`, `SettingsSlider`, `InfiniteRadialAnimation`, `Box`).
- Цвета объявлены в `lib_ui/ui/colors.palette` (около 500 ключей: `windowBg`, `windowFg`, `activeButtonBg`, `msgInBg`, `msgOutBg`, `dialogsBgActive`, ...). Темы `.tdesktop-theme` переопределяют их.
- Палитра — `style::palette`, меняется в рантайме через `Window::Theme::Apply`/`style::main_palette::apply`, после чего рассылается `style::PaletteChanged()` (rpl-событие) → виджеты перерисовываются.

**Вывод:** M3-цвета внедряем как *генератор палитры* (seed → ~500 ключей), а не правкой каждого виджета. Формы, пружины, индикаторы — правкой виджетов lib_ui и `.style`.

### 1.3 Точки внедрения (hooks)

| Подсистема | Файл | Что делаем |
|---|---|---|
| Палитра | `window/themes/window_theme.cpp` (`Apply`, `ApplyDefault`, `KeepApplied`) | После загрузки темы — `marble::Theme::Patch(palette)` при включённом M3E |
| Палитра (низ) | `lib_ui/ui/style/style_core_palette.cpp` | Утилиты массового `set(key, color)` |
| Ripple | `lib_ui/ui/effects/ripple_animation.cpp` | Новый `marble::StateLayer` + ripple M3 (sparkle-free, 15%) |
| Спиннер | `lib_ui/ui/effects/radial_animation.cpp` (`InfiniteRadialAnimation`) | Подмена отрисовки на `marble::LoadingIndicator`/`CircularWavyProgress` |
| Прогресс | `history/view/media/history_view_document.cpp`, `media/view/media_view_*` | `marble::LinearWavyProgress` |
| Кнопки | `lib_ui/ui/widgets/buttons.cpp` | `RoundButton` → M3 Button (filled/tonal/outlined/text/elevated), морфинг формы |
| Переключатели | `lib_ui/ui/widgets/checkbox.cpp` (`ToggleView`, `CheckView`, `RadioView`) | M3 Switch/Checkbox/Radio |
| Слайдеры | `lib_ui/ui/widgets/continuous_sliders.cpp`, `discrete_sliders.cpp` | M3E Slider (handle-стержень, gap, stop indicators) |
| Меню | `lib_ui/ui/widgets/menu/*`, `popup_menu.cpp` | M3E Menu (vibrant, группы, скругление 16) |
| Боксы | `lib_ui/ui/layers/box_layer_widget.cpp`, `generic_box.cpp` | M3 Dialog (28 px радиус, `surfaceContainerHigh`) |
| Поля | `lib_ui/ui/widgets/fields/input_field.cpp` | Filled/Outlined TextField |
| Тосты | `lib_ui/ui/toast/toast_widget.cpp` | M3 Snackbar |
| Тултипы | `lib_ui/ui/widgets/tooltip.cpp` | Plain/Rich Tooltip |
| Список чатов | `dialogs/dialogs_inner_widget.cpp`, `dialogs/ui/dialogs_layout.cpp` | M3 List items, «expressive» выбор |
| Папки | `window/window_filters_menu.cpp`, `dialogs/ui/chats_filter_tag.cpp`, горизонтальные табы | M3E ButtonGroup / Vertical NavigationRail |
| Сообщения | `history/view/history_view_message.cpp`, `ui/chat/message_bubble.cpp` | Bubble-формы M3E |
| Поле ввода | `history/view/controls/history_view_compose_controls.cpp` | Floating toolbar + M3 TextField |
| Окно | `window/window_title_qt.cpp`, `ui/platform/*/ui_window_title_*` | M3 Top App Bar в заголовке |
| Ghost | `apiwrap.cpp`, `api/api_*`, `data/data_histories.cpp` | Фильтр исходящих read/typing/online |

---

## 2. Сборка и инфраструктура форка

### 2.1 Форк и сабмодули

```bash
git clone --recursive https://github.com/telegramdesktop/tdesktop.git marblegram-desktop
cd marblegram-desktop
git remote rename origin upstream
git checkout -b marble/main upstream/dev
# lib_ui тоже форкаем — основная часть изменений там
cd Telegram/lib_ui && git checkout -b marble/main
```

Правило: **все изменения lib_ui** — отдельными коммитами с префиксом `[marble/ui]`, изменения приложения — `[marble/app]`. Это упрощает ребейз на upstream (раз в релиз).

### 2.2 Минимизация конфликтов

- Новый код — в новых файлах: `Telegram/SourceFiles/marble/**` и `Telegram/lib_ui/ui/marble/**`.
- В апстрим-файлах — только «точки вызова» в 1–3 строки, обрамлённые маркерами:

```cpp
// MARBLE-BEGIN: m3e ripple
if (marble::Enabled()) { marble::PaintStateLayer(p, ...); return; }
// MARBLE-END
```

- Скрипт `tools/marble/check_markers.py` проверяет, что все изменения апстрим-файлов внутри маркеров.

### 2.3 Сборка

**Linux (рекомендуется контейнер):**
```bash
cd Telegram/build/docker/centos_env
poetry run gen_dockerfile | docker build -t tdesktop:centos_env -
docker run --rm -it -v $PWD/../../../..:/usr/src/tdesktop tdesktop:centos_env \
  /usr/src/tdesktop/Telegram/build/docker/centos_env/build.sh \
  -D TDESKTOP_API_ID=XXXX -D TDESKTOP_API_HASH=YYYY -D DESKTOP_APP_MARBLE=ON
```
> На musl-хосте (Alpine) собирать только в контейнере glibc — нативно Qt/WebRTC-зависимости не собираются.

**Windows:** VS 2022, `Telegram\build\prepare\win.bat`, затем `Telegram\configure.bat x64 -D TDESKTOP_API_ID=... -D TDESKTOP_API_HASH=... -D DESKTOP_APP_MARBLE=ON`.

**macOS:** Xcode 15+, `./Telegram/build/prepare/mac.sh`, `./configure.sh -D ...`.

### 2.4 CMake

```cmake
# Telegram/CMakeLists.txt
option(DESKTOP_APP_MARBLE "Build Marblegram M3E edition" ON)
if (DESKTOP_APP_MARBLE)
    target_compile_definitions(Telegram PRIVATE DESKTOP_APP_MARBLE)
    include(cmake/marble_sources.cmake)   # nice_target_sources(Telegram ... marble/*)
    target_link_libraries(Telegram PRIVATE desktop-app::lib_marble_plugins)
endif()
```

Стили Marblegram регистрируются в `generate_styles(...)` (`Telegram/cmake/td_ui.cmake`): `marble/marble.style`, `marble/tokens/*.style`.

### 2.5 Идентичность сборки

| Параметр | Значение |
|---|---|
| Имя | Marblegram Desktop |
| App ID (Linux) | `org.marblegram.desktop` |
| Каталог данных | `~/.local/share/MarblegramDesktop` (Win: `%APPDATA%\Marblegram Desktop`) |
| Автообновление | Отключено (`TDESKTOP_DISABLE_AUTOUPDATE`) или свой сервер + подпись |
| Схема ссылок | `tg://` (обработчик), `marble://` для внутренних |
| API ID | **Собственный** с my.telegram.org — не использовать официальный |

---

## 3. Модуль `marble/` — ядро дизайн-системы

### 3.1 Раскладка

```
Telegram/lib_ui/ui/marble/
├─ marble.style                 ← структуры компонентов
├─ tokens/
│  ├─ color.style               ← ссылки-алиасы на palette (m3Primary → activeButtonBg ...)
│  ├─ type.style                ← 15 ролей шрифта (+ emphasized)
│  ├─ shape.style               ← corner-токены
│  ├─ motion.style              ← spring-токены
│  └─ elevation.style
├─ marble_theme.h/.cpp          ← генерация схемы, патч палитры
├─ hct/                         ← HCT, TonalPalette, DynamicScheme (порт material-color-utilities)
│  ├─ cam16.cpp hct.cpp tonal_palette.cpp scheme_*.cpp quantizer_celebi.cpp score.cpp
├─ marble_shapes.h/.cpp         ← RoundedPolygon, Morph, 35 MaterialShapes
├─ marble_spring.h/.cpp         ← SpringAnimation (damping/stiffness)
├─ marble_state_layer.h/.cpp    ← hover/focus/press/drag слои
├─ marble_ripple.h/.cpp
├─ marble_loading_indicator.*   ← LoadingIndicator / ContainedLoadingIndicator
├─ marble_wavy_progress.*       ← LinearWavyProgress / CircularWavyProgress (determinate + indeterminate)
├─ marble_button.*              ← Button, IconButton, ToggleButton, SplitButton
├─ marble_button_group.*        ← ButtonGroup (standard / connected)
├─ marble_fab.*                 ← FAB, ExtendedFAB, FabMenu
├─ marble_toolbar.*             ← FloatingToolbar (horizontal/vertical, vibrant)
├─ marble_slider.*
├─ marble_switch.* marble_checkbox.* marble_radio.*
├─ marble_chip.*
├─ marble_menu.*
├─ marble_navigation_rail.*     ← Wide/Collapsed NavigationRail
├─ marble_top_app_bar.*         ← Small / Medium / Large / Flexible
├─ marble_search_bar.*
├─ marble_snackbar.* marble_tooltip.* marble_badge.*
├─ marble_dialog.* marble_side_sheet.* marble_bottom_sheet.*
├─ marble_text_field.*
├─ marble_carousel.*
└─ marble_surface.*             ← скруглённые поверхности с кэшем
```

### 3.2 Глобальный переключатель

```cpp
namespace marble {
[[nodiscard]] bool Enabled();             // Core::App().settings().marble().enabled
[[nodiscard]] rpl::producer<bool> EnabledValue();
[[nodiscard]] const Scheme &CurrentScheme();
[[nodiscard]] rpl::producer<> SchemeChanged();
} // namespace marble
```

Все компоненты проверяют `Enabled()` на этапе отрисовки, чтобы переключение было мгновенным (без пересоздания виджетов). Структурные изменения (высоты строк и т. п.) применяются после `Ui::ForceFullRepaint` + переразметки.

### 3.3 Соглашения кода

- Стиль кода — как в tdesktop: табы, `[[nodiscard]]`, `not_null<>`, `rpl::lifetime`, `base::unique_qptr`.
- Никаких `QPropertyAnimation` — только `Ui::Animations::Simple/Basic` и `marble::Spring` (поверх `Ui::Animations::Basic`).
- Отрисовка — `QPainter` + `PainterHighQualityEnabler`. Тяжёлые формы — кэш `QImage` по ключу (размер, цвет, DPR, progress-бакет).
- Все размеры в `.style` в «логических» пикселях; масштаб интерфейса (`style::Scale()` 100–300%) применяется codegen-ом автоматически.

---

## 4. Цвет: M3 Color System и Material You на десктопе

### 4.1 Порт material-color-utilities

Портируем C++-версию `material-color-utilities` (официально существует `cpp/`) в `lib_ui/ui/marble/hct`:
- `Hct`, `Cam16`, `ViewingConditions`
- `TonalPalette`, `CorePalette`
- `DynamicScheme` + варианты: `TonalSpot` (по умолчанию), `Vibrant`, `Expressive`, `Content`, `Fidelity`, `Monochrome`, `Neutral`, `Rainbow`, `FruitSalad`
- `DynamicColor` с contrast level (−1…1)
- `QuantizerCelebi` + `Score` — извлечение seed из изображения

Лицензия: Apache-2.0 — совместима с GPLv3 tdesktop (с указанием NOTICE).

### 4.2 Источники seed-цвета

| Источник | Платформа | Реализация |
|---|---|---|
| Акцент ОС | Windows 10/11 | `UISettings::GetColorValue(UIColorType::Accent)` (WinRT) или реестр `HKCU\Software\Microsoft\Windows\DWM\AccentColor` |
| Акцент ОС | macOS | `NSColor.controlAccentColor` (через `objc`), отслеживание `NSSystemColorsDidChangeNotification` |
| Акцент ОС | Linux/GNOME 47+ | `org.freedesktop.appearance accent-color` через xdg-desktop-portal Settings |
| Акцент ОС | KDE | `kdeglobals [General] AccentColor` + portal |
| Обои ОС | Win/macOS/GNOME | Путь к обоям → `QuantizerCelebi` (128 цветов) → `Score` |
| Обои чата | все | Текущий `Data::WallPaper` → квантование (в фоне, `crl::async`) |
| Ручной | все | Палитра из 16 seed + HCT-пикер |

Подписка: изменения акцента → debounce 300 мс → `marble::Theme::Rebuild()`.

### 4.3 Роли M3 (полный список)

```
primary onPrimary primaryContainer onPrimaryContainer primaryFixed primaryFixedDim onPrimaryFixed onPrimaryFixedVariant inversePrimary
secondary onSecondary secondaryContainer onSecondaryContainer secondaryFixed secondaryFixedDim onSecondaryFixed onSecondaryFixedVariant
tertiary onTertiary tertiaryContainer onTertiaryContainer tertiaryFixed tertiaryFixedDim onTertiaryFixed onTertiaryFixedVariant
error onError errorContainer onErrorContainer
surface onSurface surfaceVariant onSurfaceVariant surfaceTint
surfaceDim surfaceBright surfaceContainerLowest surfaceContainerLow surfaceContainer surfaceContainerHigh surfaceContainerHighest
inverseSurface inverseOnSurface outline outlineVariant scrim shadow
```

### 4.4 Маппинг M3-ролей → ключи `colors.palette`

Генератор `marble/hct/palette_mapper.cpp` содержит таблицу. Ниже — основа (полная таблица в §22.2).

| Ключ tdesktop | M3-роль (Light) | M3-роль (Dark) | Примечание |
|---|---|---|---|
| `windowBg` | `surface` | `surface` | |
| `windowFg` | `onSurface` | `onSurface` | |
| `windowBgOver` | `onSurface @ 8%` поверх `surface` | то же | state layer hover |
| `windowBgRipple` | `onSurface @ 12%` | то же | press |
| `windowSubTextFg` | `onSurfaceVariant` | `onSurfaceVariant` | |
| `windowActiveTextFg` | `primary` | `primary` | ссылки, активный текст |
| `windowBgActive` | `primary` | `primary` | |
| `windowFgActive` | `onPrimary` | `onPrimary` | |
| `activeButtonBg` | `primary` | `primary` | filled button |
| `activeButtonFg` | `onPrimary` | `onPrimary` | |
| `lightButtonBg` | `transparent` | `transparent` | text button |
| `lightButtonFg` | `primary` | `primary` | |
| `lightButtonBgOver` | `primary @ 8%` | то же | |
| `shadowFg` | `shadow @ 16%` | `shadow @ 40%` | |
| `boxBg` | `surfaceContainerHigh` | `surfaceContainerHigh` | диалоги |
| `boxTitleFg` | `onSurface` | `onSurface` | |
| `menuBg` | `surfaceContainer` | `surfaceContainer` | меню |
| `menuBgOver` | `onSurface @ 8%` | | |
| `menuIconFg` | `onSurfaceVariant` | | |
| `menuFgDisabled` | `onSurface @ 38%` | | |
| `dialogsBg` | `surfaceContainerLow` | `surface` | список чатов |
| `dialogsBgOver` | `onSurface @ 6%` | | |
| `dialogsBgActive` | `secondaryContainer` | `secondaryContainer` | выбранный чат |
| `dialogsNameFgActive` | `onSecondaryContainer` | | |
| `dialogsTextFgActive` | `onSecondaryContainer @ 80%` | | |
| `dialogsUnreadBg` | `primary` | `primary` | бейдж |
| `dialogsUnreadBgMuted` | `outline` | `outline` | |
| `dialogsUnreadFg` | `onPrimary` | | |
| `dialogsVerifiedIconBg` | `primary` | | |
| `dialogsOnlineBadgeFg` | `tertiary` | `tertiary` | |
| `historyComposeAreaBg` | `surfaceContainer` | `surfaceContainer` | панель ввода |
| `historyComposeIconFg` | `onSurfaceVariant` | | |
| `historySendIconFg` | `primary` | | |
| `msgInBg` | `surfaceContainerHighest` | `surfaceContainerHigh` | входящие |
| `msgInBgSelected` | `secondaryContainer` | | |
| `msgOutBg` | `primaryContainer` | `primaryContainer` | исходящие |
| `msgOutBgSelected` | `primaryContainer ⊕ primary@16%` | | |
| `historyTextInFg` | `onSurface` | | |
| `historyTextOutFg` | `onPrimaryContainer` | | |
| `msgInServiceFg` | `primary` | | |
| `msgOutServiceFg` | `onPrimaryContainer` (tone −10) | | |
| `msgInDateFg` | `onSurfaceVariant` | | |
| `msgOutDateFg` | `onPrimaryContainer @ 70%` | | |
| `historyOutIconFg` | `primary` | | галочки |
| `msgServiceBg` | `surfaceContainerHighest @ 72%` | `surfaceContainer @ 72%` | плашки дат |
| `msgServiceFg` | `onSurface` | | |
| `historyPeer1NameFg`…`8` | тона tertiary/secondary/hue-сдвиги | | см. §4.7 |
| `sideBarBg` | `surfaceContainer` | `surfaceContainerLowest` | Rail папок |
| `sideBarBgActive` | `secondaryContainer` | | индикатор |
| `sideBarIconFg` | `onSurfaceVariant` | | |
| `sideBarIconFgActive` | `onSecondaryContainer` | | |
| `titleBg` | `surfaceContainer` | `surfaceContainer` | заголовок окна |
| `titleFg` | `onSurface` | | |
| `titleButtonCloseBgOver` | `error` | `error` | |
| `scrollBarBg` | `onSurface @ 32%` | | |
| `scrollBarBgOver` | `onSurface @ 48%` | | |
| `inputFieldBg`/`filterInputInactiveBg` | `surfaceContainerHighest` | | search bar |
| `activeLineFg` | `primary` | | |
| `activeLineFgError` | `error` | | |
| `checkboxFg` | `onSurfaceVariant` | | |
| `toastBg` | `inverseSurface` | `inverseSurface` | snackbar |
| `toastFg` | `inverseOnSurface` | | |
| `tooltipBg` | `inverseSurface` | | plain tooltip |
| `mediaviewBg` | `scrim @ 92%` | | |
| `attentionButtonFg` | `error` | | |
| `introBg` | `surface` | | |
| `callBg` | `surfaceContainerLowest`(dark scheme всегда) | | |

### 4.5 Пайплайн применения

```
seed (QColor) ──► Hct ──► DynamicScheme(variant, isDark, contrast)
                              │
                              ▼
                   marble::Scheme (≈50 ролей, QColor)
                              │ PaletteMapper (таблица ~500 строк)
                              ▼
            style::palette::set(...) для каждого ключа
                              │
                              ▼
          style::NotifyPaletteChanged() ──► перерисовка
```

```cpp
void marble::Theme::Apply(const Options &o) {
	const auto scheme = BuildScheme(o.seed, o.variant, o.dark, o.contrast);
	auto palette = style::palette();
	palette.load(style::main_palette::get());          // база
	MapToPalette(scheme, palette, o);                  // перезапись ключей
	style::main_palette::apply(palette);
	Window::Theme::Background()->notify(BackgroundUpdate(BackgroundUpdate::Type::ApplyingTheme, false));
	_schemeChanged.fire({});
}
```

**Совместимость с `.tdesktop-theme`:** если пользователь применил стороннюю тему — M3E-генерация палитры по умолчанию выключается (но формы/пружины/индикаторы остаются). Опция «Перекрасить тему в Material You» принудительно накладывает маппинг.

### 4.6 Тёмная тема и «ночной режим»

- `Light`, `Dark`, `Как в системе` (через `QStyleHints::colorScheme()` Qt 6.5+ / portal).
- `AMOLED`: `surface = #000`, `surfaceContainer* = tone 4/6/8/10/12` нейтральной палитры.
- Контраст: `Стандартный (0)`, `Средний (0.5)`, `Высокий (1.0)` — также следить за `HighContrast` ОС.

### 4.7 Цвета имён и аватаров (peer colors)

Telegram задаёт 7 базовых цветов + серверные `PeerColors`. В M3E:
- Базовые цвета гармонизируются с primary: `Blend.harmonize(peerColor, primary)` (сдвиг hue ≤ 15°).
- Для имени в пузыре: tone 40 (light) / 80 (dark) палитры этого hue.
- Для фона аватара: `primaryContainer`-подобный tone 90/30, буквы — tone 10/90.
- Градиенты аватаров заменить на однотонные контейнеры + форма (см. §6.4), опция «Градиенты Telegram».

### 4.8 Обои чата

- Стандартная обоина → **M3 тональный паттерн**: фон `surfaceContainerLowest`, паттерн (Telegram pattern SVG) цветом `primary @ 6–10%`.
- Градиентные обои (4-точечные) — точки берутся из `primaryContainer`, `tertiaryContainer`, `secondaryContainer`, `surfaceContainerHigh`.
- Пользовательская картинка → обратный путь: становится seed (если включено «Цвет из обоев»).

---

## 5. Типографика

### 5.1 Шрифт

- По умолчанию tdesktop использует встроенный **Open Sans** (или системный). Для M3E:
  - **Google Sans Flex** / **Roboto Flex** (вариативный, оси `wght`, `wdth`, `opsz`, `GRAD`, `ROND`) — встраиваем в `Resources/fonts/`.
  - Fallback: `Roboto` → системный.
  - Опция «Системный шрифт» сохраняется.
- Вариативные оси используем через `QFont::setVariableAxis` (Qt 6.7+). Для Qt < 6.7 — статические начертания 400/500/600/700.
- Emphasized-варианты M3E: `wght +100..+200`, для display — `ROND 100` (скруглённые формы глифов).

### 5.2 Шкала M3 (desktop, px при 100%)

| Роль | Размер / Межстрочный | Вес | Emphasized | Где применяем |
|---|---|---|---|---|
| displayLarge | 57/64 | 400 | 500 | экран входа, пустые состояния |
| displayMedium | 45/52 | 400 | 500 | — |
| displaySmall | 36/44 | 400 | 500 | счётчики, Stars/статистика |
| headlineLarge | 32/40 | 400 | 500 | Large Top App Bar |
| headlineMedium | 28/36 | 400 | 500 | заголовки профилей |
| headlineSmall | 24/32 | 400 | 500 | заголовки диалогов (boxTitle) |
| titleLarge | 22/28 | 400 | 500 | шапка чата, Medium App Bar |
| titleMedium | 16/24 | 500 | 600 | имя в списке чатов, пункты настроек |
| titleSmall | 14/20 | 500 | 600 | подзаголовки секций |
| bodyLarge | 16/24 | 400 | 500 | текст сообщений (по умолч. 15/22 для плотности) |
| bodyMedium | 14/20 | 400 | 500 | превью в списке, описания |
| bodySmall | 12/16 | 400 | 500 | подписи, время |
| labelLarge | 14/20 | 500 | 600 | кнопки, табы |
| labelMedium | 12/16 | 500 | 600 | бейджи, чипы |
| labelSmall | 11/16 | 500 | 600 | счётчики, время в пузыре |

### 5.3 Реализация в `.style`

```
// lib_ui/ui/marble/tokens/type.style
m3FontFamily: "Google Sans Flex";
m3DisplayLarge: font(57px m3FontFamily);
m3HeadlineSmall: font(24px m3FontFamily);
m3TitleLarge: font(22px m3FontFamily);
m3TitleMedium: font(16px semibold m3FontFamily);
m3TitleSmall: font(14px semibold m3FontFamily);
m3BodyLarge: font(16px m3FontFamily);
m3BodyMedium: font(14px m3FontFamily);
m3BodySmall: font(12px m3FontFamily);
m3LabelLarge: font(14px semibold m3FontFamily);
m3LabelMedium: font(12px semibold m3FontFamily);
m3LabelSmall: font(11px semibold m3FontFamily);
```

И переназначение базовых: `normalFont → m3BodyMedium`, `semiboldFont → m3TitleSmall`, `boxTitleFont → m3HeadlineSmall`, `dialogsNameStyle → m3TitleMedium`, `historyMessageStyle.font → m3BodyLarge (15px)`.

> Важно: размер текста сообщений регулируется пользователем (`Settings → Chat → Text size`) — сохраняем масштабирование, M3-роль задаёт только базу.

### 5.4 Летеринг и выравнивание

- Letter-spacing M3 (`0.1–0.5 px`) применяем только к label-ролям (`QFont::setLetterSpacing(AbsoluteSpacing)`), в тексте сообщений — 0.
- Line-height: tdesktop хранит `lineHeight` в `TextStyle` — выставляем по таблице.
- Цифры в таймерах/счётчиках — `tnum` (tabular figures) через `QFont::setFeature` (Qt 6.6+).

---

## 6. Формы (Shape) и MaterialShapes

### 6.1 Corner-токены

| Токен | px | Применение |
|---|---|---|
| none | 0 | медиа на весь экран |
| extraSmall | 4 | тултипы, мелкие бейджи, scrollbar |
| small | 8 | чипы, snackbar, мелкие кнопки иконок (square) |
| medium | 12 | карточки, мини-медиа, меню-элементы |
| large | 16 | меню, FAB, навигационные элементы, поле ввода |
| largeIncreased | 20 | пузыри сообщений |
| extraLarge | 28 | диалоги, sheets, контейнеры |
| extraLargeIncreased | 32 | крупные карточки, медиа-плеер |
| extraExtraLarge | 48 | экран входа, промо |
| full | h/2 | кнопки, search bar, бейджи, switch |

### 6.2 RoundedPolygon / Morph

Портируем `androidx.graphics.shapes` (Kotlin → C++):
- `RoundedPolygon(numVertices, radius, center, rounding, perVertexRounding)`
- `CornerRounding(radius, smoothing)`
- `Morph(start, end)` → `asCubics(progress)` → `QPainterPath`
- `Feature`, `Cubic`, `MeasuredPolygon`, `FeatureMapper`

```cpp
namespace marble::shapes {
struct CornerRounding { float radius = 0.f; float smoothing = 0.f; };
class RoundedPolygon {
public:
	static RoundedPolygon Star(int points, float innerRadius, CornerRounding r);
	static RoundedPolygon Circle(int vertices = 8);
	static RoundedPolygon Rectangle(float w, float h, CornerRounding r);
	[[nodiscard]] QPainterPath toPath(QRectF bounds) const;
	[[nodiscard]] RoundedPolygon normalized() const;
};
class Morph {
public:
	Morph(const RoundedPolygon &a, const RoundedPolygon &b);
	[[nodiscard]] QPainterPath toPath(float progress, QRectF bounds) const;
};
} // namespace marble::shapes
```

### 6.3 35 MaterialShapes

`Circle, Square, Slanted, Arch, Fan, Arrow, SemiCircle, Oval, Pill, Triangle, Diamond, ClamShell, Pentagon, Gem, Sunny, VerySunny, Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided, Ghostish, Clover4Leaf, Clover8Leaf, Burst, SoftBurst, Boom, SoftBoom, Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart`.

Параметры берём 1:1 из Android-реализации Marblegram (`org.telegram.marble.MaterialShapes`), включая доработанный `Pentagon` (rounding 0.42) — общий JSON `shared/material_shapes.json` генерирует C++ и Java.

### 6.4 Где используются формы

| Место | Форма | Поведение |
|---|---|---|
| Аватары | настройка: Circle / Cookie9 / Cookie12 / Pentagon / Squircle / Clover4 / Flower / Puffy | При hover — морф в «Soft» версию (+0.15 rounding), 250 мс spring |
| Аватар «онлайн» | Circle + бейдж 10 px tertiary | |
| Свои истории (stories) | кольцо-«Sunny» вместо круга | непрочитанные — wavy-ring |
| LoadingIndicator | Soft Burst → Cookie9 → Pentagon → Pill → Sunny → Cookie4 → Oval | 7 форм, морф + вращение |
| FAB | Square(16) → нажатие Square(12) | |
| Кнопки | Pill → нажатие Square(8–12) | squish |
| Выбранный элемент Rail | Pill-индикатор 56×32 | ширина анимируется spring |
| Реакции «пузырёк» | Pill | |
| Иконки папок в Rail | Cookie4 подложка при выборе (опция) | |
| Пустые состояния | Большие формы (Burst/Flower) с иллюстрацией | |
| Голосовые / кружки | Circle (кружки) + волна вокруг при воспроизведении | |

### 6.5 Пузыри сообщений

- Радиус `largeIncreased` (20 px), «хвост» — опционально (по умолчанию **выкл**, M3E-стиль «без хвостов»).
- Группировка: смежные пузыри одного отправителя — внутренние углы `small` (6 px), внешние — 20 px.
- Медиа-в-пузыре: внутренние радиусы = внешний − отступ (concentric rule).
- Реализация: `ui/chat/message_bubble.cpp` (`PaintBubble`, `BubbleRounding`), расширить `BubbleCornerRounding` до числовых значений вместо `Large/Small/Tail`.

### 6.6 Кэш форм

`marble::ShapeCache` — LRU на 256 записей: ключ `{shapeId, morphPair, progressBucket(0..63), size, dpr, color}` → `QImage` с альфой. Для аватаров — маска `QImage::Format_Alpha8`, применяемая через `CompositionMode_DestinationIn`.

---

## 7. Motion: пружины и токены анимаций

### 7.1 Spring-токены M3E

| Токен | damping ratio | stiffness | Применение |
|---|---|---|---|
| fastSpatial | 0.9 | 1400 | маленькие элементы: switch thumb, checkbox, button squish |
| defaultSpatial | 0.9 | 700 | индикаторы, раскрытие меню, rail |
| slowSpatial | 0.9 | 300 | большие перемещения: панели, sheets |
| fastEffects | 1.0 | 3800 | цвет/альфа на маленьких |
| defaultEffects | 1.0 | 1600 | цвет/альфа |
| slowEffects | 1.0 | 800 | фейды крупных слоёв |
| expressive fastSpatial | 0.6 | 800 | «упругие» FAB, кнопки (Expressive scheme) |
| expressive defaultSpatial | 0.8 | 380 | ButtonGroup, морфинг |
| expressive slowSpatial | 0.8 | 200 | раскрытие FAB Menu, переходы экранов |

Схема движения выбирается в настройках: **Standard** / **Expressive** (по умолчанию Expressive).

### 7.2 Реализация

```cpp
namespace marble {
struct SpringSpec { float dampingRatio; float stiffness; float visibilityThreshold = 0.001f; };

class Spring final {
public:
	explicit Spring(Fn<void()> update);
	void animateTo(float target, SpringSpec spec, float initialVelocity = NAN); // NAN → текущая
	void jumpTo(float value);
	[[nodiscard]] float value() const;
	[[nodiscard]] float velocity() const;
	[[nodiscard]] bool animating() const;
private:
	void step(crl::time now);           // аналитическое решение (under/critically/over-damped)
	Ui::Animations::Basic _ticker;
	float _value = 0.f, _velocity = 0.f, _target = 0.f;
	crl::time _last = 0;
	SpringSpec _spec{};
	Fn<void()> _update;
};
} // namespace marble
```

- Шаг — точное аналитическое решение, а не Эйлер: стабильно при пропусках кадров.
- Прерываемость: новый `animateTo` сохраняет скорость → естественные «догоняния».
- `Ui::Animations` синхронизирован с vsync через `Ui::Animations::Manager` — используем его тикер.

### 7.3 Не-пружинные переходы

| Переход | Длительность | Кривая |
|---|---|---|
| Fade through (смена разделов) | 300 мс | emphasizedDecelerate (0.05,0.7,0.1,1) |
| Shared axis X (вглубь настроек) | 400 мс | emphasized |
| Container transform (чат → профиль) | 450 мс | emphasized |
| Появление тоста | 250 мс | emphasizedDecelerate |
| Исчезновение | 200 мс | emphasizedAccelerate (0.3,0,0.8,0.15) |

tdesktop использует `Window::SlideAnimation` и `Ui::SlideAnimation` (скриншот-слайд) — сохраняем механику, но меняем: сдвиг 30% + fade вместо 100%, кривая emphasized.

### 7.4 Reduce motion

Учитываем `Settings → Advanced → Performance → Animations` (`Ui::Animations::Disabled`) + системный «уменьшение движения» (Win `SPI_GETCLIENTAREAANIMATION`, macOS `accessibilityDisplayShouldReduceMotion`, GNOME `enable-animations`). При reduce — пружины становятся `jumpTo`, волны замирают в амплитуде 0 (обычная прямая полоса).

---

## 8. Elevation, тональные поверхности, тени, блюр

### 8.1 Уровни

| Уровень | dp | Поверхность | Тень | Где |
|---|---|---|---|---|
| 0 | 0 | surface | нет | фон окна, лента |
| 1 | 1 | surfaceContainerLow | 1px ambient | карточки, список чатов (light) |
| 2 | 3 | surfaceContainer | слабая | панели, rail, top bar при скролле |
| 3 | 6 | surfaceContainerHigh | средняя | FAB, меню, диалоги, search |
| 4 | 8 | surfaceContainerHigh | | hover на FAB |
| 5 | 12 | surfaceContainerHighest | сильная | drag-объекты |

Тени: `Ui::Shadow` / `Ui::BoxShadow` в tdesktop основаны на 9-patch картинках (`st::boxRoundShadow`). Генерируем новые 9-patch-и программно (`marble::GenerateShadow(radius, elevation, dpr)` — две гауссовы тени key+ambient по M3), кэшируем.

### 8.2 Блюр (опционально, «Glass»)

Android-версия Marblegram имеет блюр-подложки. На десктопе:
- Windows 11: `DwmSetWindowAttribute(DWMWA_SYSTEMBACKDROP_TYPE, DWMSBT_MAINWINDOW /*Mica*/ | DWMSBT_TABBEDWINDOW /*Mica Alt*/ | DWMSBT_TRANSIENTWINDOW /*Acrylic*/)` — для заголовка и боковой панели.
- macOS: `NSVisualEffectView` под окном (sidebar material).
- Внутри окна (поверх ленты): собственный блюр — downscale ×4 → stack blur (`Images::BlurLargeImage` уже есть в tdesktop) → upscale; обновление только при изменении содержимого под панелью.
- По умолчанию: **выкл** на слабых GPU (Software OpenGL / нет ANGLE).

---

## 9. Иконки

### 9.1 Набор

- Основа — **Material Symbols Rounded** (вариативный шрифт или SVG), оси: `FILL 0/1`, `wght 400`, `GRAD 0`, `opsz 20/24`.
- Выбранное состояние = `FILL 1` (+ морф заливки 150 мс).
- Иконки Telegram-специфичные (секретный чат, бот, Stars, топики) — перерисовать в стиле Symbols (2 px stroke при 24, скруглённые концы).

### 9.2 Пайплайн

tdesktop хранит иконки как PNG-маски (`Resources/icons/*.png`, `@2x`, `@3x`) и `icon {{ "name", colorKey }}` в `.style`, а в новых версиях — SVG.
1. `tools/marble/icons/map.json` — соответствие `tdesktopIconName → material_symbol_name (+fill)`.
2. Скрипт `render_icons.py` (cairosvg/resvg) рендерит Symbols в маски 1x/2x/3x с правильными паддингами (тот же canvas, что у исходника, чтобы не трогать `.style`).
3. Генерация попадает в `Resources/icons/marble/` и подключается через overlay qrc с тем же путём — исходные `.style` не меняются.
4. Иконки без соответствия — список `missing.txt` для ручной отрисовки.

### 9.3 Объём

Около **1100** иконок в tdesktop. Приоритеты:
- P0 (≈180): меню, панель ввода, шапка чата, список чатов, настройки (главная), медиа-просмотрщик.
- P1 (≈400): профили, боксы, звонки, плеер.
- P2: остальное.

### 9.4 Цветные иконки настроек

Как в Android Marblegram: пункты главной страницы настроек получают иконку в цветной форме (Cookie4 / Circle 36 px), цвет — гармонизированный тон (`primary`, `tertiary`, `secondary` + 6 hue-сдвигов). Включается опцией «Цветные иконки».

---

## 10. Состояния и ripple

### 10.1 State layers

| Состояние | Непрозрачность слоя (цвет = контентный цвет) |
|---|---|
| hover | 8% |
| focus | 10% + focus ring |
| pressed | 10% (+ ripple) |
| dragged | 16% |
| disabled container | onSurface 12% |
| disabled content | onSurface 38% |

### 10.2 Ripple

Текущий `Ui::RippleAnimation` — круг, растущий от точки нажатия, `ripple.color`. M3-версия:
- Радиус растёт до `hypot(w,h)` за 225 мс (`decelerate`), fade-out 150 мс после отпускания (минимум 150 мс видимости).
- Альфа 10%, без «sparkle» (на десктопе не нужен).
- Ограничение маской формы компонента (path), не прямоугольником.
- Hover-слой рисуется отдельно (`StateLayer`), ripple — сверху.

Поскольку почти все кнопки tdesktop используют `RippleButton::paintRipple`, достаточно изменить `RippleAnimation::paint` + цвета `*Ripple` в палитре, а форму маски передавать через новый `RippleButton::setRippleMask(Fn<QPainterPath(QSize)>)`.

### 10.3 Фокус

- Focus ring: 3 px `secondary`, отступ 2 px снаружи формы.
- Показывается только при навигации клавиатурой (`Tab`, стрелки) — флаг `marble::KeyboardNavigationActive()`, сбрасывается мышью.
- Добавить фокус-цепочку в список чатов, шапку, панель ввода (tdesktop частично это умеет).

---
