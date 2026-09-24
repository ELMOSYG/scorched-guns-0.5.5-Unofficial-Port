"""Compare the values of en_us.json and zh_cn.json, not just their keys.

tools/audit_lang_keys.py proves every key exists in both. It cannot tell whether the Chinese text
sits on the *right* key, which is exactly the failure mode the player suspects: a translation pasted
onto a neighbouring line. This looks for the parts of a string that must not change when it is
translated:

  * format specifiers (%s, %d, %1$s) - a translation with the wrong count crashes or prints wrong
  * numbers and enum-like tokens (tick counts, item ids, key names)
  * entries whose Chinese is grossly longer or shorter than the English

Report only for now: it prints candidates for a human to read.

Usage:  python tools/audit_lang_values.py            (summary)
        python tools/audit_lang_values.py --all      (list every flagged entry)
"""
import json
import pathlib
import re
import sys

LANG = pathlib.Path("src/main/resources/assets/scguns/lang")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")
NUMBER = re.compile(r"\d+(?:\.\d+)?")
TOKEN = re.compile(r"[a-z_]+:[a-z0-9_/.]+|\b[a-z]+(?:_[a-z]+)+\b")

# English keyword -> the Chinese terms a faithful translation of that keyword should contain. This is
# the closest a script can get to "does the Chinese describe the same thing": it cannot judge wording,
# but a description about damage that contains no word for damage is a wrong key more often than not.
KEYWORD_PAIRS = {
    "tick": ("刻", "tick"),
    "second": ("秒",),
    "damage": ("伤害", "损伤", "伤害值"),
    "radius": ("半径", "范围"),
    "ammo": ("弹药", "子弹", "弹"),
    "reload": ("换弹", "装填", "上弹"),
    "magazine": ("弹匣", "弹夹"),
    "accuracy": ("精度", "准确", "散布"),
    "recoil": ("后坐", "后座"),
    "fire rate": ("射速", "开火速度"),
    "durability": ("耐久",),
    "enchant": ("附魔",),
    "armor": ("护甲", "装甲"),
    "health": ("生命", "血量"),
    "speed": ("速度",),
    "cooldown": ("冷却",),
    "range": ("射程", "距离", "范围"),
    "aim": ("瞄准",),
    "craft": ("合成", "制作"),
    "loot": ("战利品", "掉落"),
    "shield": ("盾",),
    "explosion": ("爆炸", "爆"),
    "poison": ("中毒", "毒"),
    "sulfur": ("硫",),
    "maid": ("女仆",),
    "turret": ("炮塔", "炮台"),
    "bayonet": ("刺刀",),
    "grenade": ("手雷", "榴弹", "投掷"),
    "block": ("块", "方块", "格挡"),
    "weight": ("重量", "负重", "重力"),
    "energy": ("能量", "电量", "充能"),
    "progression": ("进度", "等级", "阶段"),
    "orange": ("橙",),
}


def placeholders(text):
    return sorted(PLACEHOLDER.findall(text))


def numbers(text):
    return sorted(NUMBER.findall(text))


def tokens(text):
    return sorted(set(TOKEN.findall(text.lower())))


def main():
    show_all = "--all" in sys.argv
    en = json.loads((LANG / "en_us.json").read_text(encoding="utf-8"))
    zh = json.loads((LANG / "zh_cn.json").read_text(encoding="utf-8"))

    shared = sorted(set(en) & set(zh))
    bad_placeholders, bad_numbers, length_outliers = [], [], []
    token_loss = []
    keyword_loss = []

    for key in shared:
        e, z = en[key], zh[key]
        if placeholders(e) != placeholders(z):
            bad_placeholders.append((key, e, z))
        if numbers(e) != numbers(z):
            bad_numbers.append((key, e, z))
        # English words/ids that have no counterpart in the Chinese: strong smell of a wrong key,
        # weak on its own (many are simply translated). Only ids/key-ish tokens are reported.
        lost = [t for t in tokens(e) if ":" in t and t not in z]
        if lost:
            token_loss.append((key, e, z, lost))
        if len(z) > max(40, len(e) * 4) or (len(e) > 30 and len(z) < len(e) / 8):
            length_outliers.append((key, e, z))

        # Word boundaries matter: without them "range" matches "Orange" and the report fills with
        # nonsense. Even so this check stays noisy - a translated word is often not the one listed
        # here - so it is a review aid, never a gate. See HANDOFF section 52.
        words = set(re.findall(r"[a-z]+", e.lower()))
        absent = [word for word, terms in KEYWORD_PAIRS.items()
                  if word in words and not any(term in z for term in terms)]
        if absent and len(z.strip()) > 2:
            keyword_loss.append((key, e, z, absent))

    def dump(name, rows, limit=12):
        print("\n=== %s: %d" % (name, len(rows)))
        for row in rows[:limit if not show_all else len(rows)]:
            print("  %s" % row[0])
            print("      en: %s" % row[1])
            print("      zh: %s" % row[2])
            if len(row) > 3:
                print("      missing tokens: %s" % row[3])
        if len(rows) > limit and not show_all:
            print("  ... %d more (--all)" % (len(rows) - limit))

    print("%d keys in both files" % len(shared))
    dump("format specifiers differ", bad_placeholders)
    dump("numbers differ", bad_numbers)
    dump("namespaced ids lost in translation", token_loss)
    dump("length outliers", length_outliers)
    dump("Chinese lacks the term the English is about", keyword_loss)


if __name__ == "__main__":
    main()
