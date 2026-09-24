"""Take the dev server's RCON password out of the source before publishing.

The password is only the locally generated one of a dev world (run/server.properties, which is not in
the repository), so this is hygiene rather than a real secret - but a hardcoded password in a public
repository invites trouble. The value stays the same as a default, so nothing changes for the tools:
it is simply read from `SCGUNS_RCON_PASSWORD` first.

Usage:
    python tools/harden_rcon_password.py            # dry run, prints what would change
    python tools/harden_rcon_password.py --apply    # rewrite
    python tools/harden_rcon_password.py --selftest # prove the rewrite is not a no-op
"""

import pathlib
import re
import sys

TOOLS = pathlib.Path("tools")
LITERAL = '"scgunsverify"'
ENV_EXPR = 'os.environ.get("SCGUNS_RCON_PASSWORD", "scgunsverify")'
IMPORT = "import os\n"

# `password = "scgunsverify"` / `PASSWORD = sys.argv[1] if ... else "scgunsverify"` /
# `Rcon("scgunsverify")` - all of them can simply use the call. The negative lookbehind skips the
# default argument of an env lookup that is already there, which is what makes this idempotent: every
# rewritten site keeps the literal, but always preceded by ", ".
PATTERNS = (
    re.compile(r'(?<!, )(?<!os\.environ\.get\()"scgunsverify"'),
)


def needs_import(text):
    return not re.search(r"^import os$", text, re.M)


def rewrite(text):
    """-> (new text, number of replacements)"""
    replaced = PATTERNS[0].sub(ENV_EXPR, text)
    count = len(PATTERNS[0].findall(text))
    if count and needs_import(replaced):
        # Put it with the other imports, after the module docstring.
        lines = replaced.splitlines(keepends=True)
        for index, line in enumerate(lines):
            if line.startswith("import ") or line.startswith("from "):
                lines.insert(index, IMPORT)
                break
        replaced = "".join(lines)
    return replaced, count


def main():
    apply = "--apply" in sys.argv
    if "--selftest" in sys.argv:
        # The live file is already rewritten, so the pre-fix text has to come from git.
        import subprocess

        try:
            before = subprocess.run(["git", "show", "HEAD:tools/rcon_cmd.py"],
                                    capture_output=True, text=True, check=True).stdout
        except (subprocess.CalledProcessError, FileNotFoundError):
            before = 'password = "scgunsverify"\n'
        after, count = rewrite(before)
        if count == 0:
            print("selftest FAILED: nothing matched - the pattern is broken")
            return 1
        if after.count(LITERAL) != count:
            # The literal survives exactly once per replacement, inside the env lookup; anything more
            # means a call site was missed.
            print("selftest FAILED: expected the literal only inside %d env lookup(s), found %d"
                  % (count, after.count(LITERAL)))
            return 1
        if "import os" not in after:
            print("selftest FAILED: the rewrite did not add the import")
            return 1
        again, second = rewrite(after)
        if second or again != after:
            print("selftest FAILED: not idempotent (%d change(s) on the second run)" % second)
            return 1
        print("selftest OK: %d replacement(s), idempotent, import added" % count)
        return 0

    total = 0
    for path in sorted(TOOLS.glob("rcon_*.py")):
        text = path.read_text(encoding="utf-8")
        updated, count = rewrite(text)
        if not count:
            continue
        total += count
        print("%-38s %d replacement(s)" % (path.name, count))
        if apply:
            path.write_text(updated, encoding="utf-8")
    print("%d replacement(s) %s" % (total, "written" if apply else "(dry run - pass --apply)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
