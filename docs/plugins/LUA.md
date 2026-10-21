# Lua-плагины

Движок — **LuaJ 3.0.1** (Lua 5.2, чистая Java). Доступны стандартные библиотеки `string`, `table`,
`math`, `os`, `io` (в пределах песочницы Android), `coroutine`, а также `luajava`.

## Вызов методов `tg`

Методы Java-объекта вызываются через **двоеточие**:

```lua
tg:toast("Привет")
local me = tg:self_id()
```

## Типы

| Java | Lua |
| --- | --- |
| `long` / `int` / `double` | number |
| `String` | string |
| `boolean` | boolean |
| `null` | nil |
| объект | userdata (методы через `:`) |

## Мост к Java (`luajava`)

```lua
local LaunchActivity = luajava.bindClass("org.telegram.ui.LaunchActivity")
local fragment = LaunchActivity:getLastFragment()

local ArrayList = luajava.bindClass("java.util.ArrayList")
local list = luajava.new(ArrayList)
list:add("a")

-- реализовать Java-интерфейс Lua-таблицей
local runnable = luajava.createProxy("java.lang.Runnable", {
    run = function() tg:toast("на UI-потоке") end
})
tg:run_on_ui(runnable)
```

## Модули

`require "util"` ищет `util.lua` в папке плагина.

## Пример

См. `examples/hello_lua`.


## Работа с Java-списками (SDK 2)

`tg:find_views`, аргументы событий и поля TL-объектов — это `java.util.List`. Перебор:

```lua
function luajava_list(list)
  local t = {}
  for i = 0, list:size() - 1 do t[#t + 1] = list:get(i) end
  return t
end
```
