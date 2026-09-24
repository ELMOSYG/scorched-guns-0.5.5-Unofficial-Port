"""Thirty-fourth-stage: 1.21 recipe inputs.

Recipe#matches/assemble and RecipeManager#getRecipeFor now take a RecipeInput
instead of a Container, so the machine recipes get a small adapter and the call
sites wrap their SimpleContainer in it.

usage: python tools/port_rewrite34.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from port_rewrite import ensure_import, match_forward  # noqa: E402

ROOT = r"E:\mod\scgun-0.5.5-1.21.1-neoforge"
SRC = os.path.join(ROOT, "src", "main", "java")

ADAPTER = '''package top.ribs.scguns.common.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 1.21 recipe lookup adapter.
 *
 * <p>Recipes and {@code RecipeManager#getRecipeFor} take a {@link RecipeInput}
 * now; the Scorched Guns machines already own a {@link Container}, so this wraps
 * one without copying the stacks.</p>
 */
public class ContainerRecipeInput implements RecipeInput {
    private final Container container;

    public ContainerRecipeInput(Container container) {
        this.container = container;
    }

    public Container container() {
        return this.container;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.container.getItem(index);
    }

    @Override
    public int size() {
        return this.container.getContainerSize();
    }
}
'''


def main() -> None:
    with open(os.path.join(SRC, "top", "ribs", "scguns", "common", "recipe", "ContainerRecipeInput.java"),
              "w", encoding="utf-8", newline="\n") as fh:
        fh.write(ADAPTER)
    print("wrote ContainerRecipeInput")

    total = {"recipe type": 0, "matches/assemble": 0, "container calls": 0, "call sites": 0}
    for dirpath, _, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith(".java") or fn == "ContainerRecipeInput.java":
                continue
            path = os.path.join(dirpath, fn)
            text = open(path, encoding="utf-8", errors="replace").read()
            original = text

            if "implements Recipe<SimpleContainer>" in text:
                text = text.replace("implements Recipe<SimpleContainer>", "implements Recipe<ContainerRecipeInput>")
                total["recipe type"] += 1
            if "Recipe<ContainerRecipeInput>" in text:
                text, n = re.subn(r"(matches|assemble)\(\s*SimpleContainer\s+", r"\1(ContainerRecipeInput ", text)
                total["matches/assemble"] += n
                text, n = re.subn(r"\b(\w+)\.getContainerSize\(\)", r"\1.size()", text)
                total["container calls"] += n
                text = ensure_import(text, "top.ribs.scguns.common.recipe.ContainerRecipeInput")

            # call sites that hand a SimpleContainer to a recipe lookup
            if "getRecipeFor(" in text or "getAllRecipesFor(" in text:
                def wrap(m: re.Match) -> str:
                    return "%snew ContainerRecipeInput(%s)" % (m.group(1), m.group(2))

                text, n = re.subn(r"(getRecipeFor\([^,]+,\s*)new SimpleContainer\(([^;]*?)\)(?=,)", wrap, text)
                if n:
                    total["call sites"] += n
                    text = ensure_import(text, "top.ribs.scguns.common.recipe.ContainerRecipeInput")
                text, n = re.subn(r"(getRecipeFor\([^,]+,\s*)(\w+)(?=,\s*this\.level)", r"\1new ContainerRecipeInput(\2)", text)
                if n:
                    total["call sites"] += n
                    text = ensure_import(text, "top.ribs.scguns.common.recipe.ContainerRecipeInput")

            if text != original:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(text)
    for k, v in total.items():
        print(f"{v:6d}  {k}")


if __name__ == "__main__":
    main()
