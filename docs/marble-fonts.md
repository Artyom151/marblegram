# Marblegram — шрифты

## Что использует Telegram (аудит `getTypeface`/`bold()` по исходникам)

| Файл в assets | Что это | Вызовов | В Marblegram |
|---|---|---|---|
| системный sans-serif | весь обычный текст | — | Roboto Flex wght 400, opsz 14 (`installGlobal`) |
| `rmedium.ttf` через `AndroidUtilities.bold()` | заголовки, имена, кнопки | 1190 | Roboto Flex wght 600 |
| `rmedium.ttf` напрямую | счётчики, подписи | 20 | Roboto Flex wght 600 |
| `rextrabold.ttf` | крупные цифры, промо | 4 | Roboto Flex wght 800 |
| `rcondensedbold.ttf` | истории/редактор | 12 | Roboto Flex wght 700, wdth 75 |
| `ritalic.ttf`, `rmediumitalic.ttf` | курсив в тексте | 19 | Roboto Flex slnt −10 (400/600) |
| `num.otf` | крупные числа (таймеры, статистика) | 26 | Roboto Flex wght 700, opsz 28 |
| `rmono.ttf`, `rmono_var.ttf` | код, моноширинный | 18 | **без изменений** (Roboto Mono) |
| `mw_bold.ttf`, `mw_bolditalic.ttf` | Merriweather — стили текста в редакторе | 19 | **без изменений** (авторский шрифт) |
| `gram.ttf` | глифы-иконки (TON/Stars) | 8 | **без изменений** |

## Шрифт M3 Expressive

Шкала M3 Expressive (Compose Material 3 1.5) построена на вариативном **Roboto Flex**
(оси `wght`, `wdth`, `opsz`, `slnt`, `GRAD`…, лицензия OFL). Google Sans Flex не используется.
Файл: `TMessagesProj/src/main/assets/fonts/marble_roboto_flex.ttf`, источник — `google/fonts/ofl/robotoflex`,
лицензия — `docs/licenses/RobotoFlex-OFL.txt`.

`MarbleTypography.typeface(style, emphasized)` выдаёт инстанс с `opsz` = размер стиля, emphasized — +100 к весу,
display/headline emphasized — `wdth 92`. Выключается переключателем «Expressive-типографика» в настройках Marblegram.
