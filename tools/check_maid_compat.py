"""Compile the maid compat with javac against this port, and report the errors.

This is the inner loop for porting the compat: javac is far faster than a Gradle round, and it
needs the same three inputs (the port's compiled classes, NeoForge + the vendored libs from
build-logs/compile-classpath.txt, and the TLM build the user runs).

    gradlew printCompileClasspath        # refresh build-logs/compile-classpath.txt first
    python tools/check_maid_compat.py    # 0 errors => ready for the gradle build

Pass --tag NAME to write the log/classes to NAME-suffixed paths, so several people (or agents)
can run it at the same time without clobbering each other.

Exit code 0 means the compat sources compile; 1 means they do not.
"""
import os
import re
import subprocess
import sys
from collections import Counter

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCES = os.path.join(REPO, 'maid-compat', 'src', 'main', 'java')
LIBS = os.path.join(REPO, 'maid-compat', 'libs')
TAG = ''
if '--tag' in sys.argv:
    TAG = '-' + sys.argv[sys.argv.index('--tag') + 1]
OUT = os.path.join(REPO, 'build', 'maid-compat-classes' + TAG)
LOG = os.path.join(REPO, 'build-logs', 'maid-compat-javac%s.txt' % TAG)
JAVAC = r'D:\jdk-21.0.3\bin\javac.exe'


def main():
    port_classes = os.path.join(REPO, 'build', 'classes', 'java', 'main')
    classpath_file = os.path.join(REPO, 'build-logs', 'compile-classpath.txt')
    if not os.path.isfile(classpath_file):
        print('missing %s - run: gradlew printCompileClasspath' % classpath_file)
        return 1
    base = open(classpath_file, encoding='utf-8').read().strip()

    # javac's @argfile treats a backslash as an escape, so separators are written as forward
    # slashes; quoting is only needed for entries that contain a space.
    entries = [port_classes]
    entries += [os.path.join(LIBS, n) for n in sorted(os.listdir(LIBS)) if n.endswith('.jar')]
    entries += base.split(os.pathsep)
    cleaned = [e.replace('\\', '/') for e in entries]
    argfile = os.path.join(REPO, 'build-logs', 'maid-compat-classpath.txt')
    open(argfile, 'w', encoding='utf-8').write(
        os.pathsep.join('"%s"' % e if ' ' in e else e for e in cleaned))

    sources = []
    for root, _dirs, names in os.walk(SOURCES):
        sources += [os.path.join(root, n) for n in names if n.endswith('.java')]
    sources_file = os.path.join(REPO, 'build-logs', 'maid-compat-sources.txt')
    open(sources_file, 'w', encoding='utf-8').write('\n'.join(sources))
    os.makedirs(OUT, exist_ok=True)

    result = subprocess.run(
        [JAVAC, '-J-Duser.language=en', '-J-Duser.country=US', '-J-Dfile.encoding=UTF-8',
         '-nowarn', '-proc:none', '-Xmaxerrs', '20000',
         '-cp', '@' + argfile, '-sourcepath', SOURCES, '-d', OUT, '@' + sources_file],
        capture_output=True, text=True, encoding='utf-8', errors='replace')
    text = result.stdout + result.stderr
    open(LOG, 'w', encoding='utf-8').write(text)

    errors = re.findall(r'^(\S+):(\d+): error: (.*)$', text, re.M)
    print('%d source files, %d error(s)' % (len(sources), len(errors)))
    if not errors:
        return 0

    per_file = Counter(os.path.basename(e[0]) for e in errors)
    print('')
    print('=== errors per file')
    for name, count in per_file.most_common():
        print('  %3d  %s' % (count, name))
    print('')
    print('=== first errors')
    for path, line, msg in errors[:25]:
        print('  %s:%s: %s' % (os.path.basename(path), line, msg[:120]))
    print('')
    print('full log: %s' % LOG)
    return 1


if __name__ == '__main__':
    sys.exit(main())
