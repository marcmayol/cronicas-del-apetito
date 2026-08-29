# Data safety — qué responder

El formulario de seguridad de los datos de Play, respondido. Es corto porque la
app no recoge nada: la variante de Play **ni siquiera declara el permiso de
internet**, así que no hay forma de que envíe información a ninguna parte.

Se puede comprobar en el propio AAB antes de subirlo:

```
aapt2 dump permissions app-play-release.apk
  → android.permission.POST_NOTIFICATIONS
  → android.permission.RECEIVE_BOOT_COMPLETED
  → com.marcm.cronicasapetito.play.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

El tercero lo genera Android solo, para sus propios receptores internos; no es
nuestro y no se declara en ninguna parte.

---

## Recopilación y uso compartido

| Pregunta | Respuesta |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos obligatorios? | **No** |

Con eso el formulario prácticamente termina. El matiz que importa: Play define
«recopilar» como **enviar los datos fuera del dispositivo**. Lo que la app
guarda en el móvil y no transmite no cuenta como recopilación, y esta app no
transmite nada.

Los datos que el usuario escribe —comidas, ánimo, fotos— viven en el
almacenamiento privado de la app. Salen solo si él los saca a mano: al
compartir un PDF o una imagen, o al guardar una copia de seguridad eligiendo
destino. Eso es una acción suya con una app de su elección, no una
transmisión de la nuestra.

---

## Prácticas de seguridad

| Pregunta | Respuesta | Por qué |
|---|---|---|
| ¿Se cifran los datos en tránsito? | **No procede** | No hay tránsito: la app no tiene red |
| ¿Se puede pedir la eliminación de los datos? | **Sí** | Cada registro se borra desde la app; desinstalar o «Borrar datos» lo elimina todo, y no hay copia en ningún servidor |
| ¿Cumple la política de Familias? | No procede | La app no está dirigida a menores |

---

## Si preguntan por el permiso de notificaciones

No es un dato personal, es un permiso de ejecución, y no entra en este
formulario. Si en la revisión piden justificarlo: los recordatorios para anotar
la comida son la función principal de la app.

---

## Lo que NO hay que marcar

Merece la pena decirlo explícitamente, porque es fácil marcar de más:

- Ni **información personal** (no hay nombre, correo ni identificadores).
- Ni **información de salud o forma física**, pese a la categoría. Ese apartado
  es para datos que se **recopilan**; aquí no salen del dispositivo.
- Ni **fotos** (no se envían).
- Ni **ID de dispositivo**, **ubicación**, **contactos** o **actividad de la
  app**: nada de eso se toca.

Marcar de más no es más prudente: obliga a declarar prácticas que no existen y
a explicarlas en una revisión.

---

## Declaraciones de permisos sensibles

Ninguna. La variante de Play evita a propósito los dos permisos que la habrían
requerido:

- **`REQUEST_INSTALL_PACKAGES`** — no se enlaza el módulo de auto-actualización.
  Play prohíbe que una app se actualice fuera de la tienda.
- **`SCHEDULE_EXACT_ALARM`** — Google la reserva para alarmas y temporizadores.
  La app usa alarmas inexactas, que para un recordatorio de comida sobran.
