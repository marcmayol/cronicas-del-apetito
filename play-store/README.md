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

## Lo que sigue necesitando manos

1. **Subir el AAB.** El archivo pesa 10,5 MB y el puente del navegador de
   Claude corta en 10 MB, así que hay que soltarlo a mano en
   Prueba cerrada → Alpha → la versión en borrador:
   `app/build/outputs/bundle/playRelease/app-play-release.aab`
2. **Los 12 testers.** Play exige 12 con opt-in continuo durante 14 días antes
   de dar acceso a producción. Solo hay una lista («family», 1 usuario), así
   que hay que decidir a quién se invita. Es lo único que puede retrasar esto
   semanas.
3. Revisar y enviar la versión a revisión.
