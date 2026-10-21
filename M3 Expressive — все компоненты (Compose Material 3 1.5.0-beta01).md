# M3 Expressive — все компоненты (Compose Material 3 1.5.0-beta01)

Oct 16, 2026 · @Артём

## Версия и подключение

Версии 1.5.7 не существует. На 16 октября 2026 последняя стабильная — **1.4.0**, самая свежая — **1.5.0-beta01** (7 октября 2026). Весь M3 Expressive живёт в 1.5.x, поэтому берём 1.5.0-beta01. [Release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)

**build.gradle.kts (модуль app):**

```kotlin
plugins {
    kotlin("plugin.compose")
}

dependencies {
    implementation("androidx.compose.material3:material3:1.5.0-beta01")
    // адаптивная навигация (бар ↔ рейл):
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-beta01")
}

kotlin {
    compilerOptions {
        // чтобы не писать @OptIn в каждом файле
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}
```

- Если используешь Compose BOM, версию material3 указывай явно — BOM обычно тянет стабильную 1.4.0.
- `androidx.graphics:graphics-shapes` (морфинг форм) material3 подтягивает сам.
- Многое в 1.5 уже стабильно (кнопки, FAB-меню, тулбары, ButtonGroup, flexible app bars, списки, меню, волнистые прогрессы). Под `ExperimentalMaterial3ExpressiveApi` остались, например, `MaterialShapes` и `LoadingIndicator` — opt-in выше закрывает всё.

**Пакеты для импорта:**

| Что | Пакет |
| --- | --- |
| Почти все компоненты, тема, формы | `androidx.compose.material3.*` |
| Карусели | `androidx.compose.material3.carousel.*` |
| Pull-to-refresh | `androidx.compose.material3.pulltorefresh.*` |
| NavigationSuiteScaffold | `androidx.compose.material3.adaptive.navigationsuite.*` |
| Морфинг форм (`Morph`, `RoundedPolygon`) | `androidx.graphics.shapes.*` |

## Тема: цвета, моушен, типографика, формы

Всё приложение оборачивается в `MaterialExpressiveTheme` вместо `MaterialTheme`: она включает expressive-моушен (пружины) и expressive-палитру по умолчанию.

```kotlin
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
```

| API | Что даёт |
| --- | --- |
| `MaterialExpressiveTheme` | Тема с expressive-дефолтами |
| `expressiveLightColorScheme()` | Expressive-палитра (светлая) |
| `dynamicLightColorScheme` / `dynamicDarkColorScheme` | Material You из обоев, Android 12+ |
| `MotionScheme.expressive()` / `MotionScheme.standard()` | Пружинная анимация всех компонентов |
| `MaterialTheme.motionScheme.defaultSpatialSpec()` | Готовые спеки для своих анимаций (`fast…`, `slow…`, `…EffectsSpec`) |
| `Typography` → `displayLargeEmphasized` … `labelSmallEmphasized` | Акцентные (жирнее) версии всех 15 стилей |
| `Shapes` → `largeIncreased`, `extraLargeIncreased`, `extraExtraLarge` | Новые ступени скругления |
| `MaterialShapes` | 35 готовых «весёлых» форм |
| `toShape()` / `toPath()` | Превращают форму из `MaterialShapes` в `Shape` / `Path` |

**MaterialShapes (все 35):** Circle, Square, Slanted, Arch, Fan, Arrow, SemiCircle, Oval, Pill, Triangle, Diamond, ClamShell, Pentagon, Gem, Sunny, VerySunny, Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided, Ghostish, Clover4Leaf, Clover8Leaf, Burst, SoftBurst, Boom, SoftBoom, Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart.

```kotlin
// аватарка в форме «печеньки»
Image(
    painter = avatar,
    contentDescription = null,
    modifier = Modifier.size(56.dp).clip(MaterialShapes.Cookie9Sided.toShape()),
)
```

Для морфинга одной формы в другую (например, аватар при нажатии) используй `Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided)` из `androidx.graphics.shapes`. В 1.5.0-beta01 API `PolygonShape` / `CornerRounding` переехал и поменялся, так что сверяйся с samples в [API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary).

## Кнопки

В M3E у кнопок 5 размеров (XS–XL), морфинг формы при нажатии (круг → скруглённый квадрат), toggle-варианты и группы. Все импорты — `androidx.compose.material3.*`.

