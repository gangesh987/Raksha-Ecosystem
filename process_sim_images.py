import os
from PIL import Image

CALLER_IMG = os.environ.get("CALLER_IMG", os.path.join("media", "sim_caller_video.png"))
USER_IMG = os.environ.get("USER_IMG", os.path.join("media", "sim_user_video.png"))

if not (os.path.exists(CALLER_IMG) and os.path.exists(USER_IMG)):
    print("[Info] Caller or User image not found. Skipping simulation image processing.")
    exit(0)

im_caller = Image.open(CALLER_IMG).convert("RGB")
im_caller = im_caller.resize((512, 512), Image.Resampling.LANCZOS)
im_caller.save("app/src/main/res/drawable/sim_caller_video.png", "PNG")
im_caller.save("media/sim_caller_video.png", "PNG")

im_user = Image.open(USER_IMG).convert("RGB")
im_user = im_user.resize((512, 512), Image.Resampling.LANCZOS)
im_user.save("app/src/main/res/drawable/sim_user_video.png", "PNG")
im_user.save("media/sim_user_video.png", "PNG")

print("Processed and saved sim_caller_video.png and sim_user_video.png!")
