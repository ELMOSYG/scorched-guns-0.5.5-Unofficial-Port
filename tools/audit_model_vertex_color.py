"""Every model that accepts a vertex colour must actually pass it to its parts.

1.21.1 moved the colour into the model render entry point:
`renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, int color)` with a
matching five argument `ModelPart.render(...)`. A model that accepts the colour and then calls
the four argument `render(...)` compiles, renders the mob, and silently ignores the tint.

That is how the sulfurhead's translucent gel layer broke: it asked for alpha 0.4 through
`FastColor.ARGB32.colorFromFloat(0.4F, 1F, 1F, 0.3F)`, the model dropped it, and the shell came
out opaque white - reported as "the translucent material does not render". All 21 ported models
had the same hole.

Usage:
    python tools/audit_model_vertex_color.py
"""

import pathlib
import re
import sys

CLIENT = pathlib.Path("src/main/java/top/ribs/scguns/entity/client")
SIGNATURE = re.compile(r'(?:public|protected)\s+void\s+renderToBuffer\s*\(([^)]*)\)\s*\{', re.S)
CALL = re.compile(r'\.render\(([^;()]*(?:\([^()]*\)[^;()]*)*)\)\s*;')
FOUR_ARGS = 4
FIVE_ARGS = 5


def method_body(text, open_brace):
    depth, i = 0, open_brace
    while i < len(text):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return text[open_brace + 1:i]
        i += 1
    return ""


def depth_aware_split(args):
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


def check(text):
    """-> (problems, checked call count)"""
    problems, checked = [], 0
    for match in SIGNATURE.finditer(text):
        params = match.group(1)
        body = method_body(text, match.end() - 1)
        for call in CALL.finditer(body):
            args = depth_aware_split(call.group(1))
            if len(args) == FOUR_ARGS:
                problems.append("%s dropped the colour: %s"
                                % (params.count("int color") and "renderToBuffer" or "method",
                                   call.group(0).strip()))
            elif len(args) == FIVE_ARGS:
                checked += 1
    return problems, checked


def main():
    problems, checked, files = [], 0, 0
    for path in sorted(CLIENT.glob("*Model.java")):
        found, calls = check(path.read_text(encoding="utf-8"))
        files += 1
        checked += calls
        for problem in found:
            problems.append("%s: %s" % (path.name, problem))

    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d model(s) ignore the vertex colour they are given "
              "(translucent/tinted layers will render untinted)" % len(problems))
        return 1
    print("0 problem(s): %d part render call(s) in %d model(s) pass the vertex colour"
          % (checked, files))
    return 0


if __name__ == "__main__":
    sys.exit(main())
