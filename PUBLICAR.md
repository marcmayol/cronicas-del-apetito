# Publicar una versión

Distribución fuera de Play Store: APK **release firmado** en GitHub Releases +
manifiesto `docs/updates.json` en GitHub Pages. La app se auto-actualiza (módulo
[`actualizador`](actualizador/README.md)).

## Preparativos (una sola vez)

### 1. Keystore de producción (fuera del repo)

La firma **debe ser estable**: todas las versiones se firman con la MISMA keystore, o
la actualización por `PackageInstaller` no funciona. Créala una vez y guárdala bien
(sin ella no podrás volver a actualizar la app):

```bash
keytool -genkeypair -v \
  -keystore C:/ruta/segura/cronicas-release.jks \
  -alias cronicas -keyalg RSA -keysize 2048 -validity 10000
```

Copia `keystore.properties.example` a `keystore.properties` (gitignored) y rellénalo,
o exporta las variables `CRONICAS_STORE_FILE`, `CRONICAS_STORE_PASSWORD`,
`CRONICAS_KEY_ALIAS`, `CRONICAS_KEY_PASSWORD`. **Nunca** se versiona la keystore ni las
contraseñas.

### 2. Repositorio público + GitHub Pages

El manifiesto y los assets se sirven sin autenticación, así que el repo debe ser
**público** y Pages activado **desde la rama `main`, carpeta `/docs`**:

```bash
gh repo edit marcmayol/cronicas-del-apetito --visibility public
# Tras el primer manifiesto commiteado en docs/, activa Pages (/docs) en
# Settings → Pages, o:
gh api -X POST repos/marcmayol/cronicas-del-apetito/pages \
  -f 'source[branch]=main' -f 'source[path]=/docs'
```

En la **primera** release, el orden es: subir versión → ejecutar el script (crea la
Release y commitea `docs/updates.json`) → activar Pages `/docs` → comprobar la URL.
A partir de la segunda, Pages ya está activo y el script verifica la URL solo.

## Publicar una versión nueva

1. Sube `versionCode` (y `versionName`) en `app/build.gradle.kts`. **El `versionCode`
   siempre incrementa**; es lo único que decide si hay novedad.
2. Prepara sin publicar y revisa el manifiesto:

   ```bash
   python scripts/publicar_release.py --dry-run --notas "Qué cambia…"
   ```

3. Publica:

   ```bash
   python scripts/publicar_release.py --notas "Qué cambia…"
   ```

El script: construye el APK release firmado, verifica que el `versionCode` del APK
(leído con `aapt2`) coincide con el declarado y con el del manifiesto y que el
`sha256` es el del APK real (**aborta si algo no cuadra**), crea la Release con `gh`
subiendo el APK, commitea y pushea `docs/updates.json`, y verifica que la URL pública
ya sirve el `versionCode` nuevo (reintentando por la caché del CDN).

## Migración de datos debug → release (solo la primera vez)

La app venía instalándose como *debug* (`…cronicasapetito.debug`). El release limpio
usa `…cronicasapetito` (otra app para el sistema), así que sus datos no migran solos.
Desde la v2.2 esto lo hace la propia app: **Ajustes → Tus datos → Guardar una
copia** escribe un ZIP con todo el historial y las fotos donde tú elijas, y
**Recuperar** lo vuelve a meter. No hace falta cable, ni adb, ni que la app sea
debuggable — que es justo el problema de la app pública: `run-as` no funciona
con ella.

Además, al abrir una versión que migra la base, la app deja una copia del
archivo tal cual estaba en `cronicas.db.antes-de-v<N>`, dentro de su propia
carpeta.

Para el paquete debug, que sí es debuggable, sigue valiendo la vía corta:

```bash
adb exec-out run-as com.marcm.cronicasapetito.debug cat databases/cronicas.db > cronicas-backup.db
```

## Dos variantes, dos caminos

La app se distribuye por dos sitios con reglas opuestas, así que el mismo
código sale en dos sabores:

| | `fuera` | `play` |
|---|---|---|
| Dónde | DracApps y marcmayol.com | Google Play |
| Paquete | `com.marcm.cronicasapetito` | `com.marcm.cronicasapetito.play` |
| Se actualiza | sola, con `:actualizador` | desde la tienda |
| Permisos | 8 | 2 (notificaciones y arranque) |
| Se construye con | `assembleFueraRelease` | `bundlePlayRelease` |

**Son dos apps distintas a ojos de Android, y a propósito.** Con el mismo
`applicationId`, la clave que genera Play App Signing chocaría con la keystore
de DracApps: quien tuviera una no podría actualizar a la otra. Separadas, cada
una lleva su firma y **se pueden tener las dos instaladas a la vez** —útil para
comparar—, con la pega de que no comparten datos: para pasar de una a otra se
usa la copia de seguridad de Ajustes.

Lo que la variante de Play **no** lleva, y por qué:

- **El auto-actualizador.** La política de Device and Network Abuse prohíbe que
  una app se actualice por una vía que no sea Play, y `REQUEST_INSTALL_PACKAGES`
  no se puede usar para eso. Es motivo de retirada, no un aviso.
- **`SCHEDULE_EXACT_ALARM`.** Google la reserva para alarmas y temporizadores.
  La app ya cae a `setAndAllowWhileIdle` sin ella, así que pedirla sería
  arriesgar el rechazo por algo que no necesita.

Para probar la variante de Play sin publicarla, el canal de **pruebas internas**
de Play instala y actualiza igual que el actualizador propio, y además el tiempo
en pruebas cerradas cuenta para el requisito de los 12 testers.

## Verificación de desarrolladores: el paquete de fuera también hay que registrarlo

Play avisó el **8-sep-2026**: antes del **30 de septiembre de 2026** hay que
registrar en *Verificación de desarrolladores de Android* el nombre de paquete y
las claves de firma de **todas** las apps que se distribuyan en Android, no solo
las de Play. Lo que no se registre deja de poder instalarse en dispositivos
Android certificados de determinados países.

El 9-sep la cuenta tenía registrados **tres** paquetes, los tres de Play:
`com.marcm.cronicasapetito.play` (3 claves verificadas),
`com.marcm.grimoriodepociones` y `com.marcmayol.buildingmyfuturo`.

**`com.marcm.cronicasapetito` —el de DracApps— no está.** Es otro nombre de
paquete y otra firma, así que la verificación de la variante `play` no le sirve
de nada. Lo mismo vale para el resto del catálogo de DracApps.

Se registra en Play Console → *Verificación de desarrolladores de Android* →
*Registrar nombre de paquete*, y la huella SHA-256 de la keystore de release se
saca con:

```bash
keytool -list -v -keystore C:/ruta/segura/cronicas-release.jks -alias cronicas
```
