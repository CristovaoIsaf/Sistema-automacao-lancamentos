from pathlib import Path
import os
root = Path('.').resolve()
for path in root.rglob('*'):
    if not path.is_file():
        continue
    if path.suffix.lower() not in {'.ts', '.tsx', '.js', '.jsx', '.css', '.json', '.mjs'}:
        continue
    try:
        data = path.read_bytes()
    except Exception:
        continue
    for i, b in enumerate(data):
        if b < 32 and b not in (9, 10, 13):
            print(path.relative_to(root), 'control', hex(b), 'at', i)
            break