| Компонент | Что нового в M3E |
| --- | --- |
| `Button`, `ElevatedButton`, `FilledTonalButton`, `OutlinedButton`, `TextButton` | `shapes = ButtonDefaults.shapes()` — морфинг при нажатии; размеры через `ButtonDefaults.ExtraSmallContainerHeight` … `ExtraLargeContainerHeight` + `contentPaddingFor(height)` |
| `IconButton`, `FilledIconButton`, `FilledTonalIconButton`, `OutlinedIconButton` | `shapes = IconButtonDefaults.shapes()`; размеры `IconButtonDefaults.extraSmallContainerSize()` … `extraLargeContainerSize()`; ширина `IconButtonWidthOption.Narrow / Uniform / Wide` |
| `IconToggleButton`, `FilledIconToggleButton`, `FilledTonalIconToggleButton`, `OutlinedIconToggleButton` | Иконка-переключатель с морфингом формы во включённом состоянии |
| `ToggleButton`, `ElevatedToggleButton`, `FilledTonalToggleButton`, `OutlinedToggleButton` | Новые: обычная кнопка с состоянием `checked`; дефолты в `ToggleButtonDefaults` и т.п. |
| `ButtonGroup` | Группа кнопок, соседи «сжимаются» при нажатии (`Modifier.animateWidth`), есть overflow-меню |
| Connected button group | `Row` из `ToggleButton` с `ButtonGroupDefaults.connectedLeadingButtonShapes()`, `connectedMiddleButtonShapes()`, `connectedTrailingButtonShapes()` — замена SegmentedButton |
| `SplitButton` | Кнопка + стрелка меню: `SplitButtonDefaults.LeadingButton` и `TrailingButton`. Старый `SplitButtonLayout` deprecated |

```kotlin
// фильтры чатов: Все / Личные / Группы / Каналы
val tabs = listOf("Все", "Личные", "Группы", "Каналы")
var selected by remember { mutableIntStateOf(0) }

Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    tabs.forEachIndexed { i, label ->
        ToggleButton(
            checked = selected == i,
            onCheckedChange = { selected = i },
            modifier = Modifier.weight(1f),
            shapes = when (i) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            },
        ) { Text(label) }
    }
}
```

## FAB

FAB получил три размера, extended-варианты под каждый и раскрывающееся меню, где сам FAB морфится в кнопку «закрыть».

| Компонент | Назначение |
| --- | --- |
| `FloatingActionButton`, `MediumFloatingActionButton`, `LargeFloatingActionButton` | Обычный FAB трёх размеров (`SmallFloatingActionButton` в M3E не рекомендуется) |
| `SmallExtendedFloatingActionButton`, `ExtendedFloatingActionButton`, `MediumExtendedFloatingActionButton`, `LargeExtendedFloatingActionButton` | FAB с текстом, `expanded` сворачивает его в иконку при скролле |
| `FloatingActionButtonMenu` + `FloatingActionButtonMenuItem` | Меню действий над FAB |
| `ToggleFloatingActionButton` | FAB-переключатель для меню, морфит форму и иконку |
| `Modifier.animateFloatingActionButton(visible, alignment)` | Анимация появления и исчезновения FAB |

```kotlin
// «Новый чат / группа / канал» на экране чатов
var expanded by rememberSaveable { mutableStateOf(false) }

FloatingActionButtonMenu(
    expanded = expanded,
    button = {
        ToggleFloatingActionButton(
            checked = expanded,
            onCheckedChange = { expanded = it },
        ) {
            Icon(
                painter = painterResource(if (expanded) R.drawable.ic_close else R.drawable.ic_edit),
                contentDescription = null,
            )
        }
    },
) {
    FloatingActionButtonMenuItem(onClick = { /* новый чат */ }, text = { Text("Новый чат") }, icon = { Icon(painterResource(R.drawable.ic_person), null) })
    FloatingActionButtonMenuItem(onClick = { /* группа */ }, text = { Text("Новая группа") }, icon = { Icon(painterResource(R.drawable.ic_group), null) })
    FloatingActionButtonMenuItem(onClick = { /* канал */ }, text = { Text("Новый канал") }, icon = { Icon(painterResource(R.drawable.ic_campaign), null) })
}
```

