import os
import zipfile
import time

OUTPUT_ZIP = "RakshaCall_FINAL_OVERALL_PROJECT.zip"

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
    ".pyc",
    ".apk",
    ".zip"  # Exclude existing zip files to avoid recursion and bloated sizes
}

EXCLUDE_EXACT_FILES = {
    "APP OF RAKSHA.zip",
    "RakshaCall-Full-Project.zip",
    "RakshaCall_FINAL_REALTIME_PROTOTYPE.zip",
    "RakshaCall-Debug.apk",
    "RakshaCall-Release.apk",
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
        if norm_path.startswith(ex + "/") or f"/{ex}/" in norm_path or norm_path.endswith("/" + ex):
            return True
            
    _, ext = os.path.splitext(rel_path)
    if ext.lower() in EXCLUDE_EXTENSIONS:
        return True
        
    basename = os.path.basename(rel_path)
    if basename in EXCLUDE_EXACT_FILES or basename.endswith(".zip"):
        return True
        
    return False

print(f"==================================================")
print(f"PACKAGING FINAL OVERALL ZIP: {OUTPUT_ZIP}")
print(f"==================================================")

file_count = 0
total_uncompressed = 0
start_time = time.time()

with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk("."):
        # Prune excluded directories in-place for fast traversal
        dirs[:] = [d for d in dirs if not should_exclude(os.path.relpath(os.path.join(root, d), "."))]
        
        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, ".")
            
            if should_exclude(rel_path):
                continue
                
            zf.write(full_path, rel_path)
            file_count += 1
            total_uncompressed += os.path.getsize(full_path)

elapsed = time.time() - start_time
zip_size = os.path.getsize(OUTPUT_ZIP)

print(f"Archive Created Successfully: {OUTPUT_ZIP}")
print(f"Total Files Included: {file_count}")
print(f"Uncompressed Data:   {total_uncompressed / (1024*1024):.2f} MB")
print(f"Final Compressed ZIP: {zip_size / (1024*1024):.2f} MB")
print(f"Time Taken:           {elapsed:.2f} seconds")
print(f"==================================================")
