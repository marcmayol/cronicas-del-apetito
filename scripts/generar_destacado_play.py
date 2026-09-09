# -*- coding: utf-8 -*-
"""Genera el grafico destacado (1024x500) de la ficha de Play, por idioma.

La ficha en ingles es una ficha aparte, y hasta ahora heredaba el destacado en
castellano: en la ficha inglesa se leia "Cronicas del Apetito" sobre una app que
alli se llama Appetite Chronicles.

El panel izquierdo -el tenedor- se toma del destacado original en vez de
redibujarlo: asi las dos versiones comparten el mismo dibujo, pixel a pixel, y
esto solo decide el texto.

    python scripts/generar_destacado_play.py --idioma en
    → play-store/graficos/en/destacado-1024x500.png
"""

import argparse
import os

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FUENTE = os.path.join(RAIZ, "app", "src", "main", "res", "font", "lora.ttf")
BASE = os.path.join(RAIZ, "play-store", "graficos", "destacado-1024x500.png")

ANCHO, ALTO = 1024, 500
PANEL = 341           # ancho del panel marron de la izquierda
CREMA = (251, 243, 231)
MARRON = (122, 78, 45)
TINTA_SUAVE = (122, 106, 88)

# Medidas sacadas del destacado original, para que el nuevo caiga donde el viejo.
X = 402
Y_TITULO_1, Y_TITULO_2, Y_SUB = 172, 253, 353
TAM_TITULO = 70

TEXTOS = {
    "es": ("Crónicas", "del Apetito", "Anota lo que comes, sin llevar la cuenta", 29),
    "en": ("Appetite", "Chronicles", "Log what you eat, without keeping count", 28),
}


def generar(idioma: str) -> str:
    linea1, linea2, sub, tam_sub = TEXTOS[idioma]

    im = Image.open(BASE).convert("RGB")
    d = ImageDraw.Draw(im)
    # Se borra el lado del texto entero: el panel y el tenedor se conservan.
    d.rectangle([PANEL, 0, ANCHO, ALTO], fill=CREMA)

    f_tit = ImageFont.truetype(FUENTE, TAM_TITULO)
    f_sub = ImageFont.truetype(FUENTE, tam_sub)
    d.text((X, Y_TITULO_1), linea1, font=f_tit, fill=MARRON)
    d.text((X, Y_TITULO_2), linea2, font=f_tit, fill=MARRON)
    d.text((X, Y_SUB), sub, font=f_sub, fill=TINTA_SUAVE)

    ancho_max = max(d.textlength(t, font=f) for t, f in
                    ((linea1, f_tit), (linea2, f_tit), (sub, f_sub)))
    if X + ancho_max > ANCHO - 40:
        raise SystemExit(f"El texto llega a {X + ancho_max:.0f} px: se come el margen derecho.")

    destino = os.path.join(RAIZ, "play-store", "graficos", idioma, "destacado-1024x500.png")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    im.save(destino)
    return destino


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--idioma", choices=sorted(TEXTOS), default="es")
    args = p.parse_args()
    print(generar(args.idioma))
