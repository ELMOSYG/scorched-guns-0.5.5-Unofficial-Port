"""Forty-fourth-stage: prepare the excluded JEI 19 classes so they compile the
moment the `build.gradle` excludes are removed.

Two things are provably wrong as written (verified against
`libs/jei-1.21.1-neoforge-19.27.0.340.jar` and the 1.21.1 sources, see
build-logs/JEI19_NOTES.md):

  * `super.draw(...)` inside a class whose only superclass is `Object` is not
    valid Java - an interface default method must be qualified as
    `IRecipeCategory.super.draw(...)`. The 0.5.5 decompile lost that qualifier.
  * `RecipeManager#getAllRecipesFor` returns `List<RecipeHolder<T>>` in 1.21,
    while `IRecipeRegistration.addRecipes` wants `List<T>`.

`IRecipeCategory#getBackground` and the `GuiGraphics.renderTooltip` overload the
categories use were both checked and still exist in JEI 19 / 1.21.1, so they are
deliberately left alone.

Idempotent: re-running must report 0 changes.
`python tools/port_rewrite44.py --selftest` exercises both rules.

usage: python tools/port_rewrite44.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
COMPAT = os.path.join(ROOT, "src", "main", "java", "top", "ribs", "scguns", "compat")
SUFFIX = ".stream().map(RecipeHolder::value).toList()"

SUPER_DRAW = re.compile(r"(?<![\w.])super\.draw\(")
GET_ALL = re.compile(r"\.getAllRecipesFor\(")


def fix_super_draw(text: str, stats: dict) -> str:
    text, n = SUPER_DRAW.subn("IRecipeCategory.super.draw(", text)
    if n:
        stats["IRecipeCategory.super.draw"] = stats.get("IRecipeCategory.super.draw", 0) + n
    return text


def fix_get_all_recipes(text: str, stats: dict) -> str:
    out = text
    pos = 0
    count = 0
    while True:
        m = GET_ALL.search(out, pos)
        if not m:
            break
        open_paren = m.end() - 1
        close = match_forward(out, open_paren)
        if close < 0:
            break
        if out[close + 1:close + 1 + len(SUFFIX)] == SUFFIX:
            pos = close + 1  # already unwrapped
            continue
        out = out[:close + 1] + SUFFIX + out[close + 1:]
        pos = close + 1 + len(SUFFIX)
        count += 1
    if count:
        stats["getAllRecipesFor unwrap"] = stats.get("getAllRecipesFor unwrap", 0) + count
        out = ensure_import(out, "net.minecraft.world.item.crafting.RecipeHolder")
    return out


def process(text: str, stats: dict) -> str:
    return fix_get_all_recipes(fix_super_draw(text, stats), stats)


def main() -> None:
    total: dict = {}
    changed = 0
    for name in sorted(os.listdir(COMPAT)):
        if not name.endswith(".java"):
            continue
        path = os.path.join(COMPAT, name)
        text = open(path, encoding="utf-8", errors="replace").read()
        new = process(text, total)
        if new != text:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
    print(f"rewrote {changed} files")
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print(f"{v:6d}  {k}")


SELFTEST = [
    ("      super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);\n",
     "      IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);\n"),
    ("      List<GunBenchRecipe> r = recipeManager.getAllRecipesFor(GunBenchRecipe.Type.INSTANCE);\n",
     "      List<GunBenchRecipe> r = recipeManager.getAllRecipesFor(GunBenchRecipe.Type.INSTANCE)"
     ".stream().map(RecipeHolder::value).toList();\n"),
    ("      registration.addRecipes(T, recipeManager.getAllRecipesFor(LightningBatteryRecipe.Type.INSTANCE));\n",
     "      registration.addRecipes(T, recipeManager.getAllRecipesFor(LightningBatteryRecipe.Type.INSTANCE)"
     ".stream().map(RecipeHolder::value).toList());\n"),
]


def selftest() -> None:
    for text, expected in SELFTEST:
        got = fix_get_all_recipes(fix_super_draw(text, {}), {}).replace(
            "import net.minecraft.world.item.crafting.RecipeHolder;\n\n", "")
        status = "ok  " if got == expected else "FAIL"
        print(f"{status} {got.strip()}")
        assert got == expected, "expected %r got %r" % (expected, got)
        again = fix_get_all_recipes(fix_super_draw(got, {}), {}).replace(
            "import net.minecraft.world.item.crafting.RecipeHolder;\n\n", "")
        assert again == got, "not idempotent: %r" % again
    print("port_rewrite44 self-test passed (and is idempotent)")


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        selftest()
    else:
        main()
