"""Draws the launcher banner and icon: res/drawable-xhdpi/{banner,icon}.png.

Run: python3 tools/make_art.py  (needs Pillow)
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "res" / "drawable-xhdpi"
BG = (14, 52, 110)
FG = (255, 255, 255)
ACCENT = (86, 182, 255)
FONTS = [
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]


def font(size):
    for path in FONTS:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def bluetooth(draw, cx, cy, h, width, color):
    """The Bluetooth rune: a vertical stroke with two arrow loops."""
    t, b, w = cy - h / 2, cy + h / 2, h * 0.28
    pts = [(cx - w, cy - h * 0.25), (cx + w, cy + h * 0.25), (cx, b),
           (cx, t), (cx + w, cy - h * 0.25), (cx - w, cy + h * 0.25)]
    draw.line(pts, fill=color, width=width, joint="curve")


def ok_badge(draw, cx, cy, r, size):
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=ACCENT)
    f = font(size)
    box = draw.textbbox((0, 0), "OK", font=f)
    draw.text((cx - (box[2] - box[0]) / 2 - box[0], cy - (box[3] - box[1]) / 2 - box[1]),
              "OK", font=f, fill=BG)


def banner():
    img = Image.new("RGB", (640, 360), BG)
    d = ImageDraw.Draw(img)
    bluetooth(d, 150, 180, 190, 18, FG)
    ok_badge(d, 215, 255, 52, 44)
    f = font(46)
    d.text((300, 118), "Bluetooth", font=f, fill=FG)
    d.text((300, 178), "OK fix", font=f, fill=ACCENT)
    img.save(OUT / "banner.png")


def icon():
    img = Image.new("RGB", (96, 96), BG)
    d = ImageDraw.Draw(img)
    bluetooth(d, 40, 48, 60, 7, FG)
    ok_badge(d, 66, 70, 20, 17)
    img.save(OUT / "icon.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    banner()
    icon()
    print("wrote", OUT / "banner.png", OUT / "icon.png")
