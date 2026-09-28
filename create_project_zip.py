"""
RakshaCall — Proper Project ZIP Creator
Preserves full directory structure with correct relative paths.
Excludes build artifacts, caches, venvs, and .git.
"""
import os
import zipfile
import hashlib
import sys

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
ZIP_NAME = "RakshaCall_FINAL_FULL_PROJECT.zip"
ZIP_PATH = os.path.join(PROJECT_ROOT, ZIP_NAME)

# Directories and files to exclude
EXCLUDE_DIRS = {
    '.gradle', 'build', 'test_clean_venv', '.pytest_cache',
    '__pycache__', 'node_modules', '.git', '.idea', '.vscode',
    '.gemini', 'captures', '.cxx', '.transforms',
}

EXCLUDE_FILES = {
    ZIP_NAME,               # Don't include ourselves
    'create_zip.ps1',       # Broken script
    'local.properties',     # Developer-specific
    'rakshacall.db',        # Runtime SQLite DB
}

EXCLUDE_EXTENSIONS = {
    '.pyc', '.pyo', '.class', '.o', '.so', '.dll', '.zip',
}

def should_exclude(rel_path: str, is_dir: bool = False) -> bool:
    """Check if a path should be excluded from the ZIP."""
    parts = rel_path.replace('\\', '/').split('/')
    
    # Check directory exclusions
    for part in parts:
        if part in EXCLUDE_DIRS:
            return True
    
    if is_dir:
        return False
    
    # Check file exclusions
    basename = os.path.basename(rel_path)
    if basename in EXCLUDE_FILES:
        return True
    
    # Check extension exclusions
    _, ext = os.path.splitext(basename)
    if ext.lower() in EXCLUDE_EXTENSIONS:
        return True
    
    # Exclude .env files that might contain secrets (but keep .env.example)
    if basename == '.env':
        return True
    
    return False

def compute_sha256(filepath: str) -> str:
    """Compute SHA-256 hash of a file."""
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        for chunk in iter(lambda: f.read(8192), b''):
            h.update(chunk)
    return h.hexdigest().upper()

def main():
    print(f"Creating ZIP: {ZIP_PATH}")
    print(f"Project root: {PROJECT_ROOT}")
    print()
    
    # Remove existing ZIP
    if os.path.exists(ZIP_PATH):
        os.remove(ZIP_PATH)
        print(f"Removed old {ZIP_NAME}")
    
    file_count = 0
    dir_set = set()
    
    with zipfile.ZipFile(ZIP_PATH, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for dirpath, dirnames, filenames in os.walk(PROJECT_ROOT):
            # Compute relative path from project root
            rel_dir = os.path.relpath(dirpath, PROJECT_ROOT)
            if rel_dir == '.':
                rel_dir = ''
            
            # Filter out excluded directories (modifying dirnames in-place prevents os.walk from descending)
            dirnames[:] = [d for d in dirnames if not should_exclude(os.path.join(rel_dir, d) if rel_dir else d, is_dir=True)]
            
            for filename in sorted(filenames):
                rel_file = os.path.join(rel_dir, filename) if rel_dir else filename
                
                if should_exclude(rel_file):
                    continue
                
                abs_file = os.path.join(dirpath, filename)
                
                # Use forward slashes in ZIP for cross-platform compatibility
                arcname = rel_file.replace('\\', '/')
                
                try:
                    zf.write(abs_file, arcname)
                    file_count += 1
                    
                    # Track directories
                    parts = arcname.split('/')
                    for i in range(1, len(parts)):
                        dir_set.add('/'.join(parts[:i]))
                except Exception as e:
                    print(f"  WARNING: Could not add {rel_file}: {e}")
    
    # Verify the ZIP
    zip_size = os.path.getsize(ZIP_PATH)
    zip_hash = compute_sha256(ZIP_PATH)
    
    print()
    print(f"{'='*60}")
    print(f"ZIP CREATED SUCCESSFULLY")
    print(f"{'='*60}")
    print(f"  File: {ZIP_NAME}")
    print(f"  Files: {file_count}")
    print(f"  Directories: {len(dir_set)}")
    print(f"  Size: {zip_size:,} bytes ({zip_size / (1024*1024):.1f} MB)")
    print(f"  SHA-256: {zip_hash}")
    print()
    
    # Verify structure by listing top-level entries
    with zipfile.ZipFile(ZIP_PATH, 'r') as zf:
        top_level = set()
        all_names = zf.namelist()
        for name in all_names:
            top = name.split('/')[0]
            top_level.add(top)
        
        init_count = sum(1 for n in all_names if n.endswith('__init__.py'))
        
        print(f"STRUCTURE VERIFICATION:")
        print(f"  Top-level entries: {sorted(top_level)}")
        print(f"  __init__.py count: {init_count}")
        print(f"  Total entries: {len(all_names)}")
        
        # Check critical directories exist
        critical_dirs = ['app', 'backend', 'ml', 'docs', 'tests', 'proto']
        for d in critical_dirs:
            found = any(n.startswith(d + '/') for n in all_names)
            status = "FOUND" if found else "MISSING"
            print(f"  {d}/: {status}")
    
    print()
    print("DONE.")

if __name__ == '__main__':
    main()
