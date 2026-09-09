# Publicar en Google Play

Lo necesario para subir **Crónicas del Apetito** a Play, ya preparado.

| Archivo | Qué es |
|---|---|
| `AVISO-CATEGORIA.md` | **Léelo primero.** Por qué la categoría no puede ser Salud |
| `PRODUCCION.md` | El formulario de acceso a producción, respondido de verdad |
| `CAPTURAS.md` | Cómo se hacen las capturas de la ficha, y por qué no valen los pantallazos |
| `FICHA.md` | Nombre, descripciones corta y larga, categoría y enlaces, listos para pegar |
| `data-safety.md` | El formulario de seguridad de los datos, respondido |
| `graficos/` | Icono 512×512, destacado 1024×500 por idioma y seis capturas por idioma |

El bundle se genera con:

```
gradlew :app:bundlePlayRelease
→ app/build/outputs/bundle/playRelease/app-play-release.aab
```

Es la variante `play`: paquete `com.marcm.cronicasapetito.play`, sin
auto-actualizador y con dos permisos. Ver `PUBLICAR.md` en la raíz para por qué
son dos apps distintas.

## Estado en Play Console (30-ago-2026)

La app existe: `com.marcm.cronicasapetito.play`, ID 4975165717784640622, en
estado **Borrador**. Todo lo que se podía dejar hecho, está hecho:

| Apartado | Cómo quedó |
|---|---|
| Ficha de Play Store | Nombre, descripción breve (70/80) y larga (2064/4000), icono, destacado y 4 capturas |
| Categoría | **Estilo de vida** — no Salud, ver `AVISO-CATEGORIA.md` |
| Contacto | correo + `marcmayol.com/cronicas-del-apetito/` |
| Política de privacidad | `…/privacidad.html` |
| Datos de inicio de sesión | No hay nada restringido |
| Anuncios | No contiene |
| **Salud** | «Mi aplicación no tiene ninguna función de salud» |
| Apps gubernamentales | No |
| Funciones financieras | Ninguna |
| Audiencia objetivo | 13-15, 16-17 y 18+ |
| Seguridad de los datos | **No se declara ninguna recogida de datos** |
| Clasificación de contenido | Todo «No» → PEGI 3, ESRB Para todos, Classind Todas las edades |
| Prueba cerrada (Alpha) | Canal creado, **177 países**, correo de comentarios puesto |

## En pruebas (30-ago-2026)

**Google la aprobó el mismo día.** El canal Prueba cerrada – Alpha está activo
con la **2.4.1 (17)** en 177 países, y los dos enlaces ya funcionan:

| Para | Enlace |
|---|---|
| Apuntarse como tester (web) | `https://play.google.com/apps/testing/com.marcm.cronicasapetito.play` |
| La ficha en Play (Android) | `https://play.google.com/store/apps/details?id=com.marcm.cronicasapetito.play` |

Los testers los gestiona el grupo `testers-community@googlegroups.com`, puesto
en *Grupos de Google* (no en *Listas de correo*, que es donde no vale).

**Testers Community**: enviado con el crédito Pro — 25 testers e informe ASO.
En su panel: *Private Testing Pro · Day 0 / 16 · reports pending*. Su garantía
dice que si se completan los 14 días y Google no da acceso a producción,
devuelven el importe.

## Los informes llegaron (9-sep-2026)

Tres documentos de Testers Community: la respuesta de los testers, una auditoría ASO
de la ficha y una plantilla para el formulario de acceso a producción.

**Ni un fallo.** Ningún cierre inesperado ni ningún error de funcionamiento en ningún
dispositivo ni versión de Android. Lo que trajeron fue otra cosa:

| Lo que dijeron | Qué se hizo |
|---|---|
| Las capturas son pantallazos sin explicar nada | Seis nuevas con titular, en ES y EN. Ver `CAPTURAS.md` |
| A la descripción le faltan las palabras que la gente busca | Ficha reescrita, y ficha en inglés. Ver `FICHA.md` |
| No hay forma de valorar la app desde dentro | «Valorar la app» en Ajustes, que abre la ficha (v2.5) |
| «No hay política de privacidad ni condiciones» | Falso en Play, cierto dentro: ahora están en Ajustes, y las condiciones se han escrito (v2.5) |
| Atrás cierra la app sin avisar | Ahora pide pulsarlo dos veces (v2.5) |

La **auditoría ASO daba 49/100** (grado F): la palabra clave no aparecía ni en el título ni
en las descripciones, y la larga se quedaba en 2.023 caracteres. Todo eso está corregido en
`FICHA.md`, que además explica lo único que no se hizo y por qué.

Y revisando la app en inglés para las capturas salieron **dos fallos de traducción** que
nadie había visto: los totales de la vista Semana y las iniciales del calendario seguían en
castellano. Arreglados también en la 2.5.

## La 2.5.1 está en el canal (9-sep-2026, 17:06)

Google la aprobó el mismo día, otra vez. En Prueba cerrada – Alpha:

| Qué | Cómo quedó |
|---|---|
| Versión | **19 (2.5.1)**, «Disponible para determinados testers», 177 países |
| Ficha en español | Título, descripción breve y larga nuevas, y las 6 capturas con titular |
| Ficha en inglés | Creada entera: `Appetite Chronicles: Food Log`, sus dos descripciones y sus 6 capturas |
| Destacado en inglés | Enviado a revisión aparte, ver abajo |

La ficha inglesa heredaba el **gráfico destacado en castellano**, así que anunciaba
«Crónicas del Apetito» sobre una app que allí se llama Appetite Chronicles. Se ha
hecho el suyo (`graficos/en/destacado-1024x500.png`) y enviado a revisión; el
generador está en `scripts/generar_destacado_play.py` y se explica en `FICHA.md`.

## Lo que queda

1. Que corran los **14 días** con los testers instalados. El 9-sep Play contaba
   **12 testers durante 9 días sin interrupciones**, así que la ventana se cumple
   sobre el **13-sep-2026** y hasta entonces el botón sigue apagado.
2. Entonces, **Solicitar acceso a producción** y rellenar el formulario con `PRODUCCION.md`.

## Si algún día lleva anuncios

Es la vía de financiación que se ha planteado. No es marcar una casilla: hay
que tocar cuatro sitios a la vez o Play bloquea la versión.

- Permiso `com.google.android.gms.permission.AD_ID` en el manifiesto
- Contenido de la aplicación → **ID de publicidad**: cambiar a «Sí»
- Contenido de la aplicación → **Anuncios**: «Contiene anuncios»
- **Seguridad de los datos**: dejaría de ser «no se recopila nada», porque la
  red publicitaria sí recopila
- Y la política de privacidad de `docs/privacidad.html`, que hoy promete
  justamente lo contrario
