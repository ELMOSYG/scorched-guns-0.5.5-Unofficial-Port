"""Every vanilla tool class must be given its attribute modifiers.

1.20.1 passed the modifiers to the constructor:
    new PickaxeItem(Tier, int attackDamage, float attackSpeed, Properties)
1.21 moved them into the item's ATTRIBUTE_MODIFIERS component:
    new Item.Properties().attributes(PickaxeItem.createAttributes(Tier, damage, speed))

A tool registered with a bare `new Properties()` still compiles and still mines, but it has no
attack damage or attack speed modifiers at all - it hits like an empty hand. That is what the
anthralite pickaxe, sword, axe, shovel and hoe were doing; the mod's own tool classes
(WaraxeItem, CogMaceItem, ...) had already been converted, the plain vanilla ones had not.

Usage:
    python tools/audit_item_attributes.py
"""

import pathlib
import re
import sys

MOD_ITEMS = pathlib.Path("src/main/java/top/ribs/scguns/init/ModItems.java")
VANILLA_TOOLS = ("PickaxeItem", "AxeItem", "ShovelItem", "HoeItem", "SwordItem")
REGISTER = re.compile(r'REGISTER\.register\(')


def registration_blocks(text):
    """-> list of (line number, id, body) for every REGISTER.register(...) call."""
    out = []
    for match in REGISTER.finditer(text):
        depth, i, start = 0, match.end() - 1, match.end() - 1
        while i < len(text):
            if text[i] == '(':
                depth += 1
            elif text[i] == ')':
                depth -= 1
                if depth == 0:
                    break
            i += 1
        body = text[start:i]
        id_match = re.search(r'"([a-z0-9_]+)"', body)
        out.append((text.count("\n", 0, match.start()) + 1,
                    id_match.group(1) if id_match else "?", body))
    return out


def check(text):
    """-> list of problem descriptions."""
    problems = []
    for line, item_id, body in registration_blocks(text):
        for tool in VANILLA_TOOLS:
            if "new %s(" % tool not in body:
                continue
            if ".attributes(" not in body:
                problems.append("line %d: %s creates %s with no attributes"
                                % (line, item_id, tool))
    return problems


def main():
    if not MOD_ITEMS.exists():
        print("FAIL: %s is missing" % MOD_ITEMS)
        return 1
    problems = check(MOD_ITEMS.read_text(encoding="utf-8"))
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d tool item(s) would have no attribute modifiers (they hit like an empty hand)"
              % len(problems))
        return 1
    print("0 problem(s): every vanilla tool registration sets attribute modifiers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
