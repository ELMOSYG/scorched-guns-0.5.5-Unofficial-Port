"""Make every ported model pass the vertex colour on to its parts.

1.21.1 changed the model render entry point to `renderToBuffer(poseStack, vertexConsumer,
packedLight, packedOverlay, int color)` and `ModelPart.render` gained a five argument form
that applies that colour. The port converted the signatures but not the bodies, so each model
still called `part.render(poseStack, vertexConsumer, packedLight, packedOverlay)` - which
renders with plain white and full alpha.

Consequence: any layer that tints the model lost its tint. The sulfurhead's gel layer asks for
alpha 0.4 and got 1.0 (its translucent shell rendered as opaque white), and its primed
overlay lost its pulse colour. Reported as "the translucent material does not render".

This rewrites the calls inside every `renderToBuffer(..., int color)` body to pass `color`.
Run `tools/audit_model_vertex_color.py` afterwards; it is the check that keeps this fixed.

Usage:
    python tools/patch_model_vertex_color.py [--dry-run]
"""

import pathlib
import re
import sys

CLIENT = pathlib.Path("src/main/java/top/ribs/scguns/entity/client")
SIGNATURE = re.compile(r'(?:public|protected)\s+void\s+renderToBuffer\s*\(([^)]*)\)\s*\{',
                       re.S)
CALL = re.compile(r'\.render\(([^;()]*(?:\([^()]*\)[^;()]*)*)\)\s*;')


def method_body(text, open_brace):
    """Returns (start, end) of the brace-delimited body starting at open_brace."""
    depth = 0
    i = open_brace
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return open_brace + 1, i
        i += 1
    raise ValueError("unbalanced braces")


def split_args(args):
    """Splits on commas that are not inside parentheses."""
    out, depth, current = [], 0, ""
    for ch in args:
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


def patch(text):
    """-> (new text, number of calls fixed)"""
    fixed = 0
    # Work on the last match first so earlier offsets stay valid.
    for match in reversed(list(SIGNATURE.finditer(text))):
        if "int color" not in match.group(1):
            continue
        start, end = method_body(text, match.end() - 1)
        body = text[start:end]

        def repl(call):
            nonlocal fixed
            args = split_args(call.group(1))
            if len(args) == 4:
                fixed += 1
                return '.render(%s, color);' % ', '.join(args)
            return call.group(0)

        text = text[:start] + CALL.sub(repl, body) + text[end:]
    return text, fixed


def main():
    dry_run = "--dry-run" in sys.argv
    total, files = 0, 0
    for path in sorted(CLIENT.glob("*Model.java")):
        original = path.read_text(encoding="utf-8")
        patched, fixed = patch(original)
        if not fixed:
            continue
        files += 1
        total += fixed
        print("  %-28s %d call(s)" % (path.name, fixed))
        if not dry_run:
            path.write_text(patched, encoding="utf-8")
    print("%d call(s) in %d model file(s)%s"
          % (total, files, " (dry run, nothing written)" if dry_run else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
