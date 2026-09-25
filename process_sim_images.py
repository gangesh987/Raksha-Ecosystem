import os
from PIL import Image

CALLER_IMG = r"C:\Users\gangs\.gemini\antigravity-ide\brain\212b0a25-e17b-46c0-ba0d-1604a64a484c\simulated_caller_3d_1789310759650.jpg"
USER_IMG = r"C:\Users\gangs\.gemini\antigravity-ide\brain\212b0a25-e17b-46c0-ba0d-1604a64a484c\simulated_user_3d_1789310811288.jpg"

im_caller = Image.open(CALLER_IMG).convert("RGB")
im_caller = im_caller.resize((512, 512), Image.Resampling.LANCZOS)
im_caller.save("app/src/main/res/drawable/sim_caller_video.png", "PNG")
im_caller.save("media/sim_caller_video.png", "PNG")

im_user = Image.open(USER_IMG).convert("RGB")
im_user = im_user.resize((512, 512), Image.Resampling.LANCZOS)
im_user.save("app/src/main/res/drawable/sim_user_video.png", "PNG")
im_user.save("media/sim_user_video.png", "PNG")

print("Processed and saved sim_caller_video.png and sim_user_video.png!")
