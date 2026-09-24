"""Check that the gun bench's recipe-book placement actually reaches the blueprint slot.

Why this exists: the ghost projection and the automatic placement are both driven by vanilla's
PlaceRecipe.placeRecipe, which walks a gridWidth x gridHeight grid and hands each ingredient the
*menu slot* it belongs to - counting from 0 and stepping over the result slot:

    int k1 = 0;
    for (int k = 0; k < height; k++) {
        if (k1 == outputSlot) k1++;          // step over the result slot
        ... this.addItemToSlot(ingredients.next(), k1, ...); k1++;
    }

and the client draws that ghost on `menu.slots.get(p_slot)`, while the server places into the same
slot. So the recipe's ingredient order and the menu's slot order have to agree, and the grid has to
be tall enough to reach the blueprint's slot. That agreement is arithmetic, and this script checks
it against the real constants in GunBenchMenu/GunBenchRecipe instead of trusting a comment.

Usage:
    python tools/check_gun_bench_layout.py
"""

import pathlib
import re
import sys

MENU = pathlib.Path("src/main/java/top/ribs/scguns/client/screen/GunBenchMenu.java")
RECIPE = pathlib.Path("src/main/java/top/ribs/scguns/client/screen/GunBenchRecipe.java")


def constant(text, name):
    match = re.search(r"int\s+%s\s*=\s*(\d+)\s*;" % re.escape(name), text)
    if not match:
        raise SystemExit("could not find %s" % name)
    return int(match.group(1))


def simulate_place_recipe(width, height, output_slot, ingredient_count):
    """Vanilla PlaceRecipe.placeRecipe, ported verbatim for the non-shaped case (i == width)."""
    slots = []
    k1 = 0
    for k in range(height):
        if k1 == output_slot:
            k1 += 1

        for i1 in range(width):
            if len(slots) >= ingredient_count:
                return slots
            # i == width and j == height for a non-shaped recipe, so no centring is applied.
            slots.append(k1)
            k1 += 1

    return slots


def main():
    menu = MENU.read_text(encoding="utf-8")
    recipe = RECIPE.read_text(encoding="utf-8")

    grid_size = constant(menu, "GRID_SIZE")
    menu_slot_output = constant(menu, "MENU_SLOT_OUTPUT")
    menu_slot_blueprint = constant(menu, "MENU_SLOT_BLUEPRINT")

    if not re.search(r"getGridHeight\(\)\s*\{\s*return\s+GRID_SIZE\s*\+\s*1\s*;", menu):
        print("FAIL: getGridHeight() no longer returns GRID_SIZE + 1")
        return 1

    if not re.search(r"getSize\(\)\s*\{\s*return\s+GRID_SIZE\s*\+\s*1\s*;", menu):
        print("FAIL: getSize() no longer returns GRID_SIZE + 1")
        return 1

    # The blueprint has to be the last entry of the combined input list, i.e. index GRID_SIZE.
    if "all.set(recipeItems.size(), blueprint)" not in recipe:
        print("FAIL: the blueprint is no longer appended to the recipe's input list")
        return 1

    if "return this.inputs;" not in recipe:
        print("FAIL: getIngredients() no longer returns the combined input list")
        return 1

    height = grid_size + 1
    ingredient_count = grid_size + 1
    mapping = simulate_place_recipe(1, height, menu_slot_output, ingredient_count)

    print("bench: %d attachment slots, output at menu slot %d, blueprint at menu slot %d"
          % (grid_size, menu_slot_output, menu_slot_blueprint))
    print("grid:  1 x %d inputs, so ServerPlaceRecipe walks %d + 1 = %d slots"
          % (height, height, height + 1))
    print("ghost: ingredient index -> menu slot %s" % mapping)

    blueprint_slot = mapping[grid_size]
    if blueprint_slot != menu_slot_blueprint:
        print("FAIL: the blueprint ingredient lands on slot %d, not slot %d"
              % (blueprint_slot, menu_slot_blueprint))
        return 1

    if mapping[0] != menu_slot_output + 1:
        print("FAIL: the first attachment ingredient lands on slot %d, not %d"
              % (mapping[0], menu_slot_output + 1))
        return 1

    # ServerPlaceRecipe clears gridWidth * gridHeight + 1 slots; those must be the bench's own slots.
    if height + 1 != menu_slot_blueprint + 1:
        print("FAIL: %d cleared slots do not line up with the blueprint at slot %d"
              % (height + 1, menu_slot_blueprint))
        return 1

    print("OK: a book click projects and fills every attachment slot and the blueprint slot")
    return 0


if __name__ == "__main__":
    sys.exit(main())
