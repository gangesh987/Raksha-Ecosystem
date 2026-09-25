import os
import shutil
import zipfile
import time

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

# 1. Ensure freshest built APKs are copied to root
debug_apk_source = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
release_apk_source = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "release", "app-release.apk")

debug_apk_dest = os.path.join(ROOT_DIR, "RakshaCall-Debug.apk")
release_apk_dest = os.path.join(ROOT_DIR, "RakshaCall-Release.apk")

if os.path.exists(debug_apk_source):
    shutil.copy2(debug_apk_source, debug_apk_dest)
    print(f"Copied fresh Debug APK to: {debug_apk_dest} ({os.path.getsize(debug_apk_dest)/(1024*1024):.2f} MB)")

if os.path.exists(release_apk_source):
    shutil.copy2(release_apk_source, release_apk_dest)
    print(f"Copied fresh Release APK to: {release_apk_dest} ({os.path.getsize(release_apk_dest)/(1024*1024):.2f} MB)")

# 2. Package comprehensive final ZIP
OUTPUT_ZIP = os.path.join(ROOT_DIR, "RakshaCall_FINAL_FULL_PROJECT.zip")

EXCLUDE_DIRS = {
    ".gradle",
    ".kotlin",
    ".pytest_cache",
    "build",
    "app/build",
    "node_modules",
    "__pycache__",
    ".idea",
    ".git"
}

EXCLUDE_EXTENSIONS = {
    ".pyc"
}

EXCLUDE_FILES = {
    "APP OF RAKSHA.zip",
    "RakshaCall-Full-Project.zip",
    "RakshaCall_FINAL_REALTIME_PROTOTYPE.zip",
    "RakshaCall_FINAL_OVERALL_PROJECT.zip",
    "FINAL ONE.docx",
    "ppp.docx"
}

def should_exclude(rel_path):
    parts = rel_path.replace("\\", "/").split("/")
    for part in parts:
        if part in EXCLUDE_DIRS:
            return True
            
    norm_path = rel_path.replace("\\", "/")
    for ex in EXCLUDE_DIRS:
        if norm_path.startswith(ex + "/") or f"/{ex}/" in norm_path:
            return True
            
    _, ext = os.path.splitext(rel_path)
    if ext.lower() in EXCLUDE_EXTENSIONS:
        return True
        
    basename = os.path.basename(rel_path)
    if basename in EXCLUDE_FILES:
        return True
        
    if basename == "RakshaCall_FINAL_FULL_PROJECT.zip":
        return True
        
    return False

print(f"\n==================================================")
print(f"CREATING FINAL COMPREHENSIVE ZIP: {OUTPUT_ZIP}")
print(f"==================================================")

file_count = 0
total_bytes = 0
start_time = time.time()

with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(ROOT_DIR):
        dirs[:] = [d for d in dirs if not should_exclude(os.path.relpath(os.path.join(root, d), ROOT_DIR))]
        
        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, ROOT_DIR)
            
            if should_exclude(rel_path):
                continue
                
            zf.write(full_path, rel_path)
            file_count += 1
            total_bytes += os.path.getsize(full_path)

elapsed = time.time() - start_time
final_size = os.path.getsize(OUTPUT_ZIP)

print(f"Archive Created:      {OUTPUT_ZIP}")
print(f"Total Files Packaged: {file_count}")
print(f"Uncompressed Data:    {total_bytes / (1024*1024):.2f} MB")
print(f"Final Compressed ZIP: {final_size / (1024*1024):.2f} MB")
print(f"Build Time:           {elapsed:.2f} seconds")
print(f"==================================================")
