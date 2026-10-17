"""
Marblegram banner kit — drawing primitives in M3 Expressive style for release banners (Pillow).
Typography: Roboto Flex (bundled variable font). Icons: Material Symbols Rounded. Shapes: MaterialShapes formulas.
"""

import math
import os
import urllib.request

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FLEX = os.path.join(ROOT, "TMessagesProj/src/main/assets/fonts/marble_roboto_flex.ttf")
SYMBOLS = os.path.expanduser("~/.cache/marblegram/MaterialSymbolsRounded.ttf")
SYMBOLS_URL = ("https://github.com/google/material-design-icons/raw/master/variablefont/"
               "MaterialSymbolsRounded%5BFILL,GRAD,opsz,wght%5D.ttf")
LOGO = os.path.join(ROOT, "marblegram_logo.png")

SS = 2  # supersampling factor

# Marblegram brand (from the app icon) + M3 tonal roles
BRAND = "#5B7BE0"
SURFACE = "#F5F6FC"
SURFACE_HIGH = "#E9ECF8"
ON_SURFACE = "#1A1B21"
ON_SURFACE_VAR = "#5E6272"
PRIMARY = "#3F5BC9"
PRIMARY_CONTAINER = "#DCE2FF"
ON_PRIMARY_CONTAINER = "#0F1D5C"
OUTLINE = "#C7CAD8"

_fonts = {}


def ensure_symbols():
    if not os.path.exists(SYMBOLS):
        os.makedirs(os.path.dirname(SYMBOLS), exist_ok=True)
        urllib.request.urlretrieve(SYMBOLS_URL, SYMBOLS)


def _axis_name(a):
    n = a["name"]
    return (n.decode() if isinstance(n, bytes) else n).strip().lower()


def font(size, wght=400, wdth=100, opsz=None):
    """Roboto Flex instance (sizes are in output px; supersampling applied inside). Axes matched by exact name."""
    key = ("flex", size, wght, wdth, opsz)
    if key not in _fonts:
        f = ImageFont.truetype(FLEX, int(size * SS))
        want = {"weight": wght, "width": wdth, "optical size": opsz if opsz else min(144, max(8, size))}
        f.set_variation_by_axes([want.get(_axis_name(a), a["default"]) for a in f.get_variation_axes()])
        _fonts[key] = f
    return _fonts[key]


def icon_font(size, fill=1, wght=500):
    key = ("sym", size, fill, wght)
    if key not in _fonts:
        ensure_symbols()
        f = ImageFont.truetype(SYMBOLS, int(size * SS))
        want = {"fill": fill, "weight": wght, "optical size": min(48, max(20, size))}
        f.set_variation_by_axes([want.get(_axis_name(a), a["default"]) for a in f.get_variation_axes()])
        _fonts[key] = f
    return _fonts[key]


class Canvas:
    def __init__(self, w, h, bg):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w * SS, h * SS), bg)
        self.d = ImageDraw.Draw(self.img)

    @staticmethod
    def s(v):
        return int(round(v * SS))

    def rrect(self, x, y, w, h, r, fill, outline=None, width=0):
        self.d.rounded_rectangle([self.s(x), self.s(y), self.s(x + w), self.s(y + h)], radius=self.s(r),
                                 fill=fill, outline=outline, width=self.s(width))

    def circle(self, cx, cy, r, fill):
        self.d.ellipse([self.s(cx - r), self.s(cy - r), self.s(cx + r), self.s(cy + r)], fill=fill)

    def text(self, x, y, s, f, fill, anchor="la"):
        self.d.text((self.s(x), self.s(y)), s, font=f, fill=fill, anchor=anchor)

    def text_w(self, s, f):
        return self.d.textlength(s, font=f) / SS

    def icon(self, x, y, codepoint, size, fill, anchor="mm", filled=1):
        self.d.text((self.s(x), self.s(y)), chr(codepoint), font=icon_font(size, filled), fill=fill, anchor=anchor)

    def polygon(self, pts, fill):
        self.d.polygon([(self.s(px), self.s(py)) for px, py in pts], fill=fill)

    def line(self, pts, fill, width):
        self.d.line([(self.s(px), self.s(py)) for px, py in pts], fill=fill, width=self.s(width), joint="curve")
        r = width / 2
        for px, py in (pts[0], pts[-1]):
            self.circle(px, py, r, fill)

    def paste(self, im, x, y, size, mask=None):
        im = im.convert("RGBA").resize((self.s(size), self.s(size)), Image.LANCZOS)
        if mask is not None:
            im.putalpha(mask.resize(im.size, Image.LANCZOS))
        self.img.alpha_composite(im, (self.s(x), self.s(y)))

    def save(self, path):
        self.img.resize((self.w, self.h), Image.LANCZOS).convert("RGB").save(path, optimize=True)


