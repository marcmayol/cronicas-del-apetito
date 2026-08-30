# Publicar en Google Play

Lo necesario para subir **Crónicas del Apetito** a Play, ya preparado.

| Archivo | Qué es |
|---|---|
| `AVISO-CATEGORIA.md` | **Léelo primero.** Por qué la categoría no puede ser Salud |
| `FICHA.md` | Nombre, descripciones corta y larga, categoría y enlaces, listos para pegar |
| `data-safety.md` | El formulario de seguridad de los datos, respondido |
| `graficos/` | Icono 512×512, gráfico destacado 1024×500 y cuatro capturas |

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

## Enviado a revisión (30-ago-2026)

Los 15 cambios están **en revisión de Google**: ficha, contenido, y la versión
**2.4.1 (17)** en el canal Prueba cerrada – Alpha, activo en 177 países.

Los testers los gestiona el **grupo de Google `testers-community@googlegroups.com`**
(bajo *Grupos de Google*, no *Listas de correo*, como pide Testers Community).

El enlace de opt-in aparece en Play Console cuando Google aprueba la revisión.
Su forma es siempre la misma:

```
https://play.google.com/apps/testing/com.marcm.cronicasapetito.play
```

## Lo que queda

1. **Esperar la aprobación** (hasta 7 días, normalmente menos). Hasta entonces
   el enlace no funciona.
2. Terminar el envío en testerscommunity.com (pasos *App Details* y
   *Review & Submit*).
3. Los 14 días de prueba con 12 testers antes de pedir acceso a producción.

## Testers Community (preparado, sin enviar)

El envío en testerscommunity.com está relleno hasta el último paso. **No se ha
enviado a propósito**: gastaría el crédito Pro mientras el enlace de testing
todavía devuelve 404, porque Play sigue revisando. En cuanto Play apruebe, se
entra y se pulsa *Submit App*.

| Campo | Valor |
|---|---|
| App name | Crónicas del Apetito |
| Plan | **Pro** · 25 testers · informe ASO — hay **1 crédito ya comprado** |
| Testing URL | `https://play.google.com/apps/testing/com.marcm.cronicasapetito.play` |
| App icon | `graficos/icono-512.png` |
| ¿Hecha con PWABuilder/Bubblewrap? | **No** (es Kotlin nativo; lo preguntan antes de gastar el crédito) |

Notas para los testers (en inglés y español, porque la app es bilingüe):

> No login needed: the app opens straight away, no account and no password. On
> first launch there is a three-step welcome where you pick what you want to be
> asked about (food, walks, gym, mood) and between which hours. Everything stays
> on the phone. The app is in English and Spanish, and follows the phone's
> language.

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
