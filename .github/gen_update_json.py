#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o JSON de atualizacao do fork (smarttube_fork2.json), consumido pelo
AppUpdateChecker dentro do app. Tambem gera changelog_lines.md pra notas da release.

Uso: python3 gen_update_json.py <tag> [dir_dos_apks]
Formato esperado pelo AppVersionChecker:
{
  "package": {"downloadUrl": ..., "downloadUrlList_<abi>": [...]},
  "<versionName>": {"versionCode": N, "changelog": [...]}
}
"""
import glob
import json
import os
import re
import subprocess
import sys

REPO = "MicaelSanPedro/SmartTube"
ABI_MAP = [
    ("arm64-v8a", "downloadUrlList_arm64-v8a"),
    ("armeabi-v7a", "downloadUrlList_armeabi-v7a"),
    ("x86", "downloadUrlList_x86"),
]
MAX_CHANGELOG = 8


def gradle_versions():
    with open("smarttubetv/build.gradle", encoding="utf-8") as f:
        gradle = f.read()
    code = int(re.search(r"versionCode\s+(\d+)", gradle).group(1))
    name = re.search(r'versionName\s+"([^"]+)"', gradle).group(1)
    return code, name


def git_changelog(tag):
    try:
        # Só tags de release do fork (v*-fork.*), ignorando as tags herdadas do upstream
        prev = subprocess.check_output(
            ["git", "describe", "--tags", "--match", "v*-fork.*", "--abbrev=0", f"{tag}~1"],
            text=True, stderr=subprocess.DEVNULL,
        ).strip()
        print(f"changelog: commits desde {prev}")
        log = subprocess.check_output(
            ["git", "log", f"{prev}..{tag}", "--pretty=%s"], text=True, stderr=subprocess.DEVNULL
        ).splitlines()
        skip_prefixes = ("Merge", "merge", "Bump", "bump")
        msgs = [s.strip() for s in log if s.strip() and not s.startswith(skip_prefixes)]
        return msgs[:MAX_CHANGELOG]
    except Exception as e:  # sem historico (ex: teste local) — ok
        print(f"aviso: changelog de git indisponivel ({e})", file=sys.stderr)
        return []


def main():
    if len(sys.argv) < 2:
        print("uso: gen_update_json.py <tag> [dir_apks]", file=sys.stderr)
        sys.exit(2)
    tag = sys.argv[1]
    apk_dir = sys.argv[2] if len(sys.argv) > 2 else "signed/"

    version_code, version_name = gradle_versions()

    apks = sorted(glob.glob(os.path.join(apk_dir, "*.apk")))
    if not apks:
        print(f"ERRO: nenhum APK em {apk_dir}", file=sys.stderr)
        sys.exit(1)

    def url_for(apk):
        return f"https://github.com/{REPO}/releases/download/{tag}/{os.path.basename(apk)}"

    universal = next((a for a in apks if "universal" in os.path.basename(a)), None)
    fallback = url_for(universal or apks[0])

    pkg = {"downloadUrl": fallback, "downloadUrlList": [fallback]}
    for needle, key in ABI_MAP:
        match = next((a for a in apks if needle in os.path.basename(a)), None)
        if match:
            pkg[key] = [url_for(match)]

    changelog = git_changelog(tag) or ["Otimizações de desempenho e correções do fork."]

    data = {
        "package": pkg,
        version_name: {"versionCode": version_code, "changelog": changelog},
    }

    with open("smarttube_fork2.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

    with open("changelog_lines.md", "w", encoding="utf-8") as f:
        f.write("\n".join(f"- {c}" for c in changelog))

    print(f"JSON gerado: v{version_name} (code {version_code}), {len(apks)} APK(s), {len(changelog)} linha(s) de changelog")
    print(json.dumps(data, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
