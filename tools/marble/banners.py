#!/usr/bin/env python3
"""
Marblegram release banners in M3 Expressive style.

  python3 tools/marble/banners.py                                  # all kinds → docs/banners/
  python3 tools/marble/banners.py --kind bugfix --version 1.2.1    # one banner
  python3 tools/marble/banners.py --kind update --version 1.3.0 --items "Новое меню|Тёмная тема|Быстрее загрузка"

Kinds: update, feature, bugfix, hotfix, beta, plugins. Needs Pillow with FreeType (pip install pillow).
"""

import argparse
import os
import urllib.request

from PIL import Image

import banner_kit as k

CODEPOINTS = os.path.expanduser("~/.cache/marblegram/MaterialSymbolsRounded.codepoints")
_cp = {}


def icon(name):
    if not _cp:
        if not os.path.exists(CODEPOINTS):
            os.makedirs(os.path.dirname(CODEPOINTS), exist_ok=True)
            urllib.request.urlretrieve(k.SYMBOLS_URL.replace(".ttf", ".codepoints"), CODEPOINTS)
        for line in open(CODEPOINTS, encoding="utf-8"):
            n, h = line.split()
            _cp[n] = int(h, 16)
    return _cp[name]


# kind → tag text, tag color, chips (icon, text) for the features card
KINDS = {
    "update":  ("Обновление", "#3F5BC9", [
        ("auto_awesome", "Material 3 Expressive"), ("text_fields", "Roboto Flex"),
        ("visibility_off", "Призрачный режим"), ("history", "История правок"),
        ("translate", "Перевод без лимитов"), ("extension", "Плагины SDK 2")]),
    "feature": ("Новые функции", "#6750A4", [
        ("smart_toy", "ИИ-ответы"), ("translate", "Перевод"), ("delete_history", "Удалённые сообщения"),
        ("photo_camera_front", "Камера кружков"), ("folder", "Папки ButtonGroup"), ("bolt", "Pocket Tools")]),
    "bugfix":  ("Исправления", "#2E7D32", [
        ("check_circle", "Падение экрана профиля"), ("check_circle", "Индикатор вкладок профиля"),
        ("check_circle", "Обрезка переключателей"), ("check_circle", "Слайдеры"),
        ("check_circle", "Текст выбранной вкладки"), ("check_circle", "Ложная пометка каналов")]),
    "hotfix":  ("Срочное исправление", "#C62828", [
        ("build", "Вылет при запуске"), ("build", "Отправка сообщений"), ("build", "Синхронизация")]),
    "beta":    ("Бета-версия", "#B26A00", [
        ("science", "Тестовые функции"), ("bug_report", "Сообщить об ошибке"), ("forum", "Чат тестеров")]),
    "plugins": ("Плагины", "#00796B", [
        ("code", "Python и Lua"), ("api", "Любой метод API"), ("dashboard_customize", "Любой экран"),
        ("notifications", "Любое событие"), ("lock", "Предупреждение о доступе"), ("menu_book", "Документация")]),
}

W = H = 1600
CARD = "#FFFFFF"


def header(c, version, tag, tag_color):
    logo = Image.open(k.LOGO)
    title = "Marblegram " + version
    ft = k.font(92, 760, 96, 96)
    tw = c.text_w(title, ft)
    total = 104 + 28 + tw
    x0 = (W - total) / 2
    # logo: the app icon cut out along its white Cookie badge
    from PIL import ImageDraw as _D
    m = Image.new("L", (512, 512), 0)
    _D.Draw(m).polygon(k.shape_points("cookie12", 256, 256, 158, n=360), fill=255)
    c.paste(logo, x0 - 32, 110 - 84, 168, mask=m)
    c.text(x0 + 132, 110, title, ft, "#FFFFFF", "lm")
    fl = k.font(30, 650, 100)
    lw = c.text_w(tag, fl) + 56
    c.rrect((W - lw) / 2, 186, lw, 56, 28, "#FFFFFF")
    c.circle((W - lw) / 2 + 30, 214, 7, tag_color)
    c.text((W - lw) / 2 + 46, 214, tag, fl, tag_color, "lm")


def card_m3(c, x, y, w):
    """«Material 3 Expressive» toggle + wavy progress + MaterialShapes row."""
    h = 330
    c.rrect(x, y, w, h, 56, CARD)
    ft = k.font(34, 560, 100)
    c.text(x + 56, y + 76, "Material 3 Expressive", ft, k.ON_SURFACE, "lm")
    bw = k.badge(c, x + 56 + c.text_w("Material 3 Expressive", ft) + 16, y + 64, "NEW", k.PRIMARY)
    k.switch(c, x + w - 56 - 52 - 8, y + 60, True)
    k.wavy(c, x + 56, y + 168, w - 112, 0.55, k.PRIMARY, k.PRIMARY_CONTAINER)
    names = ["soft_burst", "cookie9", "pentagon", "circle", "sunny", "cookie4", "oval"]
    step = (w - 112) / len(names)
    for i, n in enumerate(names):
        cx = x + 56 + step * (i + 0.5)
        color = k.PRIMARY if n == "circle" else "#7E95EA"
        k.Canvas.polygon(c, k.shape_points(n, cx, y + 262, 30 if n != "circle" else 34, rot_deg=i * 9), color)
    return h


