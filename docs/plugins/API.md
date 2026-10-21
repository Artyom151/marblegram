# Marblegram Plugin SDK — справочник API (SDK 2)

## Жизненный цикл

1. Установка: `.mgplugin` распаковывается в `files/marble_plugins/<id>/`. Плагин выключен.
2. Включение / запуск приложения: скрипт выполняется целиком (верхний уровень), затем вызывается `on_load()`.
3. Работа: клиент вызывает хуки, когда происходят события.
4. Выключение / удаление / «Перезапустить»: `on_unload()`, затем движок уничтожается.
5. После старта приложения и загрузки всех плагинов — `on_app_start()`.

Ошибка в хуке не роняет клиент: она записывается в `plugin.error` (видна на странице плагина)
и в logcat (`MarblePlugin`), следующие вызовы продолжают работать.

## Хуки

Все хуки — необязательные функции верхнего уровня. Вызываются на UI-потоке, кроме `on_message`
(может прийти с потока уведомлений — для UI используйте `tg.run_on_ui`).

### `on_load()`
Плагин запущен. Регистрируйте здесь действия (`add_action`), читайте настройки.

### `on_unload()`
Плагин выключается. Освобождайте ресурсы, снимайте подписки на `NotificationCenter`.

### `on_app_start()`
Все плагины загружены, приложение готово.

### `on_send_message(dialog_id, text)` → `str | false | nil`
Вызывается перед отправкой текстового сообщения (и подписи не затрагиваются).
- вернуть строку — отправится она;
- вернуть `false` / `""` — отправка отменяется;
- вернуть `nil` / `None` — текст не меняется.
Плагины вызываются по очереди: следующий получает текст, изменённый предыдущим.
Сообщения, отправленные через `tg.send_message`, хуки не проходят.

### `on_message(dialog_id, text, out)`
Новое сообщение (входящее `out = false` или исходящее `out = true`) в любом чате, кроме отложенных.

### `on_chat_open(dialog_id)`
Пользователь открыл чат.

## Объект `tg`

| Метод | Возврат | Описание |
| --- | --- | --- |
| `sdk_version()` | int | Версия SDK (сейчас 1) |
| `plugin_id()` | str | id из манифеста |
| `plugin_dir()` | str | Абсолютный путь к папке плагина (можно хранить свои файлы) |
| `context()` | Context | Android-контекст приложения |
| `account()` | int | Индекс активного аккаунта (0…n) |
| `self_id()` | long | Ваш user id |
| `user_name(user_id)` | str \| nil | Имя пользователя |
| `chat_title(dialog_id)` | str \| nil | Название чата / имя собеседника |
| `log(msg)` | — | Сообщение в logcat с тегом `MarblePlugin` |
| `toast(text)` | — | Снэкбар Material на текущем экране |
| `send_message(dialog_id, text)` | — | Отправить текст |
| `add_action(title, fn_name)` | — | Кнопка на странице плагина, вызывает функцию `fn_name()` |
| `get_setting(key, default)` | str | Прочитать строку из хранилища плагина |
| `set_setting(key, value)` | — | Записать строку (хранилище своё у каждого плагина) |
| `run_on_ui(runnable)` | — | Выполнить на UI-потоке |
| `run_in_background(runnable)` | — | Выполнить в фоновом потоке (сеть, тяжёлая работа) |
| `input(title, hint, current, secret, fn_name)` | — | Диалог ввода текста; по «OK» вызывает `fn_name(text)`. `secret = true` скрывает ввод (ключи API) |
| `http_get(url)` | str | GET-запрос, возвращает тело ответа; при HTTP ≥ 400 бросает исключение с телом ошибки. Только из фонового потока |
| `http_post(url, body, content_type)` | str | POST-запрос (по умолчанию `application/json`). Только из фонового потока |

## Идентификаторы

- `dialog_id > 0` — личный чат (= user id), `dialog_id < 0` — группа или канал (= −chat id).
- `account()` используется классами Telegram: `MessagesController.getInstance(account)` и т.п.

## Ограничения

(SDK 2 снимает большую часть ограничений SDK 1 — см. разделы ниже.)

### Исторически, SDK 1

- Медиа (фото, файлы) в хуки не передаются — только текст.
- Хуки для меню сообщений и своих экранов — через мост к Java (см. RECIPES.md).


---

# SDK 2 — полный доступ к Telegram

`tg.sdk_version()` возвращает `2`. Всё ниже доступно и из Python, и из Lua.

> ⚠️ Плагин с SDK 2 может делать **всё**, что умеет приложение: читать и слать сообщения, вызывать любые методы
> MTProto, менять настройки и интерфейс. При установке `.mgplugin` из файла Marblegram показывает предупреждение
> «Полный доступ к аккаунту». Публикуйте исходники и не ставьте плагины от незнакомых авторов.

## Новые хуки

| Хук | Когда | Возврат |
|---|---|---|
| `on_fragment_resume(fragment, name)` | открылся/вернулся любой экран (`ChatActivity`, `DialogsActivity`, `ProfileActivity`…) | — |
| `on_request(request, name)` | **каждый** исходящий запрос MTProto (любой поток) | `False` — не отправлять; объект можно менять на месте |
| `on_update(updates, name)` | каждая пачка обновлений с сервера (`TL_updates`, `TL_updateShortMessage`…) | — |