# --- MaterialShapes (polar radius formulas, as in org.telegram.marble.MaterialShapes) --------------

def cookie(n, depth):
    return lambda a: 1 - depth * (0.5 - 0.5 * math.cos(n * a))


def star(n, depth, sharp):
    return lambda a: 1 - depth + depth * (0.5 + 0.5 * math.cos(n * a)) ** sharp


def rounded_polygon(n, rounding):
    def f(a):
        seg = 2 * math.pi / n
        x = (a % seg) - seg / 2
        r = math.cos(math.pi / n) / math.cos(x)
        return (1 - rounding) * r + rounding * 0.92
    return f


def oval(ratio):
    return lambda a: 1 / math.sqrt((math.cos(a) / 1) ** 2 + (math.sin(a) / ratio) ** 2) * ratio ** 0.5


SHAPES = {
    "soft_burst": star(10, 0.18, 1.0),
    "cookie9": cookie(9, 0.10),
    "pentagon": rounded_polygon(5, 0.42),
    "circle": lambda a: 1.0,
    "sunny": star(8, 0.10, 1.4),
    "cookie4": cookie(4, 0.18),
    "oval": oval(0.62),
    "cookie12": cookie(12, 0.07),
}


def shape_points(name, cx, cy, r, rot_deg=0, n=240):
    fn = SHAPES[name]
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n
        rr = fn(a) * r
        aa = a + math.radians(rot_deg - 90)
        pts.append((cx + rr * math.cos(aa), cy + rr * math.sin(aa)))
    return pts


# --- components --------------------------------------------------------------------------------------

def switch(c, x, y, on=True, color=PRIMARY):
    """M3 switch, 52x32."""
    c.rrect(x, y, 52, 32, 16, color if on else SURFACE_HIGH, outline=None if on else "#79747E", width=0 if on else 2)
    if on:
        c.circle(x + 36, y + 16, 12, "#FFFFFF")
    else:
        c.circle(x + 16, y + 16, 8, "#79747E")


def badge(c, x, y, text, color):
    f = font(15, 700, 100)
    w = c.text_w(text, f) + 16
    c.rrect(x, y, w, 24, 6, color)
    c.text(x + w / 2, y + 12, text, f, "#FFFFFF", "mm")
    return w


def wavy(c, x, y, w, progress, color, track):
    """M3E linear wavy progress indicator."""
    end = x + w * progress
    pts = []
    xx = x
    while xx <= end:
        ramp = min(1, (xx - x) / 18, (end - xx) / 18)
        pts.append((xx, y + 7 * ramp * math.sin((xx - x) / 34 * 2 * math.pi)))
        xx += 2
    if len(pts) > 1:
        c.line(pts, color, 8)
    if end + 16 < x + w:
        c.line([(end + 16, y), (x + w, y)], track, 8)
        c.circle(x + w - 4, y, 4, color)


def chip(c, x, y, codepoint, text, fill=SURFACE_HIGH, fg=ON_SURFACE):
    f = font(22, 500, 100)
    w = c.text_w(text, f) + 72
    c.rrect(x, y, w, 52, 26, fill)
    c.icon(x + 30, y + 26, codepoint, 26, fg)
    c.text(x + 52, y + 26, text, f, fg, "lm")
    return w