Иконки бери из Material Symbols (векторные drawable): старая `material-icons-extended` давно не обновляется.

## Индикаторы загрузки и прогресса

Вместо крутящегося кружка M3E показывает морфящиеся формы (`LoadingIndicator`) и волнистые прогресс-бары.

| Компонент | Пакет | Когда использовать |
| --- | --- | --- |
| `LoadingIndicator` | `material3` | Короткая неопределённая загрузка (до \~5 с): «Соединение…», загрузка чата |
| `ContainedLoadingIndicator` | `material3` | То же, но на подложке-контейнере |
| `LinearWavyProgressIndicator` | `material3` | Волнистая полоса, например загрузка файла или медиа |
| `CircularWavyProgressIndicator` | `material3` | Волнистый круг, например кружок на отправляемом фото |
| `PullToRefreshBox` + `PullToRefreshDefaults.LoadingIndicator` | `material3.pulltorefresh` | Pull-to-refresh с expressive-индикатором |

```kotlin
LoadingIndicator()                                  // неопределённый
CircularWavyProgressIndicator(progress = { 0.6f })  // определённый
LinearWavyProgressIndicator(progress = { download })
```

`LoadingIndicator` в 1.5 снова помечен как experimental — нужен opt-in `ExperimentalMaterial3ExpressiveApi`.

## Тулбары и app bars

M3E добавляет плавающие тулбары и «гибкие» верхние бары с подзаголовком. Подзаголовок — ровно то, что нужно для «в сети / печатает…» в шапке чата.

| Компонент | Что это |
| --- | --- |
| `HorizontalFloatingToolbar`, `VerticalFloatingToolbar` | Плавающая «пилюля» с действиями, можно с FAB рядом (`floatingActionButton =`) |
| `FloatingToolbarDefaults.vibrantFloatingToolbarColors()` / `standardFloatingToolbarColors()` | Яркий или спокойный цвет тулбара |
| `FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)` | Прячет тулбар при скролле |
| `FlexibleBottomAppBar` | Нижний «docked» тулбар M3E (замена BottomAppBar с FAB) |
| `TopAppBar(title, subtitle = …, titleHorizontalAlignment = …)` | Маленький бар, теперь с подзаголовком |
| `MediumFlexibleTopAppBar`, `LargeFlexibleTopAppBar` | Большой заголовок, при скролле сворачивается в маленький |
| `TwoRowsTopAppBar` | Полностью настраиваемый двухрядный бар |
| `CenterAlignedTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar` | Классика, тоже доступна |
| `AppBarRow` / `AppBarColumn` + `AppBarOverflowIndicator` | Действия бара, лишние уходят в меню «⋮» автоматически |
| `TopAppBarDefaults.enterAlwaysScrollBehavior(scrollableState = listState)` | Поведение при скролле; в 1.5 принимает `ScrollableState` |

```kotlin
// шапка экрана чата
TopAppBar(
    title = { Text(chat.title) },
    subtitle = { Text(if (typing) "печатает…" else "в сети") },
    navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Назад") } },
    actions = {
        AppBarRow(maxItemCount = 2) {
            clickableItem(onClick = onCall, icon = { Icon(painterResource(R.drawable.ic_call), null) }, label = "Позвонить")
            clickableItem(onClick = onSearch, icon = { Icon(painterResource(R.drawable.ic_search), null) }, label = "Поиск")
            clickableItem(onClick = onMute, icon = { Icon(painterResource(R.drawable.ic_mute), null) }, label = "Без звука")
        }
    },
)
```

## Навигация

В M3E старые `NavigationBar` и `NavigationRail` заменены на более низкий `ShortNavigationBar` и `WideNavigationRail`, который раскрывается в меню.

| Компонент | Пакет | Что это |
| --- | --- | --- |
| `ShortNavigationBar` + `ShortNavigationBarItem` | `material3` | Нижняя панель M3E (64 dp), на широком экране иконка ставится слева от текста |
| `WideNavigationRail` + `WideNavigationRailItem` | `material3` | Боковой рейл, раскрывается по `rememberWideNavigationRailState()` |
| `ModalWideNavigationRail` | `material3` | Рейл поверх контента со скримом, замена navigation drawer |
| `NavigationSuiteScaffold` | `material3.adaptive.navigationsuite` | Сам выбирает бар или рейл по размеру окна |
| `NavigationBar`, `NavigationRail`, `ModalNavigationDrawer` | `material3` | Старые M3-версии, работают, но не expressive |

