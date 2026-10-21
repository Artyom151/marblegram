# Pocket Tools — Marblegram plugin (no API keys needed)
#   @weather <city>             current weather + today's range (wttr.in)
#   @rate <amount> <from> <to>  currency conversion, e.g. @rate 100 usd rub (open.er-api.com)
#   @calc <expression>          safe calculator: + - * / // % ^ ( ), sqrt, sin, cos, pi, e …

import ast
import json
import math
import operator
import re
import urllib.parse

from java import dynamic_proxy, jclass

Runnable = jclass("java.lang.Runnable")


class Task(dynamic_proxy(Runnable)):
    def __init__(self, fn):
        super().__init__()
        self.fn = fn

    def run(self):
        self.fn()


def background(fn):
    tg.run_in_background(Task(fn))


def on_load():
    tg.add_action("Проверить @weather", "demo_weather")
    tg.add_action("Проверить @calc", "demo_calc")


def demo_weather():
    background(lambda: tg.toast(weather("Москва")))


def demo_calc():
    tg.toast(calc("2^10 * 3 + sqrt(16)"))


# --- hook -------------------------------------------------------------------------------------

def on_send_message(dialog_id, text):
    s = text.strip()
    low = s.lower()

    if low.startswith("@weather"):
        city = s[len("@weather"):].strip()
        if not city:
            tg.toast("Напишите город после @weather")
            return False
        tg.toast("Смотрю погоду…")
        background(lambda: tg.send_message(dialog_id, weather(city)))
        return False

    if low.startswith("@rate"):
        m = re.match(r"@rate\s+([\d.,]+)?\s*([a-zA-Z]{3})\s+(?:в|to|in)?\s*([a-zA-Z]{3})\s*$", s, re.IGNORECASE)
        if not m:
            tg.toast("Формат: @rate 100 usd rub")
            return False
        amount = float((m.group(1) or "1").replace(",", "."))
        src, dst = m.group(2).upper(), m.group(3).upper()
        tg.toast("Смотрю курс…")
        background(lambda: tg.send_message(dialog_id, rate(amount, src, dst)))
        return False

    if low.startswith("@calc"):
        expr = s[len("@calc"):].strip()
        if not expr:
            tg.toast("Напишите выражение после @calc")
            return False
        tg.send_message(dialog_id, calc(expr))
        return False

    return None


# --- weather ----------------------------------------------------------------------------------

def weather(city):
    try:
        lang = "ru" if re.search(r"[а-яё]", city, re.IGNORECASE) else "en"
        data = json.loads(str(tg.http_get("https://wttr.in/" + urllib.parse.quote(city) + "?format=j1&lang=" + lang)))
        cur = data["current_condition"][0]
        today = data["weather"][0]
        desc = (cur.get("lang_" + lang) or cur.get("weatherDesc") or [{"value": ""}])[0]["value"]
        area = data.get("nearest_area", [{}])[0]
        place = (area.get("areaName") or [{"value": city}])[0]["value"]
        return ("🌤 %s: %s°C (ощущается %s°C), %s\n"
                "Сегодня %s…%s°C · ветер %s км/ч · влажность %s%%") % (
            place, cur["temp_C"], cur["FeelsLikeC"], desc.lower(),
            today["mintempC"], today["maxtempC"], cur["windspeedKmph"], cur["humidity"])
    except Exception as e:
        return "🌤 Не удалось узнать погоду для «%s»: %s" % (city, str(e)[:150])


# --- currency ---------------------------------------------------------------------------------

def rate(amount, src, dst):
    try:
        data = json.loads(str(tg.http_get("https://open.er-api.com/v6/latest/" + src)))
        if data.get("result") != "success":
            return "💱 Неизвестная валюта: " + src
        r = data["rates"].get(dst)
        if r is None:
            return "💱 Неизвестная валюта: " + dst
        return "💱 %s %s = %s %s\n1 %s = %s %s" % (fmt(amount), src, fmt(amount * r), dst, src, fmt(r), dst)
    except Exception as e:
        return "💱 Курс недоступен: " + str(e)[:150]


def fmt(x):
    if abs(x) < 1:
        return ("%.4f" % x).rstrip("0").rstrip(".")
    return "{:,.2f}".format(x).replace(",", " ").replace(".00", "")


# --- calculator (AST whitelist, no eval) ------------------------------------------------------

OPS = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul, ast.Div: operator.truediv,
       ast.FloorDiv: operator.floordiv, ast.Mod: operator.mod, ast.Pow: operator.pow,
       ast.USub: operator.neg, ast.UAdd: operator.pos}
FUNCS = {name: getattr(math, name) for name in
         ("sqrt", "sin", "cos", "tan", "log", "log10", "log2", "exp", "floor", "ceil", "factorial", "radians", "degrees")}
FUNCS["abs"] = abs
FUNCS["round"] = round
CONSTS = {"pi": math.pi, "e": math.e}


def _eval(node):
    if isinstance(node, ast.Expression):
        return _eval(node.body)
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return node.value
    if isinstance(node, ast.Name) and node.id in CONSTS:
        return CONSTS[node.id]
    if isinstance(node, ast.BinOp) and type(node.op) in OPS:
        left, right = _eval(node.left), _eval(node.right)
        if isinstance(node.op, ast.Pow) and abs(right) > 1000:
            raise ValueError("слишком большая степень")
        return OPS[type(node.op)](left, right)
    if isinstance(node, ast.UnaryOp) and type(node.op) in OPS:
        return OPS[type(node.op)](_eval(node.operand))
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name) and node.func.id in FUNCS and len(node.args) == 1:
        return FUNCS[node.func.id](_eval(node.args[0]))
    raise ValueError("недопустимое выражение")


def calc(expr):
    src = expr.replace("^", "**").replace("×", "*").replace("÷", "/").replace(",", ".")
    src = re.sub(r"(\d+(?:\.\d+)?)%", r"(\1/100)", src)
    try:
        value = _eval(ast.parse(src, mode="eval"))
        if isinstance(value, float) and value.is_integer() and abs(value) < 1e15:
            value = int(value)
        return "🧮 %s = %s" % (expr, value)
    except ZeroDivisionError:
        return "🧮 %s — деление на ноль" % expr
    except Exception as e:
        return "🧮 %s — %s" % (expr, e)
