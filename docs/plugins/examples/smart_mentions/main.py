# Smart Mentions — Marblegram plugin
#   @gemini <question>     ask Google Gemini, send question + answer to the chat
#   @wiki <topic>          Wikipedia summary (ru for Cyrillic, en otherwise)
#   @wiki:<lang> <topic>   Wikipedia in a specific language, e.g. @wiki:de Berlin

import json
import re
import urllib.parse

from java import dynamic_proxy, jclass

Runnable = jclass("java.lang.Runnable")

GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}"
DEFAULT_MODEL = "gemini-2.5-flash"
MAX_ANSWER = 3500  # keep under Telegram's 4096-char message limit


class Task(dynamic_proxy(Runnable)):
    def __init__(self, fn):
        super().__init__()
        self.fn = fn

    def run(self):
        self.fn()


def background(fn):
    tg.run_in_background(Task(fn))


# --- settings ---------------------------------------------------------------------------------

def on_load():
    tg.add_action("Ключ Gemini API", "ask_key")
    tg.add_action("Модель Gemini", "ask_model")
    tg.add_action("Проверить @wiki", "demo_wiki")
    if not tg.get_setting("gemini_key", ""):
        tg.log("Gemini key is not set yet")


def ask_key():
    tg.input("Ключ Gemini API", "AIza…", tg.get_setting("gemini_key", ""), True, "save_key")


def save_key(value):
    tg.set_setting("gemini_key", value.strip())
    tg.toast("Ключ Gemini сохранён")


def ask_model():
    tg.input("Модель Gemini", DEFAULT_MODEL, tg.get_setting("gemini_model", DEFAULT_MODEL), False, "save_model")


def save_model(value):
    tg.set_setting("gemini_model", value.strip() or DEFAULT_MODEL)
    tg.toast("Модель: " + (value.strip() or DEFAULT_MODEL))


def demo_wiki():
    background(lambda: tg.toast(wiki_summary("майнкрафт")[:300]))


# --- hook -------------------------------------------------------------------------------------

def on_send_message(dialog_id, text):
    stripped = text.strip()
    low = stripped.lower()

    if low.startswith("@gemini"):
        question = stripped[len("@gemini"):].strip()
        if not question:
            tg.toast("Напишите вопрос после @gemini")
            return False
        tg.toast("Спрашиваю Gemini…")
        background(lambda: answer_gemini(dialog_id, question))
        return False  # don't send the raw @gemini message

    m = re.match(r"@wiki(?::([a-z\-]{2,12}))?\s+(.+)", stripped, re.IGNORECASE | re.DOTALL)
    if m:
        lang, topic = m.group(1), m.group(2).strip()
        tg.toast("Ищу в Википедии…")
        background(lambda: tg.send_message(dialog_id, wiki_summary(topic, lang)))
        return False

    return None


# --- Gemini -----------------------------------------------------------------------------------

def answer_gemini(dialog_id, question):
    key = tg.get_setting("gemini_key", "")
    if not key:
        tg.toast("Сначала задайте ключ: Плагины → Smart Mentions → «Ключ Gemini API»")
        return
    model = tg.get_setting("gemini_model", DEFAULT_MODEL)
    body = json.dumps({
        "contents": [{"role": "user", "parts": [{"text": question}]}],
        "systemInstruction": {"parts": [{"text":
            "You answer questions asked inside a Telegram chat. Reply in the language of the question, "
            "concisely (a few sentences or a short list), plain text without Markdown."}]},
    })
    try:
        raw = tg.http_post(GEMINI_URL.format(model=urllib.parse.quote(model), key=urllib.parse.quote(key)),
                           body, "application/json; charset=utf-8")
        data = json.loads(str(raw))
        parts = data.get("candidates", [{}])[0].get("content", {}).get("parts", [])
        answer = "".join(p.get("text", "") for p in parts).strip()
        if not answer:
            reason = data.get("candidates", [{}])[0].get("finishReason") or data.get("promptFeedback", {})
            tg.toast("Gemini не ответил: " + str(reason))
            return
    except Exception as e:
        tg.toast("Ошибка Gemini: " + short_error(e))
        return
    if len(answer) > MAX_ANSWER:
        answer = answer[:MAX_ANSWER].rstrip() + "…"
    tg.send_message(dialog_id, "❓ " + question + "\n\n✨ Gemini: " + answer)


# --- Wikipedia --------------------------------------------------------------------------------

def guess_lang(topic):
    return "ru" if re.search(r"[а-яё]", topic, re.IGNORECASE) else "en"


def wiki_summary(topic, lang=None):
    lang = (lang or guess_lang(topic)).lower()
    base = "https://%s.wikipedia.org" % lang
    try:
        # find the best matching article title first (handles case, typos and redirects)
        search = json.loads(str(tg.http_get(base + "/w/api.php?action=opensearch&limit=1&namespace=0&format=json&search="
                                            + urllib.parse.quote(topic))))
        title = search[1][0] if len(search) > 1 and search[1] else topic
        page = json.loads(str(tg.http_get(base + "/api/rest_v1/page/summary/"
                                          + urllib.parse.quote(title.replace(" ", "_"), safe=""))))
    except Exception as e:
        return "📖 Википедия: не нашёл «%s» (%s)" % (topic, short_error(e))
    extract = (page.get("extract") or "").strip()
    url = page.get("content_urls", {}).get("desktop", {}).get("page", base + "/wiki/" + urllib.parse.quote(title))
    if page.get("type") == "disambiguation":
        extract = extract or "Многозначный термин — уточните запрос."
    if len(extract) > MAX_ANSWER:
        extract = extract[:MAX_ANSWER].rstrip() + "…"
    return "📖 %s\n\n%s\n\n%s" % (page.get("title", title), extract, url)


def short_error(e):
    text = str(e)
    m = re.search(r'"message"\s*:\s*"([^"]+)"', text)  # Google/Wikipedia JSON error body
    if m:
        text = m.group(1)
    return text[:200] + ("…" if len(text) > 200 else "")
