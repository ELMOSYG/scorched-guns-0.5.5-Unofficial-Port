#!/usr/bin/env python3
"""Analyze data/scguns/advancement/**.json for 1.21.1 schema drift.

Reports, per file:
  * icon shape ("item" key is a 1.20.1 leftover; 1.21.1 ItemStack.STRICT_CODEC needs "id")
  * legacy condition keys that no longer exist in 1.21.1 codecs (tag/nbt/potion/...)
  * trigger ids used
  * parent chain validity
  
Also verifies every display.title/description translate key exists in a lang file.
"""
import json
import os
import sys
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ADV_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'scguns', 'advancement')
LANG_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'scguns', 'lang')

# 1.20.1-era keys removed from the 1.21.1 codecs.
# ItemPredicate (1.21.1) fields: items, count, components, predicates
LEGACY_KEYS = {'tag', 'nbt', 'potion', 'items_tag', 'block', 'blocks', 'entity_tag', 'type_specific'}
# ItemPredicate (1.21.1) real fields
ITEM_PREDICATE_KEYS = {'items', 'count', 'components', 'predicates'}

# DisplayInfo.CODEC fields, and the icon's ItemStack codec fields. An unknown key is
# ignored by RecordCodecBuilder, but a *missing required* one (icon/title/description)
# or a wrong-typed one makes DisplayInfo fail -- and a failing `display` does NOT drop
# the advancement: 1.21 loads it with no display at all, so it silently never shows up
# in the advancement screen (verified with tools/probe_advancement_icon.py).
DISPLAY_KEYS = {'icon', 'title', 'description', 'background', 'frame',
                'show_toast', 'announce_to_chat', 'hidden'}
ICON_KEYS = {'id', 'count', 'components'}
FRAMES = {'task', 'goal', 'challenge'}


def iter_files():
    for base, _dirs, files in os.walk(ADV_DIR):
        for f in sorted(files):
            if f.endswith('.json'):
                yield os.path.join(base, f)


def adv_id(path):
    rel = os.path.relpath(path, ADV_DIR).replace('\\', '/')
    return 'scguns:' + rel[:-len('.json')]


def walk(node, path, on_kv):
    if isinstance(node, dict):
        for k, v in node.items():
            on_kv(k, v, path)
            walk(v, path + '/' + k, on_kv)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, '%s[%d]' % (path, i), on_kv)


def main():
    problems = []
    triggers = Counter()
    icons = Counter()
    legacy_hits = []
    ids = {}
    parents = {}
    titles = set()
    predicate_keys = Counter()

    for path in iter_files():
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        try:
            with open(path, 'r', encoding='utf-8') as fh:
                data = json.load(fh)
        except Exception as exc:  # noqa: BLE001
            problems.append('%s: invalid JSON: %s' % (rel, exc))
            continue
        aid = adv_id(path)
        ids[aid] = rel
        parents[aid] = data.get('parent')

        # icon shape
        display = data.get('display')
        if display is None:
            problems.append('%s: no display -> invisible in the advancement screen' % rel)
            display = {}
        if not isinstance(display, dict):
            problems.append('%s: display is not an object -> DisplayInfo fails and the '
                            'advancement loads with no display' % rel)
            display = {}
        for key in sorted(set(display) - DISPLAY_KEYS):
            problems.append('%s: unknown display key "%s"' % (rel, key))
        if 'title' not in display or 'description' not in display:
            problems.append('%s: display needs both title and description' % rel)
        if 'frame' in display and display['frame'] not in FRAMES:
            problems.append('%s: display.frame "%s" is not one of %s'
                            % (rel, display['frame'], sorted(FRAMES)))

        icon = display.get('icon')
        if icon is None:
            problems.append('%s: display without icon' % rel)
        elif not isinstance(icon, dict):
            problems.append('%s: icon is not an object -> DisplayInfo fails' % rel)
        else:
            keys = tuple(sorted(icon.keys()))
            icons[keys] += 1
            for key in sorted(set(icon) - ICON_KEYS):
                problems.append('%s: unknown icon key "%s"' % (rel, key))
            if 'item' in icon:
                problems.append('%s: icon uses 1.20.1 key "item" (1.21.1 needs "id")' % rel)
            if 'id' not in icon:
                problems.append('%s: icon has no "id"' % rel)

        # triggers + legacy keys
        for cname, crit in (data.get('criteria') or {}).items():
            if not isinstance(crit, dict):
                problems.append('%s: criterion %s is not an object' % (rel, cname))
                continue
            trig = crit.get('trigger')
            triggers[trig] += 1
            if not trig:
                problems.append('%s: criterion %s has no trigger' % (rel, cname))

        def on_kv(k, _v, kpath):
            titles.add(k) if False else None
            if k in LEGACY_KEYS:
                legacy_hits.append('%s: legacy key "%s" at %s' % (rel, k, kpath))
            if '/conditions/' in kpath and k in ('items', 'count', 'components', 'predicates'):
                predicate_keys[k] += 1

        walk(data.get('criteria') or {}, 'criteria', on_kv)

        display = data.get('display') or {}
        for field in ('title', 'description'):
            comp = display.get(field)
            if isinstance(comp, dict) and 'translate' in comp:
                titles.add(comp['translate'])
            elif isinstance(comp, str):
                problems.append('%s: display.%s is a bare string' % (rel, field))

        # requirement/criteria cross-check
        criteria = set((data.get('criteria') or {}).keys())
        if not criteria:
            problems.append('%s: empty criteria' % rel)
        for group in data.get('requirements') or []:
            for name in group:
                if name not in criteria:
                    problems.append('%s: requirement "%s" is not a criterion' % (rel, name))

    # parent chain
    for aid, parent in parents.items():
        if parent and parent not in ids:
            problems.append('%s: parent "%s" does not exist' % (ids[aid], parent))

    roots = [a for a, p in parents.items() if not p]

    # lang keys
    lang = {}
    for name in os.listdir(LANG_DIR):
        if name.endswith('.json'):
            with open(os.path.join(LANG_DIR, name), 'r', encoding='utf-8') as fh:
                lang[name] = set(json.load(fh).keys())
    missing_lang = defaultdict(list)
    for key in sorted(titles):
        for name, keys in lang.items():
            if key not in keys:
                missing_lang[name].append(key)

    print('files: %d' % len(ids))
    print('roots: %s' % sorted(roots))
    print('icon shapes: %s' % dict(icons))
    print('triggers: %s' % dict(triggers))
    print('item-predicate keys: %s' % dict(predicate_keys))
    print('legacy-key hits: %d' % len(legacy_hits))
    print('translate keys: %d' % len(titles))
    for name, keys in sorted(missing_lang.items()):
        if keys:
            print('missing in %s: %d -> %s' % (name, len(keys), keys[:12]))
    print('problems: %d' % len(problems))
    for p in problems:
        print('  ' + p)
    return 0


if __name__ == '__main__':
    sys.exit(main())
