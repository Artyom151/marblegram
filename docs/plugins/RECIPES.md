# Рецепты

Примеры на Lua; на Python — то же самое через `jclass`.

## Открыть экран Telegram

```lua
local LaunchActivity = luajava.bindClass("org.telegram.ui.LaunchActivity")
local ThemeActivity = luajava.bindClass("org.telegram.ui.ThemeActivity")
tg:run_on_ui(luajava.createProxy("java.lang.Runnable", { run = function()
    LaunchActivity:getLastFragment():presentFragment(luajava.new(ThemeActivity, 0))
end }))
```

## Перевести текст (бесплатный переводчик Marblegram)

```lua
local Translator = luajava.bindClass("org.telegram.marble.features.MarbleTranslator")
Translator:translate("Hello", "ru", luajava.createProxy(
    "org.telegram.marble.features.MarbleTranslator$Callback",
    { onResult = function(result, err)
        if result then tg:toast(result.text) else tg:toast("ошибка: " .. tostring(err)) end
    end }))
```

## Включить призрачный режим из плагина

```lua
local Ghost = luajava.bindClass("org.telegram.marble.features.MarbleGhost")
Ghost:set("ghostNoRead", true)
```

## Сменить цвет темы Marblegram

```lua
local Theme = luajava.bindClass("org.telegram.marble.MarbleTheme")
Theme:setDynamic(false)
Theme:setSeedIndex(2) -- 0 синий, 1 зелёный, 2 фиолетовый, …
```

## Подписаться на событие NotificationCenter

```lua
local NC = luajava.bindClass("org.telegram.messenger.NotificationCenter")
local observer = luajava.createProxy("org.telegram.messenger.NotificationCenter$NotificationCenterDelegate", {
    didReceivedNotification = function(id, account, args) tg:log("dialogs updated") end
})
function on_load()
    NC:getInstance(tg:account()):addObserver(observer, NC.dialogsNeedReload)
end
function on_unload()
    NC:getInstance(tg:account()):removeObserver(observer, NC.dialogsNeedReload)
end
```

## Сохранить файл плагина

```lua
local f = io.open(tg:plugin_dir() .. "/data.txt", "w")
f:write("hello")
f:close()
```
