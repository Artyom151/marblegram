-- Marblegram plugin example (Lua)

local sent = tonumber(tg:get_setting("sent", "0"))

function on_load()
    tg:log("hello_lua loaded, sent so far: " .. sent)
    tg:add_action("Показать счётчик", "show_counter")
    tg:add_action("Сбросить счётчик", "reset_counter")
end

function on_send_message(dialog_id, text)
    sent = sent + 1
    tg:set_setting("sent", tostring(sent))
    if text == "!ping" then
        return "pong 🏓"
    end
    if text == "!cancel" then
        tg:toast("Отправка отменена плагином")
        return false
    end
    return nil -- оставить текст как есть
end

function on_message(dialog_id, text, out)
    if not out and text == "!marble" then
        tg:send_message(dialog_id, "Я на Marblegram 💎")
    end
end

function show_counter()
    tg:toast("Отправлено сообщений: " .. sent)
end

function reset_counter()
    sent = 0
    tg:set_setting("sent", "0")
    tg:toast("Счётчик сброшен")
end
