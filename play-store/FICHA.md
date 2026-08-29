# Ficha de Google Play

Todo lo que hay que pegar en Play Console, listo. Los gráficos están en
`graficos/`.

---

## Identidad

**Nombre de la app** (máx. 30)

```
Crónicas del Apetito
```

*20 de 30. En inglés la app se llama Appetite Chronicles, pero el nombre de la
ficha no se traduce automáticamente: si más adelante quieres ficha en inglés,
se añade como traducción de la ficha en Play Console.*

**Categoría**: Salud y bienestar
**Etiquetas sugeridas**: diario de comidas, recordatorios, bienestar
**Web**: https://marcmayol.com/cronicas-del-apetito/
**Privacidad**: https://marcmayol.com/cronicas-del-apetito/privacidad.html
**Contacto**: incidencias en GitHub, y el correo que uses en Play Console

---

## Descripción corta (máx. 80)

```
Anota lo que comes sin llevar la cuenta: la app te pregunta a tu hora.
```

*70 de 80. Es lo único que se lee en los listados, así que dice la promesa
entera: no tienes que acordarte tú.*

---

## Descripción larga (máx. 4000)

```
Apuntar lo que comes funciona. El problema es acordarse de apuntarlo.

Crónicas del Apetito le da la vuelta: en vez de esperar a que te acuerdes, te
pregunta. Tú decides entre qué horas y cada cuánto, y cuando llega el aviso lo
anotas en un toque, sin abrir nada.


QUÉ PUEDES LLEVAR

Cuatro cosas, y solo las que quieras:

• Comida — qué has comido y cuánto, con foto del plato si te apetece
• Caminatas — los minutos que andas
• Estado de ánimo — cómo te sientes, cuando quieras anotarlo
• Gimnasio — si has ido, y cuántas veces por semana

Cada una se enciende o se apaga por separado. Si solo quieres llevar la comida,
la app solo te preguntará por la comida.


A TU HORARIO, NO AL MÍO

Eliges a qué hora empiezan los recordatorios, a qué hora terminan y cada cuánto
llegan: de cada media hora a cada seis. Si un día te sobran, los espacias o los
apagas. Y hay un botón de «me voy a dormir» que los calla hasta mañana.


PARA LLEVARLO A CONSULTA

Míralo por día, por semana o por mes, y filtra por fechas. Lo que estés viendo
se comparte tal cual, como imagen o como PDF, pensado para enseñárselo a quien
te esté acompañando. Cada tipo de registro lleva su símbolo además de su color,
así que se entiende también impreso en blanco y negro.


TUS DATOS SON TUYOS, Y SE NOTA

No hay cuenta, no hay registro, no hay servidor. Lo que anotas se queda en tu
móvil y no viaja a ninguna parte: la app ni siquiera pide permiso de internet.

Y como no hay nube que te guarde nada, la app trae su propia copia de seguridad:
desde Ajustes guardas un archivo con todo tu historial y tus fotos, donde tú
elijas, y lo recuperas igual de fácil.


ADEMÁS

• Español e inglés
• Tema claro y oscuro, el del móvil o el que elijas
• Cualquier registro se corrige o se borra, y borrar siempre se puede deshacer
• Sin anuncios, sin compras dentro de la app, sin nada que desbloquear

Crónicas del Apetito es una herramienta para apuntar lo que comes, pensada para
acompañar el trabajo con un profesional. No es un dispositivo médico, no da
consejo sanitario y no sustituye a nadie.
```

*Unos 1900 caracteres de 4000.*

---

## Gráficos (en `graficos/`)

| Qué | Archivo | Tamaño |
|---|---|---|
| Icono | `icono-512.png` | 512×512 |
| Destacado | `destacado-1024x500.png` | 1024×500 |
| Capturas | `captura-1-dia.png` … `captura-4-ajustes.png` | 1080×2400 |

Play pide **mínimo 2 capturas** de teléfono; van cuatro. Llevan datos inventados
a propósito: nada de lo que se ve es de nadie.

---

## Antes de subir

- [ ] Comprobar si la cuenta es anterior o posterior al **13-nov-2023**. Si es
      posterior: **12 testers con opt-in continuo durante 14 días** antes de
      poder pasar a producción.
- [ ] Subir `app/build/outputs/bundle/playRelease/app-play-release.aab`.
- [ ] Play App Signing: la keystore actual sirve como clave de subida. El
      paquete de Play (`com.marcm.cronicasapetito.play`) es distinto al de
      DracApps, así que no hay conflicto de firmas entre los dos canales.
- [ ] Rellenar Data safety con `data-safety.md`.
- [ ] Cuestionario de clasificación de contenido.
- [ ] Marcar que **no** contiene anuncios.
