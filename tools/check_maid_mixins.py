"""Check the maid compat's mixins against the classes they actually target.

Mixin mistakes only show up when the game loads them, and each one costs a full server start to
find. This resolves every @Mixin target in maid-compat and confirms that each injection's method
(and each @Accessor/@Invoker member) still exists in the target class:

  * targets inside the host port  -> resolved through the port's compiled classes
  * targets inside Touhou Little Maid -> resolved through maid-compat/libs

Reported problems are exactly the class of failure seen at runtime, e.g. an @Overwrite whose
method the host no longer has (SulfurheadEntity#hurt, removed when the port dropped SC2's
player-only damage rule).

Usage:
    gradlew printCompileClasspath            # refresh build-logs/compile-classpath.txt
    python tools/check_maid_mixins.py

Exit code 0 = every injection point resolves; 1 = at least one does not.
"""
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIXINS = os.path.join(REPO, 'maid-compat', 'src', 'main', 'java', 'com', 'scg2tlm', 'elmomod', 'mixin')
LIBS = os.path.join(REPO, 'maid-compat', 'libs')
CLASSPATH_FILE = os.path.join(REPO, 'build-logs', 'maid-compat-classpath.txt')
JAVAP = r'D:\jdk-21.0.3\bin\javap.exe'

TARGET = re.compile(r'@Mixin\s*\(\s*(?:value\s*=\s*)?(?:\{[^}]*)?([\w.]+)\s*\.class')
IMPORT = re.compile(r'^import\s+([\w.]+);', re.M)
# @Inject/@Redirect/@ModifyArg/... carry method = "x" or method = {"x", "y"}
INJECTION = re.compile(r'@(Inject|Redirect|ModifyVariable|ModifyArg|ModifyArgs|WrapOperation|Overwrite)\b'
                       r'\s*(?:\(([^)]*)\))?', re.S)
MEMBERSEL = re.compile(r'@(Accessor|Invoker)\s*\(\s*(?:value\s*=\s*)?"([\w$<>]+)"')
METHOD_ATTR = re.compile(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', re.S)
QUOTED = re.compile(r'"([^"]*)"')

_cache: dict[str, str | None] = {}


def classpath():
    entries = [os.path.join(REPO, 'build', 'classes', 'java', 'main')]
    entries += [os.path.join(LIBS, n) for n in sorted(os.listdir(LIBS)) if n.endswith('.jar')]
    entries += open(CLASSPATH_FILE, encoding='utf-8').read().strip().split(os.pathsep)
    return os.pathsep.join(entries)


def members(fqn):
    """Member names of a class, or None when the class cannot be resolved."""
    if fqn in _cache:
        return _cache[fqn]
    result = subprocess.run([JAVAP, '-p', '-classpath', classpath(), fqn],
                            capture_output=True, text=True, encoding='utf-8', errors='replace')
    text = result.stdout if result.returncode == 0 else None
    _cache[fqn] = text
    return text


def main():
    if not os.path.isfile(CLASSPATH_FILE):
        print('missing %s - run: gradlew printCompileClasspath' % CLASSPATH_FILE)
        return 1

    problems = 0
    checked = 0
    for name in sorted(os.listdir(MIXINS)):
        if not name.endswith('.java'):
            continue
        path = os.path.join(MIXINS, name)
        text = open(path, encoding='utf-8', errors='replace').read()
        imports = {i.rsplit('.', 1)[-1]: i for i in IMPORT.findall(text)}

        target_match = TARGET.search(text)
        if not target_match:
            # @Mixin(targets = "...") points at a mod that is only present sometimes (scgextra);
            # such a config is registered conditionally at runtime, so it cannot be resolved here.
            if re.search(r'@(Pseudo|Mixin\s*\(\s*targets\s*=)', text):
                print('%-38s skipped (string/optional target)' % name)
                continue
            print('%s: no @Mixin target found' % name)
            problems += 1
            continue
        simple = target_match.group(1).rsplit('.', 1)[-1]
        fqn = imports.get(simple, target_match.group(1))
        target_members = members(fqn)
        if target_members is None:
            print('%-38s target %s NOT FOUND on the classpath' % (name, fqn))
            problems += 1
            continue

        # methods of the target (name appears as "name(" in javap output)
        names = set(re.findall(r'([\w$]+)\s*\(', target_members))
        fields = set(re.findall(r'([\w$]+)\s*;', target_members))
        all_members = names | fields

        for kind, args in INJECTION.findall(text):
            args = args or ''
            attr = METHOD_ATTR.search(args)
            if attr:
                wanted = QUOTED.findall(attr.group(1))
            elif kind == 'Overwrite':
                # @Overwrite without method= overwrites the handler's own name
                handler = re.search(r'@Overwrite\b[^)]*\)?\s*(?:public|protected|private)?\s*[\w<>,\[\] .]+\s+(\w+)\s*\(',
                                    text, re.S)
                wanted = [handler.group(1)] if handler else []
            else:
                wanted = []
            for method in wanted:
                checked += 1
                if method not in all_members:
                    print('%-38s %s -> %s.%s  MISSING' % (name, kind, fqn.rsplit('.', 1)[-1], method))
                    problems += 1

        for kind, member in MEMBERSEL.findall(text):
            checked += 1
            if member not in all_members:
                print('%-38s %s -> %s.%s  MISSING' % (name, kind, fqn.rsplit('.', 1)[-1], member))
                problems += 1

    print('')
    print('%d member reference(s) checked across the compat mixins, %d problem(s)' % (checked, problems))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
