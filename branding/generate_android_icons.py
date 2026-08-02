from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "branding" / "moonlight-icon-foreground.png"
RES = ROOT / "app" / "src" / "main" / "res"
DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}
BACKGROUND_START = (23, 20, 43, 255)
BACKGROUND_END = (119, 88, 166, 255)


def diagonal_gradient(size: int) -> Image.Image:
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    denominator = max(1, 2 * (size - 1))
    for y in range(size):
        for x in range(size):
            ratio = (x + y) / denominator
            pixels[x, y] = tuple(
                round(start + (end - start) * ratio)
                for start, end in zip(BACKGROUND_START, BACKGROUND_END)
            )
    return image


def rounded_background(size: int) -> Image.Image:
    gradient = diagonal_gradient(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size - 1, size - 1),
        radius=round(size * 0.23),
        fill=255,
    )
    gradient.putalpha(mask)
    return gradient


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def main() -> None:
    foreground = Image.open(SOURCE).convert("RGBA")
    for density, scale in DENSITIES.items():
        adaptive_size = round(108 * scale)
        adaptive = foreground.resize(
            (adaptive_size, adaptive_size), Image.Resampling.LANCZOS
        )
        save_png(
            adaptive,
            RES / f"mipmap-{density}" / "ic_app_foreground.png",
        )

        legacy_size = round(48 * scale)
        legacy = rounded_background(legacy_size)
        legacy_foreground = foreground.resize(
            (legacy_size, legacy_size), Image.Resampling.LANCZOS
        )
        legacy.alpha_composite(legacy_foreground)
        save_png(legacy, RES / f"mipmap-{density}" / "ic_app.png")


if __name__ == "__main__":
    main()
