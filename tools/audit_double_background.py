"""Find screens that render their background twice.

1.20.1's `Screen.render` did NOT call `renderBackground`; a screen had to call it
itself.  1.21.1's `Screen.render` (`.refs/nf-src/.../Screen.java:132-138`) calls
it for you, and `renderBackground` runs the menu **blur post effect**
(`Screen.java:382` -> `GameRenderer.processBlurEffect`).  A screen that both
calls `renderBackground` itself AND calls `super.render` therefore blurs
everything it drew in between: the panel, its contents and its text all come out
blurry while anything drawn afterwards (the widgets) stays crisp.

This lists every affected screen and where the two calls are.

Usage:
    python tools/audit_double_background.py
"""

import pathlib
import re

SCREENS = pathlib.Path("src/main/java/top/ribs/scguns/client/screen")

RENDER_BG = re.compile(r"\bthis\.renderBackground\s*\(")
SUPER_RENDER = re.compile(r"\bsuper\.render\s*\(")


def main():
    affected = []
    for path in sorted(SCREENS.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        lines = text.split("\n")
        own = [i + 1 for i, line in enumerate(lines) if RENDER_BG.search(line)]
        sup = [i + 1 for i, line in enumerate(lines) if SUPER_RENDER.search(line)]
        if own and sup:
            # Same method body: our call must be before super.render for the blur
            # to land on top of our content.
            affected.append((str(path), own, sup))

    print("screens calling both renderBackground and super.render: %d" % len(affected))
    print("")
    for name, own, sup in affected:
        print("%-58s own=%s super=%s" % (name.split("\\")[-1], own, sup))
    print("")
    print("Fix: keep the explicit call (it is what draws the blurred world and the")
    print("menu backdrop) and render the widgets directly instead of via")
    print("super.render, so the background/blur runs exactly once:")
    print("    for (Renderable renderable : this.renderables) {")
    print("        renderable.render(guiGraphics, mouseX, mouseY, partialTick);")
    print("    }")


if __name__ == "__main__":
    main()
