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

## Lo que queda

1. Que corran los **14 días** con los testers instalados.
2. Entonces, **Solicitar acceso a producción** desde el panel de Play Console.
3. Mirar los informes de los testers y arreglar lo que salga (para eso están).

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
