"""
Raksha Ecosystem Final — ZIP Creator
Creates the unified Raksha-Ecosystem-Final.zip containing:
1. RakshaVideo/ (Independent Android Project & APK source)
2. RakshaCall/ (Independent Android Project & APK source)
3. backend/ (Shared Raksha Backend, API, WebSockets, Risk Engine)
4. APK/ (RakshaVideo-debug.apk + RakshaCall-debug.apk)
5. documentation/ (ARCHITECTURE.md, INTEGRATION.md, BUILD_GUIDE.md, DEPLOYMENT.md, DEMO_GUIDE.md)
"""
import os
import zipfile
import hashlib

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
ZIP_NAME = "Raksha-Ecosystem-Final.zip"
ZIP_PATH = os.path.join(PROJECT_ROOT, ZIP_NAME)

EXCLUDE_DIRS = {
    '.gradle', 'build', 'test_clean_venv', '.pytest_cache',
    '__pycache__', 'node_modules', '.git', '.idea', '.vscode',
    '.gemini', 'captures', '.cxx', '.transforms',
}

EXCLUDE_FILES = {
    ZIP_NAME,
    'local.properties',
    'rakshacall.db',
    'RakshaCall_FINAL_FULL_PROJECT.zip',
    'RakshaVideo-Final.zip',
    'RakshaCall_FINAL_OVERALL_PROJECT.zip',
    'RakshaCall_FINAL_REALTIME_PROTOTYPE.zip',
    'RakshaCall_Overall_Project_Step3.zip',
    'RakshaCall-Full-Project.zip'
}

EXCLUDE_EXTS = {
    '.pyc', '.pyo', '.class', '.o', '.so', '.dll',
}

SECTIONS = {
    'RakshaVideo': os.path.join(PROJECT_ROOT, 'RakshaVideo'),
    'RakshaCall': os.path.join(PROJECT_ROOT, 'RakshaCall'),
    'backend': os.path.join(PROJECT_ROOT, 'backend'),
    'APK': os.path.join(PROJECT_ROOT, 'APK'),
    'documentation': os.path.join(PROJECT_ROOT, 'documentation')
}

def should_exclude(rel_path: str, is_dir: bool = False) -> bool:
    parts = rel_path.replace('\\', '/').split('/')
    for part in parts:
        if part in EXCLUDE_DIRS:
            return True
    if is_dir:
        return False
    basename = os.path.basename(rel_path)
    if basename in EXCLUDE_FILES:
        return True
    _, ext = os.path.splitext(basename)
    if ext.lower() in EXCLUDE_EXTS:
        return True
    if basename == '.env':
        return True
    return False

def compute_sha256(filepath: str) -> str:
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        for chunk in iter(lambda: f.read(65536), b''):
            h.update(chunk)
    return h.hexdigest().upper()

def main():
    print(f"Creating Ecosystem ZIP: {ZIP_PATH}")
    if os.path.exists(ZIP_PATH):
        os.remove(ZIP_PATH)
        print(f"Removed previous {ZIP_NAME}")

    file_count = 0
    dir_set = set()

    with zipfile.ZipFile(ZIP_PATH, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for section_name, section_path in SECTIONS.items():
            if not os.path.exists(section_path):
                print(f"WARNING: Section {section_name} does not exist at {section_path}")
                continue

            for dirpath, dirnames, filenames in os.walk(section_path):
                rel_dir = os.path.relpath(dirpath, section_path)
                if rel_dir == '.':
                    rel_dir = ''

                dirnames[:] = [d for d in dirnames if not should_exclude(os.path.join(rel_dir, d) if rel_dir else d, is_dir=True)]

                for filename in sorted(filenames):
                    rel_file = os.path.join(rel_dir, filename) if rel_dir else filename
                    if should_exclude(rel_file):
                        continue

                    abs_file = os.path.join(dirpath, filename)
                    arcname = f"{section_name}/{rel_file}".replace('\\', '/')

                    try:
                        zf.write(abs_file, arcname)
                        file_count += 1
                        parts = arcname.split('/')
                        for i in range(1, len(parts)):
                            dir_set.add('/'.join(parts[:i]))
                    except Exception as e:
                        print(f"  WARNING: Could not add {arcname}: {e}")

    zip_size = os.path.getsize(ZIP_PATH)
    zip_hash = compute_sha256(ZIP_PATH)

    print()
    print("=" * 60)
    print("RAKSHA ECOSYSTEM FINAL ZIP CREATED SUCCESSFULLY")
    print("=" * 60)
    print(f"  File: {ZIP_NAME}")
    print(f"  Files: {file_count}")
    print(f"  Directories: {len(dir_set)}")
    print(f"  Size: {zip_size:,} bytes ({zip_size / (1024*1024):.1f} MB)")
    print(f"  SHA-256: {zip_hash}")
    print()

    # Structure Verification
    with zipfile.ZipFile(ZIP_PATH, 'r') as zf:
        all_names = zf.namelist()
        top_sections = sorted(set(n.split('/')[0] for n in all_names))
        print(f"TOP-LEVEL SECTIONS: {top_sections}")
        for s in ['RakshaVideo', 'RakshaCall', 'backend', 'APK', 'documentation']:
            count = sum(1 for n in all_names if n.startswith(s + '/'))
            print(f"  {s}/: {count} files")

if __name__ == '__main__':
    main()
