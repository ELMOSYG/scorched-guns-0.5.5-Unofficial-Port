"""Port the removed 1.20.1 BlockBehaviour#use(...) overrides to the 1.21.1 split.

1.21.1 replaced the single block interaction hook with two:

    protected ItemInteractionResult useItemOn(ItemStack, BlockState, Level, BlockPos, Player,
                                              InteractionHand, BlockHitResult)
    protected InteractionResult    useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)

The port kept the 1.20.1 method, which no longer exists anywhere in BlockBehaviour (verified with
javap against both the vanilla 1.21.1 jar and the NeoForge merged jar). Because it was never
annotated with @Override it still compiled - as a brand new method nobody calls - so every one of
these blocks silently stopped responding to right click: the gun bench, the macerator and its
powered variant, the mechanical press and its powered variant, the polar generator, the mine unit,
the memorial, the charged amethyst relay, the composter, the acid cauldron, the guano candle and
the sandbag. That is the "the mod's work blocks cannot be interacted with" report.

This rewrites each one onto the hook it belongs to:
  * a body that reads the held item becomes useItemOn (the item is taken as the new first
    parameter, and the InteractionResult values are mapped to ItemInteractionResult);
  * a body that does not touch the hand becomes useWithoutItem.
Both get @Override, so javac now fails if the signature is ever wrong again instead of silently
compiling a dead method.

Usage:
    python tools/patch_block_use.py [--dry-run]
"""

import pathlib
import re
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/block")
SIGNATURE = re.compile(r'(?P<indent>[ \t]*)public InteractionResult use\((?P<params>[^)]*)\)\s*\{')

# InteractionResult -> ItemInteractionResult, for the bodies that become useItemOn.
RESULT_MAP = {
    "InteractionResult.SUCCESS": "ItemInteractionResult.SUCCESS",
    "InteractionResult.CONSUME_PARTIAL": "ItemInteractionResult.CONSUME_PARTIAL",
    "InteractionResult.CONSUME": "ItemInteractionResult.CONSUME",
    "InteractionResult.FAIL": "ItemInteractionResult.FAIL",
    "InteractionResult.PASS": "ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION",
    "InteractionResult.sidedSuccess": "ItemInteractionResult.sidedSuccess",
    "InteractionResult.SUCCESS_NO_ITEM_USED": "ItemInteractionResult.SUCCESS",
}


def split_args(text):
    out, depth, current = [], 0, ""
    for ch in text:
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(current.strip())
            current = ""
        else:
            current += ch
    if current.strip():
        out.append(current.strip())
    return out


def body_range(text, open_brace):
    depth, i = 0, open_brace
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return open_brace, i
        i += 1
    raise ValueError("unbalanced braces")


def convert(text):
    """-> (new text, list of (block-ish name, hook))"""
    done = []
    for match in reversed(list(SIGNATURE.finditer(text))):
        params = split_args(match.group("params"))
        start, end = body_range(text, match.end() - 1)
        body = text[start + 1:end]
        names = {}
        for param in params:
            bits = param.replace("@NotNull", "").split()
            if len(bits) >= 2:
                names[bits[-2].split('.')[-1]] = bits[-1]

        hand = names.get("InteractionHand")
        uses_hand = bool(hand) and re.search(r'\b%s\b' % re.escape(hand), body)
        if uses_hand:
            hook = "useItemOn"
            new_sig = ("%spublic ItemInteractionResult useItemOn(ItemStack stack, %s, %s %s, %s %s, %s %s, %s %s, %s %s)"
                       % (match.group("indent"), params[0], params[1].split()[0], names["Level"],
                          params[2].split()[0], names["BlockPos"], params[3].split()[0], names["Player"],
                          params[4].split()[0], hand, params[5].split()[0], names["BlockHitResult"]))
            # the item is now a parameter; the hand lookup disappears
            body = body.replace("player.getItemInHand(%s)" % hand, "stack")
            body = body.replace("player.getItemInHand(%s)" % hand, "stack")
            for old, new in RESULT_MAP.items():
                body = body.replace(old, new)
        else:
            hook = "useWithoutItem"
            new_sig = ("%spublic InteractionResult useWithoutItem(%s, %s %s, %s %s, %s %s, %s %s)"
                       % (match.group("indent"), params[0], params[1].split()[0], names["Level"],
                          params[2].split()[0], names["BlockPos"], params[3].split()[0], names["Player"],
                          params[5].split()[0], names["BlockHitResult"]))

        replacement = "%s@Override\n%s {\n%s}" % (match.group("indent"), new_sig, body)
        text = text[:match.start()] + replacement + text[end + 1:]
        done.append((hook, new_sig.strip()[:70]))
    return text, done


def main():
    dry_run = "--dry-run" in sys.argv
    total = 0
    for path in sorted(SOURCE.rglob("*.java")):
        original = path.read_text(encoding="utf-8")
        if not SIGNATURE.search(original):
            continue
        patched, done = convert(original)
        total += len(done)
        print("  %-34s %s" % (path.name, ", ".join(hook for hook, _ in done)))
        if not dry_run:
            path.write_text(patched, encoding="utf-8", newline="")
    print("%d override(s)%s" % (total, " (dry run, nothing written)" if dry_run else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
