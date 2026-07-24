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
Para conservar el historial de un móvil, con él conectado por adb:

```bash
# Backup de la BD del paquete debug (requiere app debuggable)
adb exec-out run-as com.marcm.cronicasapetito.debug cat databases/cronicas.db > cronicas-backup.db
# …instalar el release y, si se quiere, restaurar en el paquete release.
```

> La app pública arranca con base de datos **limpia**; la copia local queda para
> reinyectar el historial si se desea.