```kotlin
// низ приложения: Чаты / Контакты / Настройки
ShortNavigationBar {
    destinations.forEachIndexed { i, d ->
        ShortNavigationBarItem(
            selected = current == i,
            onClick = { current = i },
            icon = { Icon(painterResource(d.icon), null) },
            label = { Text(d.label) },
        )
    }
}
```

## Поиск

В 1.5 поиск построен на `SearchBarState`: свёрнутая строка (`SearchBar`) и отдельный composable для раскрытого состояния. Старые API с `expanded` / `onExpandedChange` deprecated.

| Компонент | Что это |
| --- | --- |
| `rememberSearchBarState()` | Состояние поиска: свёрнут или развёрнут |
| `SearchBar(state, inputField)` | Свёрнутая строка поиска |
| `SearchBarDefaults.InputField(...)` | Поле ввода внутри поиска |
| `AppBarWithSearch` | Верхний бар со строкой поиска внутри (бывший `TopSearchBar`) |
| `ExpandedFullScreenSearchBar` | Раскрытый поиск на весь экран (телефон) |
| `ExpandedFullScreenContainedSearchBar` | То же, но с контейнером — expressive-вид |
| `ExpandedDockedSearchBar` / `ExpandedDockedSearchBarWithGap` + `rememberSearchBarWithGapState()` | Раскрытый поиск выпадашкой (планшет); вариант с зазором — expressive |
| `rememberSearchBarScrollState()` | Прятать строку поиска при скролле |

```kotlin
val searchState = rememberSearchBarState()
val text = rememberTextFieldState()
val scope = rememberCoroutineScope()

val input = @Composable {
    SearchBarDefaults.InputField(
        textFieldState = text,
        searchBarState = searchState,
        onSearch = { scope.launch { searchState.animateToCollapsed() } },
        placeholder = { Text("Поиск") },
    )
}
AppBarWithSearch(state = searchState, inputField = input)
ExpandedFullScreenContainedSearchBar(state = searchState, inputField = input) {
    // результаты поиска чатов и сообщений
}
```

## Списки и меню

Сегментированные списки (скруглённые блоки, склеенные в группу) — главный визуальный приём M3E для экранов настроек. В 1.5 expressive-списки и меню уже стабильные.

| Компонент | Что это |
| --- | --- |
| `ListItem` (новые перегрузки с `onClick`, `selected`, `checked`) | Интерактивный элемент списка с морфингом формы при нажатии и выборе |
| `SegmentedListItem` | Элемент сегментированной группы: первый скруглён сверху, последний снизу |
| `ListItemDefaults.segmentedShapes(index, count)` | Формы под позицию элемента в группе |
| `DropdownMenu` / `DropdownMenuPopup` | Меню; `DropdownMenuPopup` — новая база для expressive-меню и подменю |
| `DropdownMenuGroup` + `MenuDefaults.DropdownMenuGroupLabel` | Группы пунктов внутри меню с подписью |
| `DropdownMenuItem` (с `shapes`) | Пункт меню с expressive-формой |
| `SelectableDropdownMenuItem`, `CheckableDropdownMenuItem` | Пункты с выбором (радио) или галочкой |
| `MenuDefaults.itemVibrantColors()` | Яркая цветовая схема меню |
| `rememberDropdownMenuPopupPositionProvider(...)` | Позиция меню относительно якоря, каскадные подменю |
| `ExposedDropdownMenuBox` + `ExposedDropdownMenu` | Выпадающий список у поля ввода; `ExposedDropdownMenu` в 1.5 стал extension-функцией, может понадобиться новый импорт |

Для Telegram: список чатов — `ListItem` с `onClick`; настройки — группы `SegmentedListItem`; долгое нажатие на сообщение — `DropdownMenuPopup` с группами («Ответить / Копировать» и «Удалить»).

