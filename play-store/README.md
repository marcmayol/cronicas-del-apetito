# Publicar en Google Play

Lo necesario para subir **Crónicas del Apetito** a Play, ya preparado.

| Archivo | Qué es |
|---|---|
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

## Lo que sigue necesitando manos

1. **La fecha de la cuenta de Play.** Si es posterior al 13-nov-2023, hacen
   falta 12 testers con opt-in continuo durante 14 días antes de producción.
   Es lo único que puede retrasar esto semanas.
2. Subir el AAB y decidir Play App Signing (la keystore actual vale como clave
   de subida).
3. Clasificación de contenido y confirmar que no hay anuncios.
