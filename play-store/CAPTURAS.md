# Las capturas de la ficha

Seis por idioma, en `graficos/es/` y `graficos/en/`, listas para subir.

Las cuatro de agosto (`graficos/captura-*.png`) quedan **sustituidas**: eran pantallazos
crudos del móvil, con dos problemas que el informe de testers señaló y uno que ellos no
vieron.

| Problema | Cómo se arregla aquí |
|---|---|
| Eran 1080×2400, o sea **2,22:1**, y Play no admite más de 2:1 | El compositor las deja en 1080×1920 |
| En el listado se ven a unos 200 px: un pantallazo entero es una mancha ilegible | Cada una lleva un titular grande que dice qué hace esa pantalla |
| Los datos que salían eran inventados a mano, distintos en cada captura | Los pone un generador con semilla fija, así que ES y EN enseñan **los mismos días** |

## Rehacerlas

Hace falta un emulador (o un móvil de pruebas) con el **build de depuración**: la release no
es depurable y `run-as` no funciona con ella.

```bash
# 1. El emulador, en el idioma que toque y en la zona horaria del ordenador
adb shell settings put system system_locales es-ES     # o en-US
adb shell cmd locale set-app-locales com.marcm.cronicasapetito.debug --user 0 --locales es-ES
adb root && adb shell "setprop persist.sys.timezone Europe/Madrid"
adb shell "date 090922152026.00"     # una hora de noche: así el día sale entero

# 2. Registros de ejemplo (46 días hacia atrás, para que el mes anterior salga lleno)
python scripts/datos_de_ejemplo.py --idioma es --hasta 2026-09-09 --salida datos.sql
adb push datos.sql /data/local/tmp/datos.sql
adb shell "cat /data/local/tmp/datos.sql | run-as com.marcm.cronicasapetito.debug \
    sqlite3 databases/cronicas.db"

# 3. Las seis pantallas, a play-store/capturas-crudas/<idioma>/
#    01-aviso  02-anotar  03-dia  04-semana  05-mes  06-recordatorios

# 4. Montarlas
python scripts/generar_capturas_play.py --idioma es
```

El **aviso** (01) se dispara a mano, sin esperar a la hora en punto:

```bash
adb shell am broadcast -n com.marcm.cronicasapetito.debug/\
com.marcm.cronicasapetito.notifications.MealAlarmReceiver
```

Sale como banner sobre la lista, que es justo lo que se quiere enseñar: el aviso **y** la app
debajo. Hay que capturar en los 2-3 segundos siguientes, antes de que se recoja.

## Tres cosas que costaron

- **El idioma de la app va por su cuenta.** Android recuerda un idioma por aplicación: cambiar
  el del sistema no cambia el de la app, y salían capturas con la interfaz en un idioma y los
  registros en otro. Se fija con `cmd locale set-app-locales`, y se comprueba con
  `get-app-locales` **después** de cada reinicio.
- **SystemUI se queda con el idioma de arranque.** El texto del aviso («ahora» / «now») y el
  nombre bajo el icono los pinta SystemUI, no la app: hasta que no se reinicia el emulador
  entero con `system_locales` puesto, el banner sale en inglés aunque todo lo demás esté en
  castellano.
- **La barra de estado del emulador se recorta** (100 px). Llevaba la hora del emulador, la
  batería al 100 % y los iconos de otras notificaciones del sistema, que no son de la app.

## Lo que enseña cada una

| # | Pantalla | Por qué está |
|---|---|---|
| 01 | El aviso, sobre la lista | Es la promesa entera: no te acuerdas tú |
| 02 | «¿Qué quieres anotar?» | Los cuatro registros, y que se eligen |
| 03 | Vista Día | Que se escribe con tus palabras, sin listas ni calorías |
| 04 | Vista Semana | Los totales, que es lo que engancha al mirar atrás |
| 05 | Vista Mes | El color **con** su símbolo, la regla del sistema |
| 06 | Ajustes de recordatorios | Que los horarios los pones tú |