Сигнатуры меню и списков сильно менялись между alpha-версиями, поэтому точные параметры сверяй с [API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary) и samples в [catalog](https://github.com/androidx/androidx/tree/androidx-main/compose/material3/material3/samples).

## Остальные компоненты

Все компоненты ниже лежат в `androidx.compose.material3.*`, если в колонке «Пакет» не указано другое.

| Группа | Компоненты | Что важно в 1.5 |
| --- | --- | --- |
| Карусели (`material3.carousel`) | `HorizontalMultiBrowseCarousel`, `HorizontalUncontainedCarousel`, `HorizontalCenteredHeroCarousel`, `rememberCarouselState`, `Modifier.maskClip(shape)`, `carouselParallaxScrollEffect` | У hero-карусели параметр `maxItemWidth` переименован в `preferredItemWidth`; есть `CarouselDefaults.ItemSpacing` и `ContentPadding` |
| Слайдеры | `Slider`, `VerticalSlider`, `RangeSlider`, `SliderState`, `RangeSliderState`, `SliderDefaults` | Рекомендуется stateful-вариант `Slider(state = …)`; в `RangeSliderState` теперь `startValue` / `endValue` |
| Чипы | `AssistChip`, `FilterChip`, `InputChip`, `SuggestionChip` + `Elevated…` версии | У `FilterChip`, `ElevatedFilterChip`, `InputChip` есть перегрузки с морфингом формы |
| Время и дата | `TimePicker`, `TimeInput`, `TimePickerDialog`, `VibrantTimePickerDialog`, `ScrollField`, `TimeScroll`, `DatePicker`, `DateRangePicker`, `DatePickerDialog` | Новый expressive `TimePicker` со scroll-режимом; `RichTimePickerDialog` переименован в `VibrantTimePickerDialog` |
| Текстовые поля | `TextField`, `OutlinedTextField`, `SecureTextField`, `OutlinedSecureTextField` | Expressive-вид: `TextFieldDefaults.roundedShape` + `tonalColors()`; подпись `TextFieldLabelPosition.Inside / Cutout / Above` |
| Листы | `ModalBottomSheet`, `BottomSheet` (статичный), `BottomSheetScaffold`, `rememberBottomSheetState`, `VerticalDragHandle` | `rememberModalBottomSheetState` deprecated — используй `rememberBottomSheetState` |
| Скрим | `Scrim` | Новый отдельный компонент затемнения под модальными окнами |
| Тултипы | `TooltipBox`, `PlainTooltip`, `RichTooltip` | Не обрезаются краем экрана |
| Без изменений (обычный M3) | `Switch`, `Checkbox`, `RadioButton`, `Badge`, `BadgedBox`, `Card`, `AlertDialog`, `BasicAlertDialog`, `Snackbar`, `Surface`, `HorizontalDivider`, табы (`PrimaryTabRow` и др.), `SwipeToDismissBox` | Подхватывают expressive-тему автоматически |

Для Telegram пригодятся `SwipeToDismissBox` для свайпа чата в архив, `Badge` для счётчика непрочитанных, `ModalBottomSheet` для вложений и `HorizontalMultiBrowseCarousel` для медиа в профиле.

## Карта экранов Telegram-клиента

| Экран | Компоненты M3E |
| --- | --- |
| Список чатов | `AppBarWithSearch`, connected `ToggleButton` (фильтры папок), `ListItem` + `Badge`, `SwipeToDismissBox`, `FloatingActionButtonMenu`, `ShortNavigationBar` |
| Чат | `TopAppBar` с `subtitle`, `AppBarRow`, `DropdownMenuPopup` (действия с сообщением), `CircularWavyProgressIndicator` (отправка медиа), `ModalBottomSheet` (вложения), `TextField` + `roundedShape` |
| Профиль | `LargeFlexibleTopAppBar`, аватар через `MaterialShapes.*.toShape()`, `HorizontalMultiBrowseCarousel` (медиа), `ButtonGroup` (Чат / Звонок / Без звука) |
| Настройки | `MediumFlexibleTopAppBar`, группы `SegmentedListItem`, `Switch`, `Slider` (размер текста) |
| Загрузка и подключение | `LoadingIndicator`, `ContainedLoadingIndicator` |
| Планшет | `NavigationSuiteScaffold` / `WideNavigationRail`, `ExpandedDockedSearchBarWithGap` |

## Источники

- [Compose Material 3 — release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) (версии, изменения 1.5.0-alpha01 … beta01)
- [androidx.compose.material3 — API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary) (полный список composable)
- [Samples каталога material3](https://github.com/androidx/androidx/tree/androidx-main/compose/material3/material3/samples)
