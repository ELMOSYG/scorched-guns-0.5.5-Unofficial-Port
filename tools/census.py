"""Census the 0.5.5 jar: namespace/folder layout + sample file contents."""
import collections
import sys
import zipfile

JAR = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\需要移植的mod\ScorchedGuns-0.5.5-1.20.1.jar"

z = zipfile.ZipFile(JAR)
names = [n for n in z.namelist() if not n.endswith("/")]
print("total files:", len(names))

cen = collections.Counter()
for n in names:
    if n.startswith("data/"):
        p = n.split("/")
        cen["/".join(p[:3]) if len(p) > 3 else "/".join(p)] += 1
print("---- data census ----")
for k, v in sorted(cen.items()):
    print(f"{v:5d}  {k}")

cen2 = collections.Counter()
for n in names:
    if n.startswith("assets/"):
        p = n.split("/")
        cen2["/".join(p[:3])] += 1
print("---- assets census ----")
for k, v in sorted(cen2.items()):
    print(f"{v:5d}  {k}")

SAMPLES = sys.argv[1:] or []
for s in SAMPLES:
    print(f"\n===== {s} =====")
    try:
        data = z.read(s).decode("utf-8", "replace")
    except KeyError:
        print("  <missing>")
        continue
    print(data[:2500])
