#!/usr/bin/env python3
"""Remove the temporary pose diagnostic now that the pose is confirmed in game.

Drops client/render/PoseDiagnostics.java and the two call sites in
ItemInHandLayerMixin / PlayerModelMixin. Blocks are located by their exact
content (not by line number) and each removed line is verified first, so the
rest of every file stays byte-identical.

Usage:
    python tools/remove_pose_diagnostics.py            # dry run
    python tools/remove_pose_diagnostics.py --apply
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns')

BLOCKS = {
    os.path.join(SRC, 'mixin', 'client', 'ItemInHandLayerMixin.java'): [
        '         if (stack.getItem() instanceof GunItem gunItemx) {',
        '            // TEMPORARY diagnostic: proves this branch actually runs in game.',
        '            PoseDiagnostics.once(',
        '               "layer:" + stack.getItem() + ":" + display,',
        '               "ItemInHandLayer took the gun branch for display=" + display + " hand=" + hand + " item=" + stack.getItem()',
        '            );',
        '         } else {',
        '            PoseDiagnostics.once(',
        '               "layer-none:" + display,',
        '               "ItemInHandLayer saw a non-gun player item for display=" + display + " item=" + stack.getItem()',
        '            );',
        '         }',
    ],
    os.path.join(SRC, 'mixin', 'client', 'PlayerModelMixin.java'): [
        'import top.ribs.scguns.client.render.PoseDiagnostics;',
        '            // TEMPORARY diagnostic: reports whether this pose hook runs at all and',
        '            // what it is working with. Remove with the class PoseDiagnostics.',
        '            float diagAim = AimingHandler.get().getAimProgress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));',
        '            PoseDiagnostics.once(',
        '               "model:" + (player.isLocalPlayer() ? "local" : "remote") + ":"',
        '                  + Minecraft.getInstance().options.getCameraType() + ":" + (animationPos == 0.0F),',
        '               "gun pose hook: local=" + player.isLocalPlayer()',
        '                  + " camera=" + Minecraft.getInstance().options.getCameraType()',
        '                  + " firstPerson=" + Minecraft.getInstance().options.getCameraType().isFirstPerson()',
        '                  + " limbSwing=" + animationPos',
        '                  + " aimProgress=" + diagAim',
        '                  + " grip=" + gunItem.getModifiedGun(heldItem).determineGripType(heldItem).id()',
        '                  + " xRotBefore=" + model.rightArm.xRot',
        '            );',
    ],
    os.path.join(SRC, 'client', 'render', 'PoseDiagnostics.java'): None,  # whole file
}


def remove_lines(path, wanted, apply):
    with open(path, 'r', encoding='utf-8') as fh:
        lines = [l.rstrip('\r\n') for l in fh.read().splitlines(keepends=True)]
    todo = list(wanted)
    drop = []
    i = 0
    while i < len(lines) and todo:
        if lines[i] == todo[0]:
            # the leading import and the code block are separate runs
            run = 0
            while run < len(todo) and i + run < len(lines) and lines[i + run] == todo[run]:
                run += 1
            if run == len(todo):
                drop.extend(range(i, i + run))
                todo = []
                break
            drop.extend(range(i, i + 1))
            todo.pop(0)
        i += 1
    if todo:
        return 'could not locate %d line(s), first missing: %r' % (len(todo), todo[0])
    with open(path, 'r', encoding='utf-8') as fh:
        original = fh.read().splitlines(keepends=True)
    kept = [l for j, l in enumerate(original, 1) if (j - 1) not in set(drop)]
    if apply:
        with open(path, 'w', encoding='utf-8', newline='') as fh:
            fh.write(''.join(kept))
    return 'removed %d line(s)' % len(drop)


def main():
    apply = '--apply' in sys.argv
    for path, wanted in BLOCKS.items():
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        if wanted is None:
            print('DELETE %s%s' % (rel, ' (missing)' if not os.path.exists(path) else ''))
            if apply and os.path.exists(path):
                os.remove(path)
            continue
        if not os.path.exists(path):
            print('SKIP   %s (file missing)' % rel)
            continue
        result = remove_lines(path, wanted, apply)
        print('EDIT   %s: %s' % (rel, result))
    print('done%s' % (' (applied)' if apply else ' (dry run)'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
