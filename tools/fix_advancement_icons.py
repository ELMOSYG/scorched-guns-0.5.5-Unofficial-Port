#!/usr/bin/env python3
"""Migrate data/scguns/advancement/**.json icons from the 1.20.1 form to 1.21.1.

1.20.1: "icon": {"item": "scguns:musket"}
1.21.1: "icon": {"count": 1, "id": "scguns:musket"}

DisplayInfo uses ItemStack.STRICT_CODEC in 1.21.1, whose item field is "id".
The 1.20.1 "item" key is unknown to that codec, so the icon fails to parse and
every advancement that carries a display silently disappears from the
advancement tree (the root fails first, which removes the whole tab).

The edit is a surgical text splice on the single "icon" object per file so the
rest of the file stays byte-identical (these files are not plain
sort_keys dumps: "parent" comes first when present).

Without --apply nothing is written; the script only reports.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ADV_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'scguns', 'advancement')

ICON_KEY = re.compile(r'"icon"\s*:\s*\{')


def find_icon_span(text):
    """Return (start, end) of the "icon": {...} value object, braces included."""
    m = ICON_KEY.search(text)
    if m is None:
        return None
    start = text.index('{', m.start())
    depth = 0
    in_str = False
    esc = False
    for i in range(start, len(text)):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == '\\':
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return start, i + 1
    return None


def main():
    apply = '--apply' in sys.argv
    changed = []
    already = 0
    skipped = []
    for base, _dirs, files in os.walk(ADV_DIR):
        for name in sorted(files):
            if not name.endswith('.json'):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            with open(path, 'r', encoding='utf-8') as fh:
                text = fh.read()

            if text.count('"icon"') != 1:
                skipped.append('%s: %d "icon" keys' % (rel, text.count('"icon"')))
                continue

            span = find_icon_span(text)
            if span is None:
                skipped.append('%s: could not locate icon object' % rel)
                continue
            start, end = span
            try:
                obj = json.loads(text[start:end])
            except Exception as exc:  # noqa: BLE001
                skipped.append('%s: icon is not valid JSON: %s' % (rel, exc))
                continue
            if not isinstance(obj, dict):
                skipped.append('%s: icon is not an object' % rel)
                continue
            if 'id' in obj:
                already += 1
                continue
            item = obj.get('item')
            if item is None:
                skipped.append('%s: icon has neither item nor id' % rel)
                continue
            extra = set(obj) - {'item', 'count'}
            if extra:
                skipped.append('%s: icon has unexpected keys %s' % (rel, sorted(extra)))
                continue

            # indentation of the "icon" line, then one level deeper for members
            line_start = text.rfind('\n', 0, start) + 1
            key_indent = re.match(r'[ \t]*', text[line_start:start]).group(0)
            mem_indent = key_indent + '  '
            replacement = '{\n%s"count": %d,\n%s"id": "%s"\n%s}' % (
                mem_indent, int(obj.get('count', 1)), mem_indent, item, key_indent)
            new_text = text[:start] + replacement + text[end:]
            changed.append(rel)
            if apply:
                with open(path, 'w', encoding='utf-8', newline='') as fh:
                    fh.write(new_text)

    print('changed: %d' % len(changed))
    print('already migrated: %d' % already)
    print('skipped: %d' % len(skipped))
    for s in skipped:
        print('  ' + s)
    return 0


if __name__ == '__main__':
    sys.exit(main())
