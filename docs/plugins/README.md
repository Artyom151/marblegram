# Marblegram Plugin SDK

Плагины Marblegram — это скрипты на **Lua 5.2** или **Python 3.11**, которые работают внутри клиента
и могут менять его поведение: переписывать и отменять отправляемые сообщения, реагировать на входящие,
показывать уведомления, добавлять свои кнопки, хранить настройки — и через мост к Java вызывать
**любой** класс Telegram и Android.

> ⚠️ Плагин выполняется с правами приложения и видит все ваши чаты. Ставьте только то, чему доверяете.

SDK version: **1** (`tg:sdk_version()` / `tg.sdk_version()`).

## Формат плагина

Плагин — это zip-архив с расширением **`.mgplugin`**:

```
my_plugin.mgplugin
├── plugin.json        манифест (обязательно)
├── main.lua | main.py точка входа
├── icon.png           аватарка (квадрат, ≥ 96×96), необязательно
└── icon.json          Lottie-анимация вместо аватарки, необязательно
```

### plugin.json

```json
{
  "id": "hello_lua",
  "name": "Hello",
  "author": "Артём",
  "version": "1.0",
  "description": "Пример плагина",
  "language": "lua",
  "main": "main.lua",
  "icon": "icon.png",
  "animation": "icon.json",
  "min_sdk": 1
}
```

| Поле | Обяз. | Описание |
| --- | --- | --- |
| `id` | да | Уникальный id, `[A-Za-z0-9_.-]`, до 64 символов. Повторная установка с тем же id обновляет плагин. |
| `name` | — | Название в списке плагинов |
| `author`, `version`, `description` | — | Показываются на странице плагина |
| `language` | — | `lua` (по умолчанию) или `python` |
| `main` | — | Файл входа, по умолчанию `main.lua` / `main.py` |
| `icon` | — | PNG/JPG-аватарка |
| `animation` | — | Lottie JSON — анимированная аватарка (приоритетнее `icon`) |
| `min_sdk` | — | Минимальная версия SDK |

Установка: **Настройки → Marblegram → Плагины на Lua и Python → «+»** и выбрать `.mgplugin`.
После установки плагин выключен — включите его переключателем.

## Хуки (события)

Объявите в скрипте функцию верхнего уровня с нужным именем — клиент вызовет её сам.

| Функция | Когда | Возврат |
| --- | --- | --- |
| `on_load()` | после запуска скрипта (включение / старт приложения) | — |
| `on_unload()` | перед выключением или удалением | — |
| `on_app_start()` | приложение запустилось, все плагины загружены | — |
| `on_send_message(dialog_id, text)` | вы отправляете текст | новый текст; `false` или `""` — отменить отправку; `nil`/`None` — не менять |
| `on_message(dialog_id, text, out)` | новое сообщение в любом чате (`out` = исходящее) | — |
| `on_chat_open(dialog_id)` | открыт чат | — |

`dialog_id`: > 0 — пользователь, < 0 — группа/канал.

## Объект `tg`

| Метод | Описание |
| --- | --- |
| `sdk_version()` | Версия SDK |
| `plugin_id()`, `plugin_dir()` | id и папка плагина (для своих файлов) |
| `context()` | Android `Context` приложения |
| `account()`, `self_id()` | Текущий аккаунт и ваш user id |
| `user_name(user_id)`, `chat_title(dialog_id)` | Имена |
| `log(msg)` | Пишет в logcat (`adb logcat -s MarblePlugin`) |
| `toast(text)` | Material-снэкбар на текущем экране |
| `send_message(dialog_id, text)` | Отправить сообщение (хуки других плагинов на него не срабатывают) |
| `add_action(title, function_name)` | Кнопка на странице плагина, вызывает функцию |
| `get_setting(key, default)`, `set_setting(key, value)` | Хранилище строк плагина |
| `run_on_ui(runnable)` | Выполнить на UI-потоке |

В **Lua** методы вызываются через двоеточие: `tg:toast("hi")`. В **Python** — через точку: `tg.toast("hi")`.

## Полный доступ к Telegram (мост к Java)

**Lua** (LuaJ `luajava`):

```lua
local MessagesController = luajava.bindClass("org.telegram.messenger.MessagesController")
local mc = MessagesController:getInstance(tg:account())
tg:log("dialogs loaded: " .. tostring(mc.dialogsEndReached))
```

**Python** (Chaquopy):

```python
from java import jclass
MessagesController = jclass("org.telegram.messenger.MessagesController")
mc = MessagesController.getInstance(tg.account())
```

Через мост доступны все классы приложения (`org.telegram.*`, `org.telegram.marble.*`) и Android SDK:
можно открывать экраны (`LaunchActivity.getLastFragment().presentFragment(...)`), менять тему
(`org.telegram.marble.MarbleTheme`), подписываться на `NotificationCenter` и т.д.

## Пример: Smart Mentions (Python)

`examples/smart_mentions` — пишете в любом чате:
- `@gemini что такое майнкрафт?` — плагин не отправляет это сообщение, спрашивает Google Gemini и отправляет вопрос с ответом;
- `@wiki майнкрафт` — краткая статья из Википедии со ссылкой (кириллица → ru, иначе en; `@wiki:de Berlin` — нужный язык).

Ключ Gemini бесплатный: aistudio.google.com → Get API key, затем «Ключ Gemini API» на странице плагина.

## Пример на Lua

`examples/hello_lua` — отвечает на `!ping`, добавляет кнопку и считает отправленные сообщения.

## Пример на Python

`examples/translit_py` — переводит латиницу в кириллицу, если сообщение начинается с `!ru `.

## Сборка `.mgplugin`

```sh
cd examples/hello_lua && zip -r ../hello_lua.mgplugin .
```

## Отладка

- Ошибки показываются под названием плагина и на его странице.
- «Перезапустить» на странице плагина перечитывает скрипт без перезапуска приложения.
- `adb logcat -s MarblePlugin` — вывод `tg:log` и стектрейсы.

## Python в сборке

Python 3.11 встроен в Marblegram (Chaquopy 17.1, ≈ +15 МБ на архитектуру). Поддерживаются все
архитектуры (arm64-v8a, armeabi-v7a, x86, x86_64), минимальная версия Android — 7.0.
Сторонние pip-пакеты в сборку не входят: используйте стандартную библиотеку Python и мост к Java.

## Документация

| Файл | Что внутри |
| --- | --- |
| [API.md](API.md) | Полный справочник объекта `tg`, хуков и порядка их вызова |
| [LUA.md](LUA.md) | Особенности Lua: синтаксис вызовов, типы, `luajava`, модули |
| [PYTHON.md](PYTHON.md) | Особенности Python: `jclass`, потоки, типы, стандартная библиотека |
| [RECIPES.md](RECIPES.md) | Готовые рецепты: экраны, NotificationCenter, тема, перевод, ИИ, призрачный режим |
| [PUBLISHING.md](PUBLISHING.md) | Сборка `.mgplugin`, версии, обновления, безопасность |