`on_request` вызывается очень часто — делайте его быстрым и сразу выходите, если `name` вам не интересен.

## Рефлексия: любой класс, поле, метод

| Метод | Описание |
|---|---|
| `tg.cls(name)` | класс по полному или короткому имени: `"MessagesController"`, `"ChatActivity"`, `"TL_messages_getHistory"`, `"android.view.View"` |
| `tg.get(target, field)` | читает поле (в т.ч. private). `target` — объект, класс или имя класса (static) |
| `tg.set(target, field, value)` | пишет поле; числа и строки приводятся к типу поля |
| `tg.call(target, method, *args)` | вызывает любой метод; перегрузка выбирается по числу/типам аргументов |
| `tg.new_object(class, *args)` | создаёт объект любым конструктором |

Короткие имена ищутся в `org.telegram.messenger`, `org.telegram.ui(.ActionBar/.Components/.Cells/.Stories)`,
`org.telegram.tgnet(.TLRPC$)`, `org.telegram.marble`, `android.view/widget/graphics`, `java.util`.

## Контроллеры текущего аккаунта

`tg.messages()` (MessagesController), `tg.storage()` (MessagesStorage), `tg.connections()` (ConnectionsManager),
`tg.user_config()`, `tg.notifications()`, `tg.sender()` (SendMessagesHelper), `tg.media_data()`, `tg.contacts()`,
`tg.files()` (FileLoader), `tg.user(id)`, `tg.chat(id)`, `tg.input_peer(dialog_id)`.

## MTProto: любой метод API

| Метод | Описание |
|---|---|
| `tg.tl(name)` | новый TL-объект: `tg.tl("TL_messages_getHistory")` или `tg.tl("inputPeerSelf")` |
| `tg.send_request(request, "callback")` | отправляет запрос; `callback(response, error_text)`; возвращает id запроса |
| `tg.cancel_request(id)` | отменяет запрос |

## Сообщения

`tg.edit_message(dialog_id, message_id, text)`, `tg.delete_messages(dialog_id, [ids], revoke)`,
`tg.forward_messages(from_dialog, [ids], to_dialog)`.

## События: любое событие NotificationCenter

| Метод | Описание |
|---|---|
| `tg.subscribe(event, "fn")` | `fn(account, args)` на UI-потоке; `event` — имя поля `NotificationCenter` (`messagesDeleted`, `updateInterfaces`, `dialogsNeedReload`…) |
| `tg.unsubscribe(event, "fn")` | отписка (при выключении плагина — автоматически) |
| `tg.post_event(event, [args])` | отправить событие |

## Интерфейс: любой экран и любой View

| Метод | Описание |
|---|---|
| `tg.fragment()` | экран сверху стека |
| `tg.root_view()` | корневой View этого экрана |
| `tg.find_views(root, "ClassName")` | все View этого класса (или наследники) внутри `root` |
| `tg.open_chat(dialog_id)` | открыть чат |
| `tg.present(fragment)` | открыть любой экран (созданный через `tg.new_object`) |
| `tg.alert(title, text)` | диалог |
| `tg.set_theme_color(key, argb)` / `tg.theme_color(key)` | любой цвет темы по имени ключа без `key_` |
| `tg.save_config()` | сохранить `SharedConfig` после `tg.set("SharedConfig", поле, значение)` |

## Примеры (Python)

```python
# 1. Подсветить все исходящие пузыри и сделать заголовок чата красным
def on_fragment_resume(fragment, name):
    if name == "ChatActivity":
        tg.set_theme_color("chat_outBubble", 0xFF6750A4)
        title = tg.call(tg.get(fragment, "avatarContainer"), "getTitleTextView")
        tg.call(title, "setTextColor", 0xFFFF5252)

# 2. Любой метод API: последние 5 сообщений из «Избранного»
def on_load():
    req = tg.tl("TL_messages_getHistory")
    req.peer = tg.input_peer(tg.self_id())
    req.limit = 5
    tg.send_request(req, "got_history")

def got_history(response, error):
    if error: tg.toast(error); return
    tg.log([m.message for m in response.messages])

# 3. Не отправлять «печатает…» в один конкретный чат
SECRET = -1001234567890
def on_request(req, name):
    if name == "TL_messages_setTyping" and tg.call("DialogObject", "getPeerDialogId", req.peer) == SECRET:
        return False

# 4. Реакция на удаление сообщений
def on_load():
    tg.subscribe("messagesDeleted", "deleted")

def deleted(account, args):
    tg.log("удалены: %s" % args[0])

# 5. Поменять настройку Telegram
tg.set("SharedConfig", "noSoundHintShowed", True); tg.save_config()
```

## Примеры (Lua)

```lua
function on_fragment_resume(fragment, name)
  if name == "DialogsActivity" then
    for _, cell in ipairs(luajava_list(tg:find_views(tg:root_view(), "DialogCell"))) do
      cell:setAlpha(0.9)
    end
  end
end
```

(`luajava_list` — 3 строки: перебор `java.util.List` через `:size()`/`:get(i)`, см. LUA.md.)
