"""Tripwire for the frame of the physics-structure impulse.

`PhysicsStructureHelper.applyPunchAt` has to hand Sable the impact point and direction in the
STRUCTURE'S OWN frame. Sable's punch handler builds exactly that pair
(`localPosition = pose.transformPositionInverse(player.position())`,
`localDirection = pose.transformNormalInverse(direction)`) and passes those two to
`PhysicsPipeline.applyImpulse`, so that is the frame the pipeline reads - its coordinates are
plot coordinates, in the tens of millions.

Getting this wrong twice already, both times silently:
  * a world point for the mass query made `getInverseNormalMass` return 3.4e11, so the shot
    did nothing at all ("no effect");
  * a world point for the impulse itself left the linear push correct but made the lever arm
    ~2e7 long, and the spin that produces is limited only by the body's own inertia - the
    reported symptom was a structure thrown to y=14022.

So this audit fails when the impulse call is given anything but the locally converted pair.

Since HANDOFF 26 the force is applied where the shooter stands (Sable's punch applies it at the
player's own position, verified with javap), so the local pair is built from the *shooter's*
position rather than the impact point; the impact point is only used to locate the structure.
The point is still converted into the structure's frame, which is what this audit guards.
"""

import pathlib
import re
import sys

SOURCE = pathlib.Path("src/main/java/top/ribs/scguns/util/PhysicsStructureHelper.java")

# Anything that looks like a world-space name must not reach the impulse call.
BAD_POINT_NAMES = ("worldHitPoint", "worldForcePoint", "hitPoint", "hitVec", "getLocation")

# Names that are known to hold an already-converted, structure-local value.
LOCAL_NAMES = ("local", "unitImpulse")


def check(text):
    """-> list of problem descriptions (empty when the frame handling looks right)."""
    problems = []

    if "transformPositionInverse" not in text:
        problems.append("no transformPositionInverse: the point is never converted to the "
                        "structure's frame")
    if "transformNormalInverse" not in text:
        problems.append("no transformNormalInverse: the direction is never converted to the "
                        "structure's frame")

    calls = [(m.start(), m.group(0)) for m in re.finditer(r'applyImpulseAtPoint\(([^;]*)\)', text)]
    if not calls:
        problems.append("no applyImpulseAtPoint call found")
        return problems

    for _, call in calls:
        args = call[len("applyImpulseAtPoint("):-1]
        point, _, rest = args.partition(',')
        point = point.strip()
        if not point.startswith(LOCAL_NAMES):
            problems.append("impulse point is %r, not a structure-local value" % point)
        for bad in BAD_POINT_NAMES:
            if bad in args:
                problems.append("impulse call still mentions the world-space name %r" % bad)
        if not rest.strip().startswith(LOCAL_NAMES):
            problems.append("impulse direction is %r, not a structure-local value"
                            % rest.strip())

    # The mass query has to use the same local pair (Sable's computeStrengthScalar does).
    mass = re.search(r'getInverseNormalMass\(([^)]*)\)', text)
    if mass and not mass.group(1).strip().startswith(LOCAL_NAMES):
        problems.append("getInverseNormalMass is asked with %r, not the local pair"
                        % mass.group(1).strip())

    return problems


def main():
    if not SOURCE.exists():
        print("FAIL: %s is missing" % SOURCE)
        return 1

    problems = check(SOURCE.read_text(encoding="utf-8"))
    for problem in problems:
        print("  %s" % problem)
    if problems:
        print("%d problem(s) with the physics-structure impulse frame" % len(problems))
        return 1
    print("0 problem(s): the impulse point and direction are converted into the structure's frame")
    return 0


if __name__ == "__main__":
    sys.exit(main())