def card_folders(c, x, y, w):
    """Folders as an M3E ButtonGroup."""
    h = 150
    c.rrect(x, y, w, h, 56, CARD)
    items = ["Все", "Личные", "Каналы", "Боты"]
    f = k.font(30, 600, 100)
    gap = 12
    widths = [c.text_w(t, f) + 64 for t in items]
    total = sum(widths) + gap * (len(items) - 1)
    xx = x + (w - total) / 2
    for i, t in enumerate(items):
        sel = i == 1
        c.rrect(xx, y + 40, widths[i], 70, 35 if sel else 20, k.PRIMARY if sel else k.SURFACE_HIGH)
        c.text(xx + widths[i] / 2, y + 75, t, f, "#FFFFFF" if sel else k.ON_SURFACE, "mm")
        xx += widths[i] + gap
    return h


def card_plugins(c, x, y, w):
    """Plugins list with M3 switches."""
    rows = [("Smart Mentions", "@gemini и @wiki прямо в чате", "#6750A4", "alternate_email"),
            ("Pocket Tools", "@weather · @rate · @calc", "#3F5BC9", "bolt"),
            ("Ваш плагин", "Python или Lua, SDK 2", "#00796B", "code")]
    h = 96 + 112 * len(rows)
    c.rrect(x, y, w, h, 56, CARD)
    c.text(x + 56, y + 60, "Плагины", k.font(28, 650, 100), k.PRIMARY, "lm")
    for i, (title, sub, col, ic) in enumerate(rows):
        ry = y + 96 + i * 112
        c.rrect(x + 24, ry, w - 48, 100, 28 if i not in (0, len(rows) - 1) else 28, k.SURFACE)
        c.polygon(k.shape_points("cookie9", x + 84, ry + 50, 32), col)
        c.icon(x + 84, ry + 50, icon(ic), 32, "#FFFFFF")
        c.text(x + 140, ry + 36, title, k.font(30, 560, 100), k.ON_SURFACE, "lm")
        c.text(x + 140, ry + 70, sub, k.font(22, 400, 100), k.ON_SURFACE_VAR, "lm")
        k.switch(c, x + w - 56 - 60, ry + 34, i < 2)
    return h


def card_chips(c, x, y, w, chips, tint):
    rows, cur, f = [], [], k.font(22, 500, 100)
    used = 0
    for ic, t in chips:
        cw = c.text_w(t, f) + 72
        if cur and used + cw + 14 > w - 64:
            rows.append(cur)
            cur, used = [], 0
        cur.append((ic, t, cw))
        used += cw + 14
    if cur:
        rows.append(cur)
    h = 64 + len(rows) * 66
    c.rrect(x, y, w, h, 56, CARD)
    for r, row in enumerate(rows):
        total = sum(cw for _, _, cw in row) + 14 * (len(row) - 1)
        xx = x + (w - total) / 2
        for ic, t, cw in row:
            k.chip(c, xx, y + 32 + r * 66, icon(ic), t, k.SURFACE_HIGH, tint)
            xx += cw + 14
    return h


def card_items(c, x, y, w, items, tint):
    """Custom text list (from --items)."""
    h = 72 + 64 * len(items)
    c.rrect(x, y, w, h, 56, CARD)
    for i, t in enumerate(items):
        yy = y + 68 + i * 64
        c.icon(x + 76, yy, icon("check_circle"), 34, tint)
        c.text(x + 112, yy, t, k.font(30, 500, 100), k.ON_SURFACE, "lm")
    return h


def render(kind, version, out, items=None):
    bottom = layout(k.Canvas(W, 2400, k.BRAND), kind, version, items)  # measuring pass
    h = int(bottom + 96)
    c = k.Canvas(W, h, k.BRAND)
    # soft expressive shapes in the background
    c.polygon(k.shape_points("cookie12", W - 120, h - 60, 360, 10), "#6A88E6")
    c.polygon(k.shape_points("sunny", 80, h - 140, 260, 0), "#6584E4")
    c.polygon(k.shape_points("cookie9", W - 60, 120, 180, 0), "#6584E4")
    layout(c, kind, version, items)
    c.save(out)


def layout(c, kind, version, items):
    tag, color, chips = KINDS[kind]
    header(c, version, tag, color)

    gx, gy, col_w, gap = 72, 290, (W - 72 * 2 - 32) / 2, 32
    # left column
    y = gy
    y += card_m3(c, gx, y, col_w) + gap
    y += card_folders(c, gx, y, col_w) + gap
    if items:
        y += card_items(c, gx, y, col_w, items, color)
    elif kind not in ("bugfix", "hotfix"):  # fix kinds list their items in the right column
        y += card_chips(c, gx, y, col_w, chips, color)
    else:
        y -= gap
    left = y
    # right column
    x2 = gx + col_w + gap
    y = gy
    y += card_plugins(c, x2, y, col_w) + gap
    if kind in ("bugfix", "hotfix"):
        y += card_items(c, x2, y, col_w, [t for _, t in chips], color)
    else:
        y += card_chips(c, x2, y, col_w, KINDS["plugins"][2], k.PRIMARY)
    return max(left, y)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kind", choices=sorted(KINDS), help="one banner kind (default: all)")
    ap.add_argument("--version", default="1.2.0")
    ap.add_argument("--items", help="custom list for the left card, separated by |")
    ap.add_argument("--out", default=os.path.join(k.ROOT, "docs", "banners"))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    items = a.items.split("|") if a.items else None
    for kind in ([a.kind] if a.kind else KINDS):
        path = os.path.join(a.out, "marblegram-%s-%s.png" % (a.version, kind))
        render(kind, a.version, path, items)
        print(path)


if __name__ == "__main__":
    main()
