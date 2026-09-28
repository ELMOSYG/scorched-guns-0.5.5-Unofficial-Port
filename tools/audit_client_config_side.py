"""Client-only config options must not be read raw from code that a dedicated server runs.

This is the bug behind the player's report that gunner mobs never reload (HANDOFF section 81), and it was
reproduced on a dedicated server: a mob fired, `AIGunEvent.performGunAttack` read
`Config.CLIENT.display.fireLights`, NeoForge threw

    IllegalStateException: Cannot get config value before config is loaded.

and because that read sits **before** `GunAttackGoal.consumeAmmo`, the magazine never went down - so the AI
never reached its reload branch - and the server died with "Ticking entity". Forge 1.20.1 returned the
default for an unloaded spec, so 0.5.5 could read a client option from server code harmlessly; NeoForge
21.1 throws, and that difference is what turned a latent "wrong config" into a crash.

Three sites had already been patched **individually** (`TemporaryLightManager` and `ProjectileEntity`
caught the IllegalStateException, `ProjectileEntity.onWaterImpact` defaulted to true) which is exactly why
the other six were missed. The rule here is therefore the general one:

  * a file under `client/` may read `Config.CLIENT` directly - it only ever runs on a client;
  * anywhere else, the read must go through `Config.clientOr(...)`, which returns the option's default
    when the client spec is not loaded (what Forge used to do);
  * and no file outside `client/` may catch `IllegalStateException` around such a read: that ad-hoc
    workaround is what hid the remaining sites, and catching it also hides every other state error in the
    same block.

Usage:
    python tools/audit_client_config_side.py
    python tools/audit_client_config_side.py --selftest   # must FAIL against the pre-fix revision
"""

import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path("src/main/java")
PACKAGE = ROOT / "top/ribs/scguns"
RELATIVE_PACKAGE = "src/main/java/top/ribs/scguns"
CONFIG = PACKAGE / "Config.java"

# The revision this was written against: HANDOFF section 80, where six server-reachable sites still read
# the client config raw. A fixed id, not HEAD.
PRE_FIX_REVISION = "d950b34"


def is_client_only(relative):
    """Files that only ever run on a client: the client package, and client mixins."""
    return "/client/" in relative or relative.startswith("client/")


def strip_comments(text):
    """Blank out comments while keeping their newlines, so line numbers stay usable.

    The comments in this codebase quote the broken pattern on purpose - the first version of this audit
    flagged a comment that explained the removed `catch (IllegalStateException)` as if it were the catch
    itself.
    """
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i:i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                if text[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)
