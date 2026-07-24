"""Publica una release de Crónicas del Apetito y actualiza el manifiesto de updates.

Ritual completo (hermano del de DracPDF): build del APK de release FIRMADO,
lectura del versionCode/versionName (fuente única: app/build.gradle.kts), cálculo
del sha256, verificación de coherencia (el versionCode del APK construido, leído
con aapt2, debe coincidir con el del manifiesto; si no, aborta), creación de la
Release en GitHub con el asset (gh CLI, verificando antes gh auth status) y
publicación del manifiesto docs/updates.json en GitHub Pages (commit + push),
verificando después que la URL pública ya sirve el versionCode nuevo (reintentando
por la caché del CDN).

Secretos: la firma sale de keystore.properties (fuera del repo, gitignored) o de
variables de entorno CRONICAS_STORE_FILE / CRONICAS_STORE_PASSWORD /
CRONICAS_KEY_ALIAS / CRONICAS_KEY_PASSWORD. Si faltan, aborta con mensaje claro.
Ningún secreto se escribe en el repo.

Uso:
    python scripts/publicar_release.py             # construye y publica
    python scripts/publicar_release.py --dry-run    # prepara sin publicar
    python scripts/publicar_release.py --notas "…"  # notas de la versión
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
BUILD_GRADLE = RAIZ / "app" / "build.gradle.kts"
MANIFIESTO = RAIZ / "docs" / "updates.json"
APK_RELEASE = RAIZ / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"

_REPO = "marcmayol/cronicas-del-apetito"
_PAGES_URL = "https://marcmayol.github.io/cronicas-del-apetito/updates.json"
_CHECK_HORAS = 24
_ENV_FIRMA = (
    "CRONICAS_STORE_FILE",
    "CRONICAS_STORE_PASSWORD",
    "CRONICAS_KEY_ALIAS",
    "CRONICAS_KEY_PASSWORD",
)


# --- utilidades ---------------------------------------------------------------

def _ejecutar(cmd: list[str], **kw) -> None:
    print("»", " ".join(cmd))
    if subprocess.call(cmd, cwd=str(RAIZ), **kw) != 0:
        raise SystemExit(f"Falló: {' '.join(cmd)}")

def _salida(cmd: list[str]) -> str:
    return subprocess.run(
        cmd, cwd=str(RAIZ), capture_output=True, text=True
    ).stdout

def sha256(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(65536), b""):
            h.update(bloque)
    return h.hexdigest()

def _gradlew() -> str:
    return "gradlew.bat" if os.name == "nt" else "./gradlew"


# --- versión (fuente única: build.gradle.kts) ---------------------------------

def leer_version() -> tuple[int, str]:
    texto = BUILD_GRADLE.read_text(encoding="utf-8")
    vc = re.search(r"versionCode\s*=\s*(\d+)", texto)
    vn = re.search(r'versionName\s*=\s*"([^"]+)"', texto)
    if not vc or not vn:
        raise SystemExit("No se pudo leer versionCode/versionName de app/build.gradle.kts.")
    return int(vc.group(1)), vn.group(1)


# --- firma --------------------------------------------------------------------

def asegurar_firma() -> None:
    """Comprueba que hay credenciales de firma; si vienen por env, las materializa
    en un keystore.properties temporal (borrado al terminar). Nunca sobrescribe uno
    existente ni deja secretos en el repo."""
    props = RAIZ / "keystore.properties"
    if props.exists():
        print("Firma: usando keystore.properties existente.")
        return
    if all(os.environ.get(k) for k in _ENV_FIRMA):
        print("Firma: usando variables de entorno (keystore.properties temporal).")
        props.write_text(
            f"storeFile={os.environ['CRONICAS_STORE_FILE']}\n"
            f"storePassword={os.environ['CRONICAS_STORE_PASSWORD']}\n"
            f"keyAlias={os.environ['CRONICAS_KEY_ALIAS']}\n"
            f"keyPassword={os.environ['CRONICAS_KEY_PASSWORD']}\n",
            encoding="utf-8",
        )
        import atexit
        atexit.register(lambda: props.exists() and props.unlink())
        return
    raise SystemExit(
        "Faltan credenciales de firma. Copia keystore.properties.example a "
        "keystore.properties (fuera del repo) y rellénalo, o define las variables "
        f"de entorno: {', '.join(_ENV_FIRMA)}."
    )


# --- aapt2 (verificación de coherencia) ---------------------------------------

def _sdk_dir() -> Path:
    local = RAIZ / "local.properties"
    if local.exists():
        m = re.search(r"sdk\.dir=(.+)", local.read_text(encoding="utf-8"))
        if m:
            return Path(m.group(1).strip().replace("\\\\", "\\").replace("\\:", ":"))
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(env):
            return Path(os.environ[env])
    raise SystemExit("No encuentro el Android SDK (local.properties o ANDROID_HOME).")

def _aapt2() -> Path:
    exe = "aapt2.exe" if os.name == "nt" else "aapt2"
    candidatos = sorted((_sdk_dir() / "build-tools").glob(f"*/{exe}"), reverse=True)
    if not candidatos:
        raise SystemExit("No encuentro aapt2 en build-tools del SDK.")
    return candidatos[0]

def version_code_del_apk(apk: Path) -> int:
    salida = _salida([str(_aapt2()), "dump", "badging", str(apk)])
    m = re.search(r"versionCode='(\d+)'", salida)
    if not m:
        raise SystemExit("No pude leer el versionCode del APK con aapt2.")
    return int(m.group(1))


# --- manifiesto ---------------------------------------------------------------

def url_release(version_name: str) -> str:
    return (
        f"https://github.com/{_REPO}/releases/download/"
        f"v{version_name}/cronicas-del-apetito-v{version_name}.apk"
    )

def generar_manifiesto(vc: int, vn: str, sha: str, notas: str) -> dict:
    return {
        "versionCode": vc,
        "versionName": vn,
        "url": url_release(vn),
        "sha256": sha,
        "notas": notas or f"Crónicas del Apetito {vn}.",
        "check_horas": _CHECK_HORAS,
    }

def verificar_coherencia(vc_declarado: int, apk: Path, manifiesto: dict) -> None:
    """Cinturón: el versionCode del APK construido, el declarado y el del manifiesto
    coinciden; y el sha256 del manifiesto es el del APK real."""
    vc_apk = version_code_del_apk(apk)
    if vc_apk != vc_declarado:
        raise SystemExit(
            f"El APK construido tiene versionCode {vc_apk}, pero build.gradle.kts "
            f"declara {vc_declarado}. Aborto."
        )
    if manifiesto["versionCode"] != vc_declarado:
        raise SystemExit("El versionCode del manifiesto no coincide con el declarado.")
    if manifiesto["sha256"] != sha256(apk):
        raise SystemExit("El sha256 del manifiesto no coincide con el APK construido.")


# --- construcción -------------------------------------------------------------

def construir() -> Path:
    asegurar_firma()
    _ejecutar([_gradlew(), ":app:assembleRelease"])
    if not APK_RELEASE.is_file():
        raise SystemExit(f"No se generó el APK de release: {APK_RELEASE}")
    return APK_RELEASE

def preparar(notas: str) -> tuple[dict, Path]:
    """Construye, genera y escribe el manifiesto tras verificar coherencia."""
    vc, vn = leer_version()
    apk = construir()
    manifiesto = generar_manifiesto(vc, vn, sha256(apk), notas)
    verificar_coherencia(vc, apk, manifiesto)
    MANIFIESTO.parent.mkdir(parents=True, exist_ok=True)
    MANIFIESTO.write_text(
        json.dumps(manifiesto, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    return manifiesto, apk


# --- publicación --------------------------------------------------------------

def _asset_con_nombre(apk: Path, vn: str) -> Path:
    destino = apk.with_name(f"cronicas-del-apetito-v{vn}.apk")
    if destino != apk:
        destino.write_bytes(apk.read_bytes())
    return destino

def verificar_gh() -> None:
    if subprocess.call(["gh", "auth", "status"]) != 0:
        raise SystemExit("gh no está autenticado. Ejecuta: gh auth login")

def publicar(apk: Path, manifiesto: dict, notas: str) -> None:
    vn = manifiesto["versionName"]
    asset = _asset_con_nombre(apk, vn)
    _ejecutar([
        "gh", "release", "create", f"v{vn}", str(asset),
        "--repo", _REPO,
        "--title", f"Crónicas del Apetito {vn}",
        "--notes", notas or f"Crónicas del Apetito {vn}.",
    ])
    _ejecutar(["git", "add", str(MANIFIESTO)])
    _ejecutar(["git", "commit", "-m", f"Publica el manifiesto de la v{vn}"])
    _ejecutar(["git", "push", "origin", "main"])

def verificar_url_publica(vc_esperado: int, intentos: int = 30, espera_s: int = 10) -> None:
    """La URL de Pages puede tardar por la caché del CDN: reintenta unos minutos."""
    for i in range(1, intentos + 1):
        try:
            with urllib.request.urlopen(_PAGES_URL, timeout=15) as r:
                data = json.loads(r.read().decode("utf-8"))
            if data.get("versionCode") == vc_esperado:
                print(f"URL pública OK: sirve versionCode {vc_esperado}.")
                return
            print(f"[{i}/{intentos}] Pages sirve {data.get('versionCode')}, esperaba {vc_esperado}…")
        except Exception as e:  # noqa: BLE001
            print(f"[{i}/{intentos}] Aún no disponible ({e.__class__.__name__})…")
        time.sleep(espera_s)
    raise SystemExit(
        "La URL pública no sirvió el versionCode nuevo a tiempo. La Release SÍ se "
        "creó; revisa GitHub Pages (rama/carpeta /docs) y la caché del CDN."
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Publica una release de Crónicas del Apetito.")
    parser.add_argument("--dry-run", action="store_true", help="prepara sin publicar")
    parser.add_argument("--notas", default="", help="notas de la versión")
    args = parser.parse_args(argv)

    if not args.dry_run:
        verificar_gh()

    manifiesto, apk = preparar(args.notas)
    print(f"Manifiesto v{manifiesto['versionName']} "
          f"(versionCode {manifiesto['versionCode']}, sha256 {manifiesto['sha256'][:12]}…)")
    print(f"APK: {apk}")

    if args.dry_run:
        print("--dry-run: preparado sin publicar (Release y manifiesto no subidos).")
        return 0

    publicar(apk, manifiesto, args.notas)
    verificar_url_publica(manifiesto["versionCode"])
    print(f"Release v{manifiesto['versionName']} publicada y manifiesto en Pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
