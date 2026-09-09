# -*- coding: utf-8 -*-
"""
Monta las capturas de la ficha de Play a partir de las capturas crudas del móvil.

Por qué existe esto y no se suben las capturas tal cual:

1. **Ratio.** Play no admite capturas con una relación mayor de 2:1, y el móvil las hace
   1080x2400, que es 2,22:1. Aquí salen 1080x1920.
2. **Se ven a 200 px de alto.** En el listado, la captura de un móvil entero es una mancha
   ilegible. Lo que se lee es el titular que va encima, y eso es lo que decide si alguien
   sigue mirando. Es lo que pedía el informe de testers de sep-2026.

Cada titular describe **lo que se ve en esa pantalla**. Si algo no aparece en la imagen, no
se escribe.

    python scripts/generar_capturas_play.py            # castellano
    python scripts/generar_capturas_play.py --idioma en

Lee de `play-store/capturas-crudas/<idioma>/` y escribe en `play-store/graficos/<idioma>/`.
"""

from __future__ import annotations

import argparse
import os
import sys

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FUENTE_TITULO = os.path.join(RAIZ, "app", "src", "main", "res", "font", "lora.ttf")

ANCHO, ALTO = 1080, 1920

# La paleta de la app (ui/Theme.kt), no una parecida.
CREMA = (250, 246, 239)
TOSTADO = (241, 227, 209)
MARRON = (107, 67, 38)
TINTA = (42, 32, 24)
TINTA_SUAVE = (111, 96, 82)
BORDE_TARJETA = (201, 179, 148)

# Los cuatro tipos, con su color y su glifo: la misma regla que dentro de la app.
TIPOS = [((154, 91, 47), "●"), ((74, 106, 61), "▲"), ((122, 76, 138), "◆"), ((45, 90, 130), "■")]

MARGEN = 76

# Alto de la barra de estado del móvil, que se recorta: la hora del emulador, la
# batería y los iconos de otras notificaciones no son parte de la app.
BARRA_ESTADO = 100

CAPTURAS = {
    "es": [
        (
            "01-aviso.png",
            "No te acuerdas tú:\nte lo pregunta ella",
            "El aviso llega a tu hora y se contesta sin abrir la app.",
        ),
        (
            "02-anotar.png",
            "Comida, caminata,\ngimnasio o ánimo",
            "Los cuatro se encienden y se apagan por separado.",
        ),
        (
            "03-dia.png",
            "Tu día, escrito\ncon tus palabras",
            "Sin pesar, sin contar calorías y sin buscar en listas.",
        ),
        (
            "04-semana.png",
            "La semana entera\nde un vistazo",
            "Comidas, minutos andados, notas y gimnasio, sumados solos.",
        ),
        (
            "05-mes.png",
            "El mes, día\na día",
            "Cada tipo lleva su símbolo además de su color: se lee hasta impreso.",
        ),
        (
            "06-recordatorios.png",
            "A tus horas,\nno a las mías",
            "Entre qué horas, cada cuánto y de qué quieres que te pregunte.",
        ),
    ],
    "en": [
        (
            "01-aviso.png",
            "You don't remember:\nit asks you",
            "The reminder arrives at your hour and is answered without opening the app.",
        ),
        (
            "02-anotar.png",
            "Meals, walks,\ngym or mood",
            "All four switch on and off separately.",
        ),
        (
            "03-dia.png",
            "Your day, written\nin your own words",
            "No weighing, no calorie counting, no searching a database.",
        ),
        (
            "04-semana.png",
            "A whole week\nat a glance",
            "Meals, minutes walked, notes and gym, added up for you.",
        ),
        (
            "05-mes.png",
            "The month,\nday by day",
            "Every type carries a symbol as well as a colour: it reads even printed.",
        ),
        (
            "06-recordatorios.png",
            "On your hours,\nnot mine",
            "Between which hours, how often, and what you want to be asked about.",
        ),
    ],
}


