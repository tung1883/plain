"""Builds the Sandbox language packs (c.zip, js.zip) from the folders next to this script.

    python sandbox-packs/build.py            build zips into sandbox-packs/dist/ and print the constants
                                             to paste into PackStore.java
    python sandbox-packs/build.py c=2      also publish pack c as release sandbox-pack-c-v2 (gh)

Bump a pack's VERSION in PackStore.java whenever its zip changes: the version is both the
release tag suffix and the installed-marker the app compares against.
"""
import hashlib
import os
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(HERE, "dist")
PACKS = ["c", "js"]


def build(pack):
    root = os.path.join(HERE, pack)
    out = os.path.join(DIST, pack + ".zip")
    raw = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for base, _dirs, files in os.walk(root):
            for f in sorted(files):
                full = os.path.join(base, f)
                rel = os.path.relpath(full, root).replace(os.sep, "/")
                z.write(full, rel)
                raw += os.path.getsize(full)
    sha = hashlib.sha256(open(out, "rb").read()).hexdigest()
    return out, os.path.getsize(out), raw, sha


def main():
    os.makedirs(DIST, exist_ok=True)
    results = {}
    for pack in PACKS:
        results[pack] = build(pack)
        out, size, raw, sha = results[pack]
        print(f'{pack}: zip={size} raw={raw} sha256={sha}')
    for arg in sys.argv[1:]:
        # --release c=2 js=1: publish those packs as tag sandbox-pack-<id>-v<version>
        if "=" in arg:
            pack, version = arg.split("=")
            subprocess.run(["gh", "release", "create", f"sandbox-pack-{pack}-v{version}", results[pack][0],
                            "--repo", "tung1883/plain", "--title", f"Sandbox pack {pack} v{version}",
                            "--notes", f"Sandbox language data pack ({pack}). Downloaded on demand by the app."],
                           check=True)


if __name__ == "__main__":
    main()
