"""Rewrite `scguns:*_mod_loaded` datapack conditions into `neoforge:mod_loaded`.

WHY THIS EXISTS
---------------
0.5.5 (1.20.1/Forge) gated cross-mod recipes with four custom conditions:

    scguns:create_mod_loaded         compat/CreateModCondition
    scguns:farmersdelight_mod_loaded compat/FarmersDelightModCondition
    scguns:ie_mod_loaded             compat/IEModCondition
    scguns:soul_fired_mod_loaded     compat/SoulFiredModCondition

All four were plain `ModList.get().isLoaded("<modid>")` checks, and all four
classes were dropped during the 1.21.1 port. NeoForge 1.21.1 ships an
equivalent built-in condition (`neoforge:mod_loaded` + `modid`), so every
occurrence is rewritten to that form. Without this, the affected recipe files
fail to load at datapack load time (unknown condition type).

This is lossless: `scguns:create_mod_loaded` was literally
`ModList.get().isLoaded("create")`.

Re-run this whenever `tools/convert_resources.py` regenerates resources from
the 0.5.5 jar, because the conversion reintroduces the old condition names.
The script is idempotent and reports how many files it changed.
"""
import json
import os
import re
import sys

ROOT = os.path.join('src', 'main', 'resources')

# old condition type -> modid that the deleted condition class tested for
MOD_LOADED_CONDITIONS = {
    'scguns:create_mod_loaded': 'create',
    'scguns:farmersdelight_mod_loaded': 'farmersdelight',
    'scguns:ie_mod_loaded': 'immersiveengineering',
    'scguns:soul_fired_mod_loaded': 'soul_fired',
}

# Preserve the surrounding document style: these files are 2-space indented JSON,
# so a textual rewrite keeps the diff minimal and avoids reformatting whole files.
PATTERN = re.compile(
    r'\{\s*"type"\s*:\s*"(?P<type>' + '|'.join(re.escape(k) for k in MOD_LOADED_CONDITIONS) + r')"\s*\}'
)


def main():
    changed = []
    for dirpath, _, filenames in os.walk(ROOT):
        for filename in filenames:
            if not filename.endswith('.json'):
                continue
            full = os.path.join(dirpath, filename)
            try:
                with open(full, encoding='utf-8') as handle:
                    text = handle.read()
            except OSError:
                continue
            if not any(key in text for key in MOD_LOADED_CONDITIONS):
                continue

            def replace(match):
                modid = MOD_LOADED_CONDITIONS[match.group('type')]
                return '{\n        "type": "neoforge:mod_loaded",\n        "modid": "%s"\n      }' % modid

            new_text = PATTERN.sub(replace, text)
            if new_text == text:
                print(f'  !! no substitution made in {full} (unexpected formatting, fix by hand)')
                continue
            # validate before writing
            try:
                json.loads(new_text)
            except ValueError as exc:
                print(f'  !! refusing to write invalid json {full}: {exc}')
                continue
            with open(full, 'w', encoding='utf-8', newline='\n') as handle:
                handle.write(new_text)
            changed.append(full)

    print(f'fixed {len(changed)} file(s)')
    for path in changed:
        print('   ', path)
    return 0


if __name__ == '__main__':
    sys.exit(main())
