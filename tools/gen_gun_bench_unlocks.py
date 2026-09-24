"""Generate gun bench recipe-unlock advancements (HANDOFF section 39).

The gun bench's recipe book only shows recipes the player has unlocked, and nothing unlocked the mod's
own 146 gun bench recipes - so the panel would open empty. Vanilla unlocks a recipe from an
advancement, so each gun bench recipe gets one whose trigger is "the player has that recipe's
blueprint" - which matches the mod's own progression, since blueprints are tiered items.

Four recipes have no blueprint: the four turrets (auto/basic/shotgun/sniper). Their own key item is the
turret platform, which they already require as their `gun_grip` ingredient, so they unlock from holding
that instead. They used to unlock on the first tick, which made the platform pointless and gave the
player four recipes they had never earned (HANDOFF section 43).
"""
import json
import pathlib

ROOT = pathlib.Path(r'E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\resources\data\scguns')
RECIPES = ROOT / 'recipe'
OUT = ROOT / 'advancement' / 'recipes'

# The unlock key for a gun bench recipe that has no blueprint.
NO_BLUEPRINT_KEY = 'scguns:turret_platform'

written = 0
by_key = {}
for path in sorted(RECIPES.rglob('*.json')):
    data = json.loads(path.read_text(encoding='utf-8'))
    if data.get('type') != 'scguns:gun_bench':
        continue

    rel = path.relative_to(RECIPES).with_suffix('')
    recipe_id = 'scguns:' + rel.as_posix()

    blueprint = data.get('ingredients', {}).get('blueprint', {})
    key_item = blueprint.get('item', NO_BLUEPRINT_KEY)
    criteria = {
        'has_the_recipe': {
            'conditions': {'recipe': recipe_id},
            'trigger': 'minecraft:recipe_unlocked',
        },
        'has_blueprint' if 'item' in blueprint else 'has_turret_platform': {
            'conditions': {'items': [{'items': [key_item]}]},
            'trigger': 'minecraft:inventory_changed',
        },
    }
    requirements = [['has_the_recipe', 'has_blueprint' if 'item' in blueprint else 'has_turret_platform']]

    target = OUT / rel.with_suffix('.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({
        'parent': 'minecraft:recipes/root',
        'criteria': criteria,
        'requirements': requirements,
        'rewards': {'recipes': [recipe_id]},
    }, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    written += 1
    by_key[key_item] = by_key.get(key_item, 0) + 1

print('wrote %d gun bench unlock advancement(s) to %s' % (written, OUT))
for key in sorted(by_key, key=lambda k: (-by_key[k], k)):
    print('   %-32s %d' % (key, by_key[key]))
