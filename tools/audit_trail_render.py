"""Audit the bullet-trail renderers: one hook per frame, one batch per frame, no per-trail rebuilds.

HANDOFF 82.12.  Every shotgun fires up to 26 pellets at once and each pellet leaves
a trail that lives for `maxAge` client ticks, so the trail render path is entered 26
times per frame while a blast is in flight.  Three defects compounded there and no
audit noticed any of them:

  * `BulletTrailRenderingHandler` subscribed to `RenderLevelStageEvent.AFTER_PARTICLES`
    *and* was called from `LevelRendererMixin` - the same trail geometry drawn twice per
    frame.  Both call sites hand the renderer an identity PoseStack (vanilla's level
    stack is a plain `new PoseStack()` and the camera position is subtracted per
    entity), so the second draw was pure waste rather than a visible offset.
  * `BulletTrailRenderingHandler.get()` was registered on `NeoForge.EVENT_BUS` twice
    (ScorchedGuns and ClientHandler), so `onClientTick` also ran twice per tick and
    every trail aged twice as fast; one audit printed this as a note and exited 0, the
    other did not recognise the `.get()` form at all.
  * `RenderType.energySwirl()` builds a new RenderType *and* its CompositeState on every
    call, `getTexture()` formatted and parsed a ResourceLocation on every call, and the
    render loop ended the batch inside the loop - per trail, per frame, i.e. 26 render
    types and 26 draw calls for a single blast.  The turret trail renderer, next to it,
    already batched all of its trails into one call.

The rules below are the ones that would have caught all three.

Usage:
    python tools/audit_trail_render.py
    python tools/audit_trail_render.py --selftest   # must FAIL against the pre-fix revision
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java")
HANDLERS = os.path.join(SRC, "top", "ribs", "scguns", "client", "handler")
MIXIN = "top/ribs/scguns/mixin/client/LevelRendererMixin.java"
TRAIL_SUFFIX = "TrailRenderingHandler"

PRE_FIX_REVISION = "5b53ff4"

RE_COMMENT = re.compile(r"//[^\n]*")
RE_FOR_TRAIL_LOOP = re.compile(r"for\s*\([^)]*:\s*this\.(?:bullets|trails)\.values\(\)\s*\)")
RE_RENDER_TYPE_BUILD = re.compile(r"RenderType\.(?:energySwirl|create)\s*\(")
RE_TEXTURE_PARSE = re.compile(r"ResourceLocation\.parse\s*\(")
RE_CALL = re.compile(r"(?:this\.)?(\w+)\s*\(")
KEYWORDS = {"if", "for", "while", "switch", "return", "new", "catch", "synchronized", "super", "else"}


def strip_comments(text: str) -> str:
    """Code only: comments in these files deliberately *name* the patterns we forbid.

    A plain `/*.*?*/` regex is not enough here and this is not hypothetical: a comment in
    ScorchedGuns documents a resource glob as `.../*.json`, so the regex opened a block
    comment there and blanked out the next hundred lines - including the very
    `EVENT_BUS.register(BulletTrailRenderingHandler.get())` line this audit exists to see.
    This scanner tracks line comments, block comments and string literals in one pass and
    preserves both offsets and line numbers.
    """
    out = []
    index, length = 0, len(text)
    state = None
    while index < length:
        char = text[index]
        following = text[index + 1] if index + 1 < length else ""
        if state is None:
            if char == "/" and following == "/":
                state = "line"
                out.append("  ")
                index += 2
                continue
            if char == "/" and following == "*":
                state = "block"
                out.append("  ")
                index += 2
                continue
            if char == '"':
                state = "string"
            out.append(char)
            index += 1
            continue
        if state == "line":
            if char == "\n":
                state = None
                out.append(char)
            else:
                out.append(" ")
            index += 1
            continue
        if state == "block":
            if char == "*" and following == "/":
                state = None
                out.append("  ")
                index += 2
                continue
            out.append("\n" if char == "\n" else " ")
            index += 1
            continue
        # inside a string literal
        if char == "\\" and following:
            out.append(char)
            out.append(following)
            index += 2
            continue
        if char == '"':
            state = None
        out.append(char)
        index += 1
    return "".join(out)


def match_forward(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    return -1


def method_body(text: str, name: str):
    """Brace-matched body of the first method called `name`, as (body, start)."""
    for m in re.finditer(r"\b" + re.escape(name) + r"\s*\(", text):
        open_brace = text.find("{", m.end())
        if open_brace < 0:
            continue
        semi = text.find(";", m.end())
        if 0 <= semi < open_brace:
            continue  # an abstract / interface declaration
        close = match_forward(text, open_brace)
        if close < 0:
            continue
        return text[open_brace + 1:close], open_brace
    return None, -1


def simple_name(rel_path: str) -> str:
    return os.path.basename(rel_path)[:-5]


def called_methods(body: str) -> set:
    """Names of the methods invoked in `body` (used to follow a loop into its callees)."""
    names = set()
    for match in RE_CALL.finditer(body):
        name = match.group(1)
        if name not in KEYWORDS:
            names.add(name)
    return names


def loop_body_with_callees(code: str, loop_match) -> str:
    """The trail loop's body, plus the bodies of the same-class methods it calls.

    The flush that made a shotgun cost 26 draw calls per frame was not textually inside
    the loop - the loop called `renderBulletTrail(...)`, which ended the batch. Following
    the calls (three levels deep is far more than any renderer here needs) is what makes
    the rule see it.
    """
    body = loop_match.string
    open_brace = body.find("{", loop_match.end())
    close = match_forward(body, open_brace) if open_brace >= 0 else -1
    collected = body[open_brace:close] if close > 0 else body[loop_match.end():]
    pending = called_methods(collected)
    seen = set()
    for _ in range(3):
        following = set()
        for name in pending - seen:
            seen.add(name)
            sub, _ = method_body(code, name)
            if sub is None:
                continue
            collected += "\n" + sub
            following |= called_methods(sub)
        pending = following
    return collected


def check(files: dict) -> list:
    """`files` maps a path relative to src/main/java to its text."""
    problems = []

    handlers = {p: t for p, t in files.items()
                if os.path.dirname(p).endswith("client/handler") and simple_name(p).endswith(TRAIL_SUFFIX)}
    if not handlers:
        return ["no *TrailRenderingHandler classes found - the audit is looking in the wrong place"]

    mixin = files.get(MIXIN)
    if mixin is None:
        return ["%s is missing: it is the only hook that draws trails" % MIXIN]
    mixin_code = strip_comments(mixin)

    for path, raw in sorted(handlers.items()):
        name = simple_name(path)
        code = strip_comments(raw)

        # 1. Exactly one thing must drive this renderer each frame.
        from_event = bool(re.search(r"@SubscribeEvent[^)]*\)\s*[^;{]*\b\w+\s*\([^)]*RenderLevelStageEvent",
                                    code, re.S)) or (
            "@SubscribeEvent" in code and bool(re.search(r"\b\w+\s*\([^)]*RenderLevelStageEvent", code)))
        from_mixin = re.search(re.escape(name) + r"\.get\(\)\.render\s*\(", mixin_code) is not None
        if from_event and from_mixin:
            problems.append("%s: renders through both RenderLevelStageEvent and LevelRendererMixin, so every "
                            "trail is drawn twice per frame" % name)
        elif not from_event and not from_mixin:
            problems.append("%s: nothing calls render() - no mixin call and no stage subscription" % name)

        # 2. Registered at most once, so its per-tick work runs once per tick.
        registrations = []
        for other, other_raw in files.items():
            for m in re.finditer(r"EVENT_BUS\.register\s*\(\s*" + re.escape(name) + r"\s*\.\s*(get\(\)|class)\s*\)",
                                 strip_comments(other_raw)):
                registrations.append("%s (%s)" % (other, m.group(1)))
        if len(registrations) > 1:
            problems.append("%s: registered on the event bus %d times (%s) - every @SubscribeEvent method in it "
                            "runs once per registration" % (name, len(registrations), ", ".join(registrations)))

        # 3. The batch must be ended once per frame, not once per trail.
        body, _ = method_body(code, "render")
        if body is not None:
            loop = RE_FOR_TRAIL_LOOP.search(body)
            if loop is not None:
                loop_text = loop_body_with_callees(code, loop)
                if "endBatch(" in loop_text:
                    problems.append("%s: ends the batch inside (or under) the per-trail loop, so a 26-pellet blast "
                                    "costs 26 draw calls per frame instead of one" % name)
            if body.count("endBatch(") > 1:
                problems.append("%s.render(): ends the batch %d times per frame"
                                % (name, body.count("endBatch(")))

        # 4. No RenderType may be built in a per-frame method.
        for m in re.finditer(r"\b(?:public|private|protected)\s+[\w<>\[\], .]+\s+(\w+)\s*\(", code):
            method = m.group(1)
            if not method.startswith("render"):
                continue
            method_text, _ = method_body(code, method)
            if method_text is None:
                continue
            if RE_RENDER_TYPE_BUILD.search(method_text) and "Cache" not in method:
                problems.append("%s.%s(): builds a RenderType per call; RenderType.energySwirl() allocates a new "
                                "RenderType and CompositeState every time" % (name, method))
            if RE_TEXTURE_PARSE.search(method_text):
                problems.append("%s.%s(): parses a ResourceLocation per call instead of caching the trail texture"
                                % (name, method))

        # 5. The texture lookup itself must be cached.
        texture_body, _ = method_body(code, "getTexture")
        if texture_body is not None and (".get(" not in texture_body or ".put(" not in texture_body):
            problems.append("%s.getTexture(): does not consult a cache, so the texture path is formatted and "
                            "parsed once per trail per frame" % name)

    return problems


def current_files() -> dict:
    files = {}
    for directory, _, names in os.walk(SRC):
        for name in names:
            if not name.endswith(".java"):
                continue
            path = os.path.join(directory, name)
            rel = os.path.relpath(path, SRC).replace("\\", "/")
            files[rel] = open(path, encoding="utf-8", errors="replace").read()
    return files


def git_files(revision: str) -> dict:
    listing = subprocess.run(["git", "ls-tree", "-r", "--name-only", revision, "src/main/java"],
                             capture_output=True, text=True, encoding="utf-8", check=True).stdout.split()
    files = {}
    for full in listing:
        if not full.endswith(".java"):
            continue
        rel = full[len("src/main/java/"):]
        files[rel] = subprocess.run(["git", "show", "%s:%s" % (revision, full)],
                                    capture_output=True, text=True, encoding="utf-8", check=True).stdout
    return files


def selftest() -> int:
    try:
        files = git_files(PRE_FIX_REVISION)
    except (subprocess.CalledProcessError, FileNotFoundError) as error:
        print("selftest: cannot read revision %s (%s)" % (PRE_FIX_REVISION, error))
        return 1

    found = check(files)
    print("selftest: revision %s reports %d problem(s)" % (PRE_FIX_REVISION, len(found)))
    for problem in found:
        print("   %s" % problem)

    expected = [
        "renders through both RenderLevelStageEvent and LevelRendererMixin",
        "registered on the event bus 2 times",
        "ends the batch inside (or under) the per-trail loop",
        "builds a RenderType per call",
        "does not consult a cache",
    ]
    missing = [e for e in expected if not any(e in f for f in found)]
    if missing:
        for entry in missing:
            print("selftest MISSING: %s" % entry)
        print("selftest FAILED: the audit does not notice the double render hook, the duplicate "
              "registration or the per-trail render cost")
        return 1
    print("selftest OK: the double hook, the duplicate registration and every per-trail rebuild are detected")
    return 0


def main() -> int:
    if "--selftest" in sys.argv:
        return selftest()

    problems = check(current_files())
    print("=== trail render hooks ===")
    print("  driven by LevelRendererMixin only, one batch per frame, cached RenderTypes")
    if problems:
        print("\n%d problem(s):" % len(problems))
        for problem in problems:
            print("  %s" % problem)
        return 1
    print("\n0 problem(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
