"""List files under given jar prefixes, or dump given file contents."""
import sys
import zipfile

JAR = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\需要移植的mod\ScorchedGuns-0.5.5-1.20.1.jar"
z = zipfile.ZipFile(JAR)
mode = sys.argv[1]
for arg in sys.argv[2:]:
    if mode == "ls":
        hits = [n for n in z.namelist() if n.startswith(arg) and not n.endswith("/")]
        print(f"===== {arg} ({len(hits)}) =====")
        for h in hits[:60]:
            print("  ", h)
    else:
        print(f"===== {arg} =====")
        try:
            print(z.read(arg).decode("utf-8", "replace")[:3000])
        except KeyError:
            print("   <missing>")
