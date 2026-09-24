"""Forty-sixth-stage: `FriendlyByteBuf#writeItem` was removed in 1.21.

1.20.1 wrote an item through the buffer (`buffer.writeItem(stack)`); 1.21 moved
that to the codec, `ItemStack.STREAM_CODEC`, which is typed
`StreamCodec<RegistryFriendlyByteBuf, ItemStack>` - exactly the buffer the
recipe serialisers already use. The matching reader is
`ItemStack.STREAM_CODEC.decode(buffer)`.

Idempotent; `--selftest` checks the rule and the round trip.

usage: python tools/port_rewrite46.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")
WRITE_ITEM = re.compile(r"(\w+)\.writeItem\(")


def process(text: str, stats: dict) -> str:
    out = text
    pos = 0
    count = 0
    while True:
        m = WRITE_ITEM.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        args = out[open_paren + 1:close]
        new = "ItemStack.STREAM_CODEC.encode(%s, %s)" % (m.group(1), args)
        out = out[:m.start()] + new + out[close + 1:]
        pos = m.start() + len(new)
        count += 1
    if count:
        stats["writeItem -> ItemStack.STREAM_CODEC"] = \
            stats.get("writeItem -> ItemStack.STREAM_CODEC", 0) + count
    return out


def main() -> None:
    total: dict = {}
    changed = 0
    files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith(".java")]
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        new = process(text, total)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(new)
            changed += 1
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


SELFTEST = [
    ("buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));",
     "ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(RegistryAccess.EMPTY));"),
    ("      p_123_.writeItem(ItemStack.EMPTY);",
     "      ItemStack.STREAM_CODEC.encode(p_123_, ItemStack.EMPTY);"),
]


def selftest() -> None:
    for text, expected in SELFTEST:
        got = process(text, {})
        print("%s %s" % ("ok  " if got == expected else "FAIL", got))
        assert got == expected, "expected %r got %r" % (expected, got)
        assert process(got, {}) == got, "not idempotent: %r" % got
    print("port_rewrite46 self-test passed (and is idempotent)")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
