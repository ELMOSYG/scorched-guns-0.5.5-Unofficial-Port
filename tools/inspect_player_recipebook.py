"""Read a Minecraft playerdata file and report the gun bench recipes it has unlocked.

Why this exists: the player reported "the gun bench only shows 9 gun recipes" while the recipe
book log proves the book itself is wired correctly. The book draws only *unlocked* recipes, and
unlocks live in the player's saved `recipeBook` tag, so the question is answerable offline - no
game round, no guessing (HANDOFF section 39.7).

Usage:
   python tools/inspect_player_recipebook.py "<path to ...\\playerdata\\<uuid>.dat>"

Read-only: parses gzip-compressed NBT with the standard library only.
"""

import gzip
import json
import pathlib
import struct
import sys

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class Reader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def take(self, n):
        chunk = self.data[self.pos:self.pos + n]
        if len(chunk) != n:
            raise EOFError("truncated NBT at offset %d" % self.pos)
        self.pos += n
        return chunk

    def unpack(self, fmt, size):
        return struct.unpack(fmt, self.take(size))[0]

    def byte(self):
        return self.unpack(">b", 1)

    def short(self):
        return self.unpack(">h", 2)

    def ushort(self):
        return self.unpack(">H", 2)

    def int(self):
        return self.unpack(">i", 4)

    def long(self):
        return self.unpack(">q", 8)

    def float(self):
        return self.unpack(">f", 4)

    def double(self):
        return self.unpack(">d", 8)

    def string(self):
        return self.take(self.ushort()).decode("utf-8", "replace")

    def payload(self, tag_id):
        if tag_id == TAG_BYTE:
            return self.byte()
        if tag_id == TAG_SHORT:
            return self.short()
        if tag_id == TAG_INT:
            return self.int()
        if tag_id == TAG_LONG:
            return self.long()
        if tag_id == TAG_FLOAT:
            return self.float()
        if tag_id == TAG_DOUBLE:
            return self.double()
        if tag_id == TAG_BYTE_ARRAY:
            return list(self.take(self.int()))
        if tag_id == TAG_STRING:
            return self.string()
        if tag_id == TAG_LIST:
            item_id = self.byte()
            length = self.int()
            return [self.payload(item_id) for _ in range(length)]
        if tag_id == TAG_COMPOUND:
            out = {}
            while True:
                child_id = self.byte()
                if child_id == TAG_END:
                    return out
                name = self.string()
                out[name] = self.payload(child_id)
        if tag_id == TAG_INT_ARRAY:
            return [self.int() for _ in range(self.int())]
        if tag_id == TAG_LONG_ARRAY:
            return [self.long() for _ in range(self.int())]
        raise ValueError("unknown NBT tag id %d at offset %d" % (tag_id, self.pos))


def read_nbt(path):
    with gzip.open(path, "rb") as handle:
        data = handle.read()
    reader = Reader(data)
    root_id = reader.byte()
    if root_id != TAG_COMPOUND:
        raise ValueError("root tag is %d, expected a compound" % root_id)
    reader.string()
    return reader.payload(TAG_COMPOUND)


def gun_bench_tiers(repo):
    """recipe id -> blueprint item id, straight from the shipped recipe data."""
    tiers = {}
    for path in sorted((repo / "src/main/resources/data/scguns/recipe").rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        if recipe.get("type") != "scguns:gun_bench":
            continue
        blueprint = recipe.get("ingredients", {}).get("blueprint", {}).get("item")
        recipe_id = "scguns:%s" % path.stem
        tiers[recipe_id] = blueprint
    return tiers


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2

    repo = pathlib.Path(__file__).resolve().parent.parent
    player = read_nbt(argv[1])
    known = set(player.get("recipeBook", {}).get("recipes", []))
    tiers = gun_bench_tiers(repo)

    print("player file      : %s" % argv[1])
    print("known recipes    : %d" % len(known))
    print("gun bench recipes: %d shipped" % len(tiers))

    unlocked = {rid: bp for rid, bp in tiers.items() if rid in known}
    print("gun bench unlocks: %d" % len(unlocked))

    by_tier = {}
    for rid, bp in tiers.items():
        bucket = by_tier.setdefault(bp, [0, 0])
        bucket[1] += 1
        if rid in unlocked:
            bucket[0] += 1

    for bp in sorted(by_tier, key=lambda key: (key is None, key)):
        got, total = by_tier[bp]
        print("   %-28s %2d / %2d" % (bp if bp else "(no blueprint)", got, total))

    held = []
    for item in player.get("Inventory", []):
        name = item.get("id", "")
        if "blueprint" in name:
            held.append((name, item.get("count", 1)))
    print("blueprints held  : %s" % (held if held else "none"))

    missing = sorted(set(tiers) - set(unlocked))
    print("not unlocked     : %d" % len(missing))
    for rid in missing[:12]:
        print("   %s (%s)" % (rid, tiers[rid]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
