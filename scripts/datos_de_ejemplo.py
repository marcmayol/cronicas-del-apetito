# -*- coding: utf-8 -*-
"""
Llena la base de un emulador con registros inventados, para hacer las capturas de Play.

Nada de lo que se ve en la ficha es de nadie: todo sale de aquí. Y como las capturas hay que
rehacerlas cada vez que cambia la interfaz, esto tiene que ser repetible — de ahí la semilla
fija, que hace que la misma llamada dé siempre los mismos registros.

Escribe un `.sql` que se mete en la app **de depuración** (la de release no es depurable, así
que `run-as` no vale con ella):

    python scripts/datos_de_ejemplo.py --idioma es --salida datos.sql
    adb push datos.sql /data/local/tmp/datos.sql
    adb shell "cat /data/local/tmp/datos.sql | run-as com.marcm.cronicasapetito.debug \\
        sqlite3 databases/cronicas.db"

Cubre 46 días hacia atrás desde `--hasta`, para que el mes anterior salga completo en la vista
Mes. Las horas se escriben en la zona del ordenador, así que el emulador tiene que ir en la
misma (`setprop persist.sys.timezone Europe/Madrid`) o los desayunos saldrán de madrugada.
"""

from __future__ import annotations

import argparse
import datetime as dt
import io
import random
import sys

TEXTOS = {
    "es": {
        "desayunos": [
            "Café con leche y una tostada con tomate",
            "Yogur con fruta y un puñado de nueces",
            "Tostada de aguacate y un café",
            "Bol de avena con plátano",
            "Té y dos galletas integrales",
        ],
        "comidas": [
            "Lentejas con verduras y una manzana",
            "Ensalada de pasta con atún",
            "Arroz con pollo y pimientos",
            "Crema de calabaza y una tortilla francesa",
            "Garbanzos con espinacas",
            "Pescado al horno con patatas",
            "Macarrones con tomate y queso",
        ],
        "cenas": [
            "Sopa de verduras y una tostada",
            "Revuelto de champiñones",
            "Ensalada mixta y una lata de sardinas",
            "Puré de patata y pollo a la plancha",
            "Tortilla de calabacín",
        ],
        "snacks": [
            "Un puñado de almendras",
            "Una manzana",
            "Un yogur",
            "Dos onzas de chocolate negro",
            "Un plátano",
        ],
        "animos": [
            "Tranquilo, buen día",
            "Algo cansado pero bien",
            "Con hambre a media tarde, aguanté",
            "Nervioso por el trabajo",
            "Contento, he dormido bien",
            "Un poco bajo de energía",
        ],
    },
    "en": {
        "desayunos": [
            "Coffee and toast with tomato",
            "Yoghurt with fruit and a handful of walnuts",
            "Avocado toast and a coffee",
            "Bowl of porridge with banana",
            "Tea and two wholemeal biscuits",
        ],
        "comidas": [
            "Lentils with vegetables and an apple",
            "Pasta salad with tuna",
            "Rice with chicken and peppers",
            "Pumpkin soup and a plain omelette",
            "Chickpeas with spinach",
            "Baked fish with potatoes",
            "Macaroni with tomato and cheese",
        ],
        "cenas": [
            "Vegetable soup and a slice of toast",
            "Scrambled eggs with mushrooms",
            "Mixed salad and a tin of sardines",
            "Mashed potato and grilled chicken",
            "Courgette omelette",
        ],
        "snacks": [
            "A handful of almonds",
            "An apple",
            "A yoghurt",
            "Two squares of dark chocolate",
            "A banana",
        ],
        "animos": [
            "Calm, good day",
            "A bit tired but fine",
            "Hungry mid-afternoon, held out",
            "Anxious about work",
            "Happy, slept well",
            "A little low on energy",
        ],
    },
}

DIAS = 46


def genera(idioma: str, hasta: dt.date) -> list[tuple[int, str, str, int | None]]:
    # Semilla fija: la gracia es que la ficha de cada idioma enseñe los mismos días.
    random.seed(7)
    t = TEXTOS[idioma]
    filas: list[tuple[int, str, str, int | None]] = []
    for delta in range(DIAS, -1, -1):
        dia = hasta - dt.timedelta(days=delta)

        def ts(h: int, m: int) -> int:
            return int(dt.datetime(dia.year, dia.month, dia.day, h, m).timestamp() * 1000)

        filas.append((ts(8, random.randint(5, 40)), random.choice(t["desayunos"]), "food", None))
        if random.random() < 0.7:
            filas.append((ts(11, random.randint(0, 45)), random.choice(t["snacks"]), "food", None))
        filas.append((ts(14, random.randint(0, 45)), random.choice(t["comidas"]), "food", None))
        if random.random() < 0.5:
            filas.append((ts(17, random.randint(30, 59)), random.choice(t["snacks"]), "food", None))
        filas.append((ts(21, random.randint(0, 30)), random.choice(t["cenas"]), "food", None))
        if random.random() < 0.6:
            filas.append((ts(19, random.randint(0, 50)), "", "walk", random.choice([20, 25, 30, 35, 40, 45, 50])))
        if random.random() < 0.45:
            filas.append((ts(22, random.randint(0, 40)), random.choice(t["animos"]), "mood", None))
        # El gimnasio, lunes, miércoles y viernes, con alguna falta: si saliera siempre no
        # parecería una semana de nadie.
        if dia.weekday() in (0, 2, 4) and random.random() < 0.75:
            filas.append((ts(18, random.randint(0, 30)), "yes", "gym", None))
    return filas


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--idioma", default="es", choices=sorted(TEXTOS))
    ap.add_argument("--hasta", default="", help="Último día, AAAA-MM-DD (por defecto, hoy)")
    ap.add_argument("--salida", default="datos.sql")
    args = ap.parse_args()

    hasta = dt.date.fromisoformat(args.hasta) if args.hasta else dt.date.today()
    filas = genera(args.idioma, hasta)

    sql = ["DELETE FROM meal_entries;"]
    for t, c, k, m in filas:
        c = c.replace("'", "''")
        sql.append(
            "INSERT INTO meal_entries (timestampMillis, content, kind, minutes, photoPath) "
            "VALUES (%d, '%s', '%s', %s, NULL);" % (t, c, k, "NULL" if m is None else m)
        )
    io.open(args.salida, "w", encoding="utf-8", newline="\n").write("\n".join(sql) + "\n")
    print("%d registros en %s" % (len(filas), args.salida))
    return 0


if __name__ == "__main__":
    sys.exit(main())
