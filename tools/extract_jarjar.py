"""Extract a jar-in-jar (NeoForge META-INF/jarjar) entry so it can be inspected.

Sable ships its API library as a jarjar dependency (sable-companion-common), which is
why `javap -cp sable.jar dev.ryanhcode.sable.companion...` says "class not found".

Usage:
    # every jarjar entry into a directory
    python tools/extract_jarjar.py libs/sable-neoforge-1.21.1-2.0.5.jar libs-compile

    # only the entry we compile against (the others are megabytes of runtime libraries
    # that Sable's own jarjar already provides)
    python tools/extract_jarjar.py libs/sable-neoforge-1.21.1-2.0.5.jar libs-compile/sable-companion-common-1.21.1-1.6.0.jar
"""
import os
import sys
import zipfile


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    jar = sys.argv[1]
    outdir = sys.argv[2] if len(sys.argv) > 2 else os.path.join('libs-compile')
    os.makedirs(outdir, exist_ok=True)
    only = [a for a in sys.argv[2:] if a.endswith('.jar')]
    outdir = os.path.dirname(only[0]) if only else outdir
    with zipfile.ZipFile(jar) as zf:
        inner = [n for n in zf.namelist() if n.startswith('META-INF/jarjar/') and n.endswith('.jar')]
        if only:
            wanted = set(os.path.basename(a) for a in only)
            inner = [n for n in inner if os.path.basename(n) in wanted]
        for name in inner:
            target = os.path.join(outdir, os.path.basename(name))
            with open(target, 'wb') as fh:
                fh.write(zf.read(name))
            print('%s -> %s (%d bytes)' % (name, target, os.path.getsize(target)))
        if not inner:
            print('no jarjar entries in %s' % jar)
    return 0


if __name__ == '__main__':
    sys.exit(main())
