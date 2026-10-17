# Marblegram plugin example (Python)

TABLE = {
    "shch": "щ", "yo": "ё", "zh": "ж", "kh": "х", "ts": "ц", "ch": "ч", "sh": "ш",
    "yu": "ю", "ya": "я", "a": "а", "b": "б", "v": "в", "g": "г", "d": "д", "e": "е",
    "z": "з", "i": "и", "y": "ы", "k": "к", "l": "л", "m": "м", "n": "н", "o": "о",
    "p": "п", "r": "р", "s": "с", "t": "т", "u": "у", "f": "ф", "h": "х", "j": "й",
    "'": "ь",
}
KEYS = sorted(TABLE, key=len, reverse=True)


def translit(text):
    out, i = [], 0
    while i < len(text):
        for k in KEYS:
            chunk = text[i:i + len(k)]
            if chunk.lower() == k:
                ru = TABLE[k]
                out.append(ru.upper() if chunk[0].isupper() else ru)
                i += len(k)
                break
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def on_load():
    tg.log("translit_py loaded")
    tg.add_action("Проверить", "demo")


def on_send_message(dialog_id, text):
    if text.startswith("!ru "):
        return translit(text[4:])
    return None


def demo():
    tg.toast(translit("Privet iz Marblegram!"))
