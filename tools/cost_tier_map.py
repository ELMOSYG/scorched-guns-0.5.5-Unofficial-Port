"""Derive implicit tiers for TaCZ guns (it has no tier metadata) and map Scorched Guns tiers onto them.

TaCZ recipes only carry materials, so the classification used here is objective rather than
declared: group every gun by the rarest thing it needs and by its material mass.

    A  普通        no diamond/emerald/blaze, mass small
    B  高耗普通    no diamond/emerald/blaze, mass large
    C  稀有        needs diamond / emerald / blaze rod (but no boss drop)
    D  boss        needs netherite / nether star / dragon breath / dragon egg / heavy core

Scorched Guns tiers are then placed on the same axis by what their blueprint tier's parts need.
"""
from __future__ import annotations

import glob
import json
import os
import statistics
import zipfile

TACZ_JARS = [
    r"D:\MCJAVA\.minecraft\versions\FarmingTales_ForgeⅡ\mods\[永恒枪械工坊：零] tacz-1.20.1-1.1.8-release.jar",
    r"D:\MCJAVA\.minecraft\versions\服务器\.minecraft\versions\wlzz（真菌海少组MOD扩展）\mods\[TaC：绿葡萄附属包] lradd-1.20.1-0.3.0.jar",
    r"D:\MCJAVA\.minecraft\versions\FarmingTales_ForgeⅡ\mods\[TaCZ：绿葡萄战术装备] lrtactical-1.20.1-0.4.1.jar",
]

BOSS = ("netherite", "nether_star", "dragon_breath", "dragon_egg", "heavy_core", "wither",
        "echo_shard", "shulker_shell", "heart_of_the_sea")
# 青金石/金子/原木/铜/铁都是原版矿物，玩家本来就会挖，不算进阶门槛；
# 真正的门槛是宝石（钻石/绿宝石/紫水晶）、烈焰棒与 boss 掉落。
RARE = ("diamond", "emerald", "amethyst", "blaze")
TIER_NAME = {"A": "A 普通", "B": "B 高耗普通", "C": "C 稀有", "D": "D boss"}


def classify(names: list[str], mass: int) -> str:
    text = " ".join(names)
    if any(word in text for word in BOSS):
        return "D"
    if any(word in text for word in RARE):
        return "C"
    return "A" if mass <= 60 else "B"


def tacz_guns():
    guns = []
    for jar_path in TACZ_JARS:
        if not os.path.exists(jar_path):
            continue
        with zipfile.ZipFile(jar_path) as jar:
            for name in jar.namelist():
                if not name.endswith(".json") or "/recipes/gun/" not in name:
                    continue
                try:
                    recipe = json.loads(jar.read(name).decode("utf-8"))
                except Exception:
                    continue
                if recipe.get("type") != "tacz:gun_smith_table_crafting":
                    continue
                result = recipe.get("result") or {}
                materials = recipe.get("materials") or []
                names = []
                mass = 0
                for entry in materials:
                    item = (entry.get("item") or {})
                    key = item.get("tag") or item.get("item") or "?"
                    names.append(key)
                    mass += entry.get("count", 1)
                guns.append({
                    "id": result.get("id", name),
                    "pack": os.path.basename(jar_path),
                    "mass": mass,
                    "materials": names,
                    "tier": classify(names, mass),
                })
    return guns


def scguns_tiers():
    import importlib.util
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    spec = importlib.util.spec_from_file_location("gcr", os.path.join(root, "tools", "gun_cost_report.py"))
    gcr = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gcr)
    rows = gcr.load_guns()
    by_tier = {}
    for row in rows:
        by_tier.setdefault(row["tier"], []).append(row)
    out = []
    for tier, members in by_tier.items():
        masses = []
        for row in members:
            mass = sum(row["cost"].values()) + sum(row["other"].values())
            masses.append(mass)
        gates = []
        for row in members:
            keys = " ".join(list(row["cost"].keys()) + list(row["other"].keys()))
            if row["cost"].get("scorched") or "nether_star" in keys or "vehement_coal" in keys:
                gates.append("D")
            elif (row["cost"].get("diamond_steel") or row["cost"].get("treated_brass")
                  or row["cost"].get("treated_iron") or "diamond" in keys):
                gates.append("C")
            else:
                gates.append("A")
        derived = max(set(gates), key=gates.count)
        label = gcr.TIER_NAMES.get(tier) or ("炮塔（无蓝图）" if tier == "" else tier)
        out.append({"tier": tier, "name": label, "n": len(members),
                    "median_mass": statistics.median(masses), "gate": derived})
    order = ["antique", "copper", "iron", "frontier", "treated_brass", "wrecker", "diamond_steel",
             "deep_dark", "ocean", "piglin", "end", "exo_suit", "scorched"]
    out.sort(key=lambda r: order.index(r["tier"]) if r["tier"] in order else 99)
    return out


def main() -> int:
    guns = tacz_guns()
    print("TaCZ guns found: %d" % len(guns))
    groups = {}
    for gun in guns:
        groups.setdefault(gun["tier"], []).append(gun)
    for tier in "ABCD":
        members = groups.get(tier, [])
        if not members:
            continue
        masses = [m["mass"] for m in members]
        print("\n%s : %d guns, mass median %.0f (min %d, max %d)" %
              (TIER_NAME[tier], len(members), statistics.median(masses), min(masses), max(masses)))
        for gun in sorted(members, key=lambda g: g["mass"])[:6]:
            print("   %-22s %4d  %s" % (gun["id"].replace("tacz:", "").replace("lradd:", ""), gun["mass"],
                                         ", ".join(gun["materials"])[:70]))
        if len(members) > 6:
            print("   ... heaviest: " + ", ".join(
                "%s(%d)" % (g["id"].split(":")[-1], g["mass"]) for g in
                sorted(members, key=lambda g: -g["mass"])[:4]))

    print("\nScorched Guns tiers on the same axis:")
    print("  %-30s %4s %8s %6s" % ("tier", "guns", "median", "gate"))
    for row in scguns_tiers():
        print("  %-30s %4d %8.1f %6s" % (row["name"], row["n"], row["median_mass"], row["gate"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
