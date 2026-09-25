import os
import zipfile

OUTPUT_ZIP = "RakshaCall-Full-Project.zip"

EXCLUDE_DIRS = {
    ".gradle",
    ".kotlin",
    ".pytest_cache",
    "build",
    "app/build",
    "node_modules",
    "__pycache__",
    ".idea"
}

EXCLUDE_EXTENSIONS = {
    ".pyc",
    ".apk"
}

EXCLUDE_FILES = {
    OUTPUT_ZIP,
    "RakshaCall_FINAL_REALTIME_PROTOTYPE.zip"
}

def should_exclude(rel_path):
    parts = rel_path.replace("\\", "/").split("/")
    for part in parts:
        if part in EXCLUDE_DIRS:
            return True
    
    # Check compound relative paths
    norm_path = rel_path.replace("\\", "/")
    for ex in EXCLUDE_DIRS:
        if norm_path.startswith(ex + "/") or f"/{ex}/" in norm_path or norm_path.endswith("/" + ex):
            return True
            
    _, ext = os.path.splitext(rel_path)
    if ext.lower() in EXCLUDE_EXTENSIONS:
        return True
        
    basename = os.path.basename(rel_path)
    if basename in EXCLUDE_FILES:
        return True
        
    return False

print(f"Creating {OUTPUT_ZIP}...")
count = 0
total_uncompressed = 0

with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk("."):
        # Prune excluded directories in-place for speed
        dirs[:] = [d for d in dirs if not should_exclude(os.path.relpath(os.path.join(root, d), "."))]
        
        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, ".")
            
            if should_exclude(rel_path):
                continue
                
            zf.write(full_path, rel_path)
            count += 1
            total_uncompressed += os.path.getsize(full_path)

zip_size = os.path.getsize(OUTPUT_ZIP)
print(f"Successfully created {OUTPUT_ZIP}!")
print(f"Files archived: {count}")
print(f"Uncompressed size: {total_uncompressed / (1024*1024):.2f} MB")
print(f"Compressed ZIP size: {zip_size / (1024*1024):.2f} MB")