def fuente(ruta: str, tam: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(ruta, tam)


def barra_de_marca(img: Image.Image) -> None:
    """Los cuatro colores de los tipos como firma superior, en su orden de siempre."""
    d = ImageDraw.Draw(img)
    alto = 10
    ancho_tramo = ANCHO / len(TIPOS)
    for i, (color, _) in enumerate(TIPOS):
        d.rectangle([int(i * ancho_tramo), 0, int((i + 1) * ancho_tramo), alto], fill=color)


def redondea(img: Image.Image, radio: int) -> Image.Image:
    mascara = Image.new("L", img.size, 0)
    ImageDraw.Draw(mascara).rounded_rectangle([0, 0, img.width - 1, img.height - 1], radio, fill=255)
    fuera = Image.new("RGBA", img.size, (0, 0, 0, 0))
    fuera.paste(img, (0, 0), mascara)
    return fuera


def compone(origen: str, nombre: str, titular: str, apoyo: str) -> Image.Image:
    lienzo = Image.new("RGB", (ANCHO, ALTO), CREMA)
    barra_de_marca(lienzo)
    d = ImageDraw.Draw(lienzo)

    f_tit = fuente(FUENTE_TITULO, 82)
    f_sub = ImageFont.truetype("arial.ttf", 38)

    y = 128
    for linea in titular.split("\n"):
        d.text((MARGEN, y), linea, font=f_tit, fill=TINTA)
        y += 100

    y += 16
    # El apoyo se parte a mano: son frases cortas y así no depende de textwrap.
    linea, lineas = "", []
    for p in apoyo.split():
        prueba = (linea + " " + p).strip()
        if d.textlength(prueba, font=f_sub) > ANCHO - 2 * MARGEN:
            lineas.append(linea)
            linea = p
        else:
            linea = prueba
    lineas.append(linea)
    for l in lineas:
        d.text((MARGEN, y), l, font=f_sub, fill=TINTA_SUAVE)
        y += 52

    # La captura entra por abajo y se sale del lienzo a propósito: se ve que es un móvil
    # sin gastar media imagen en enseñar el marco entero.
    cruda = Image.open(os.path.join(origen, nombre)).convert("RGB")
    cruda = cruda.crop((0, BARRA_ESTADO, cruda.width, cruda.height))
    ancho_movil = 780
    alto_movil = int(cruda.height * ancho_movil / cruda.width)
    movil = cruda.resize((ancho_movil, alto_movil), Image.LANCZOS)
    movil = redondea(movil, 36)

    arriba = max(y + 40, 470)
    x = (ANCHO - ancho_movil) // 2
    lienzo.paste(movil, (x, arriba), movil)

    # Un filo para que el crema de la captura no se funda con el crema del fondo.
    visible = ALTO - arriba
    ImageDraw.Draw(lienzo).rounded_rectangle(
        [x, arriba, x + ancho_movil - 1, arriba + min(alto_movil, visible + 40) - 1],
        36,
        outline=BORDE_TARJETA,
        width=3,
    )
    return lienzo


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--idioma", default="es", choices=sorted(CAPTURAS))
    args = ap.parse_args()

    origen = os.path.join(RAIZ, "play-store", "capturas-crudas", args.idioma)
    destino = os.path.join(RAIZ, "play-store", "graficos", args.idioma)
    if not os.path.isdir(origen):
        print("No encuentro %s" % origen)
        return 1
    os.makedirs(destino, exist_ok=True)
    for nombre, titular, apoyo in CAPTURAS[args.idioma]:
        if not os.path.exists(os.path.join(origen, nombre)):
            print("  falta %s, la salto" % nombre)
            continue
        compone(origen, nombre, titular, apoyo).save(os.path.join(destino, nombre))
        print("  %s  %dx%d" % (nombre, ANCHO, ALTO))
    print("Listas en %s" % destino)
    return 0


if __name__ == "__main__":
    sys.exit(main())
