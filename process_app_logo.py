import os
from PIL import Image, ImageDraw

SRC_IMAGE = r"C:\Users\gangs\.gemini\antigravity-ide\brain\212b0a25-e17b-46c0-ba0d-1604a64a484c\rakshacall_icon_pure_1789310287734.jpg"
BRAIN_DIR = r"C:\Users\gangs\.gemini\antigravity-ide\brain\212b0a25-e17b-46c0-ba0d-1604a64a484c"

im = Image.open(SRC_IMAGE).convert("RGBA")

# Ensure directories
os.makedirs("media", exist_ok=True)
os.makedirs("app/src/main/res/drawable", exist_ok=True)

# 1. Save Full-res 1024x1024 & 512x512
im_1024 = im.resize((1024, 1024), Image.Resampling.LANCZOS)
im_1024.save("media/app_logo.png", "PNG")
im_1024.save(os.path.join(BRAIN_DIR, "app_logo.png"), "PNG")

im_512 = im.resize((512, 512), Image.Resampling.LANCZOS)
im_512.save("media/app_logo_512.png", "PNG")
im_512.save("app/src/main/res/drawable/app_logo.png", "PNG")
im_512.save(os.path.join(BRAIN_DIR, "app_logo_512.png"), "PNG")

# Mipmap densities
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def make_round(image, size):
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(resized, (0, 0), mask=mask)
    return result

for folder, size in densities.items():
    dir_path = os.path.join("app/src/main/res", folder)
    os.makedirs(dir_path, exist_ok=True)
    
    # Square icon
    sq = im.resize((size, size), Image.Resampling.LANCZOS)
    sq.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    
    # Round icon
    rd = make_round(im, size)
    rd.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")
    print(f"Generated {folder}: {size}x{size} square and round icons")

print("All app icon mipmaps successfully generated!")
