# Crónicas del Apetito — Brief de diseño

**Para:** el diseñador
**De:** Marc Mayol
**Versión de la app documentada:** 1.6 (versionCode 7)
**Fecha:** 17 de agosto de 2026

Este documento tiene dos partes:

1. **Lo que ya existe** — todas las pantallas de la app, una por una, para que puedas rediseñarlas sin abrir el código.
2. **Lo que hay que diseñar nuevo** — vista por semana, vista por mes, filtro por rango de fechas, y compartir cualquiera de esas vistas.

---

## 1. Qué es la app y para quién

Crónicas del Apetito es una app Android **personal**, no comercial. La uso yo, y su motivo es concreto: acompañar el trabajo con mi psicóloga sobre ansiedad y comida. La app me pregunta a lo largo del día si he comido algo, y voy dejando un registro de lo que como, cuánto camino, si voy al gimnasio y cómo me siento.

Tres consecuencias de diseño que conviene tener claras desde el principio:

- **El registro se comparte con una profesional.** Lo que se exporta lo lee otra persona, a veces impreso. Tiene que ser legible fuera de la pantalla.
- **No es una app de dieta ni de calorías.** No hay objetivos, ni rachas, ni "te has pasado". Nada de gamificación culpabilizadora. El tono es de cuaderno de notas tranquilo, no de entrenador.
- **Todo es local.** No hay cuentas, no hay nube, no hay login. Se guarda en el móvil y punto.

El nombre y el tono actual ("Crónicas", el icono de luna para irse a dormir, los mensajes tipo *"Buenas noches. Te avisaré mañana a las 8:00 🌙"*) apuntan a un registro cálido y amable. Ese tono me gusta y me gustaría conservarlo, pero el aspecto visual está sin trabajar: es Material 3 por defecto con cuatro colores marrones encima. Ahí es donde entras tú.

---

## 2. Restricciones técnicas (esto no es negociable)

| | |
|---|---|
| Plataforma | Android nativo, **Jetpack Compose + Material 3** |
| Versión mínima | Android 8.0 (API 26); objetivo API 34 |
| Formato | Solo móvil, **solo vertical**. No hay tablet ni horizontal. |
| Tema | Hoy solo hay **tema claro**. Si propones modo oscuro, dilo aparte: es trabajo extra, no lo des por hecho. |
| Conectividad | **Sin red**. Todo offline. No hay avatares remotos, ni fuentes de Google Fonts cargadas en caliente, ni imágenes de stock descargadas. |
| Fuentes | Si propones una tipografía, tiene que poder empaquetarse en el APK (licencia libre: OFL, Apache…). |
| Iconos | Ahora se usan los `Icons.Filled` de Material. Si quieres iconos propios, entrégalos como **SVG/vector**, no PNG. |
| Accesibilidad | La app tiene que aguantar el **zoom de fuente del sistema a 1.3× y 1.5×** sin romperse. Nada de alturas fijas que corten texto. |
| Táctil | Zonas tocables de 48 dp mínimo. |

---

## 3. Sistema visual actual (punto de partida)

Colores del tema (Material 3 `lightColorScheme`):

| Rol | Hex | Uso |
|---|---|---|
| primary | `#7A4E2D` | Marrón, botones principales, cabeceras de día |
| onPrimary | `#FFFFFF` | Texto sobre primary |
| secondary | `#B7763D` | Marrón claro / tostado |
| background | `#FFF8EE` | Crema de fondo |
| surface | `#FFF1DC` | Crema más cálido, tarjetas |
| onBackground / onSurface | `#2A1B0E` | Marrón muy oscuro, texto |

Colores por tipo de registro (hoy se usan solo en la etiqueta de texto de cada tarjeta):

| Tipo | Hex | Etiqueta |
|---|---|---|
| Comida | `#7A4E2D` | "Comida" |
| Caminata | `#2E7D32` (verde) | "Caminata" |
| Estado de ánimo | `#6A1B9A` (morado) | "Estado de ánimo" |
| Gimnasio | `#1565C0` (azul) | "Gimnasio" |

Tipografía: la de Material 3 por defecto (Roboto). Sin personalizar.

**Aviso importante sobre estos colores:** los cuatro colores de tipo son de la paleta de Material, elegidos deprisa, y no combinan especialmente bien con el marrón/crema del tema. Tienes libertad total para rehacerlos. Solo pido que se mantenga **la distinción entre los cuatro tipos** y que funcionen también **impresos en blanco y negro** (ver §5.4), o sea que la diferencia no puede depender únicamente del color.

---

## 4. Inventario de pantallas actuales

### P1 — Principal: el historial

Es la pantalla que se abre al arrancar la app. Lo que hay hoy, de arriba abajo:

**Barra superior**
- Título: "Crónicas del Apetito"
- Icono ⚙️ → abre Ajustes (P8)
- Icono 🌙 (luna) → abre el diálogo "Me voy a dormir" (P6)
- Botón contorneado con icono de PDF + texto "Exportar PDF" → abre el diálogo de exportación (P7)

> La barra va **muy justa de espacio**: tres acciones y un título largo. Es uno de los puntos que peor está y, con las nuevas funciones (§5), va a tener que reorganizarse sí o sí.

**Banner de actualización** (solo aparece a veces, encima de todo)
La app se actualiza sola, sin Play Store, y avisa con un banner no bloqueante:
- "Nueva versión 1.7" + notas de la versión + botón "Actualizar"
- "Descargando actualización… 45%" con barra de progreso
- "Verificando la descarga…" / "Instalando…" con barra indeterminada

**Banner de modo dormir** (solo si está activo)
- Icono de luna + "Modo a dormir activo · los avisos volverán a las 8:00" + botón de texto "Ya estoy despierto"

**Lista del historial**
Agrupada por día, con el día más reciente arriba:
- Cabecera de día: "Lunes 17 de agosto 2026"
- Debajo, una tarjeta por registro, de más reciente a más antiguo:
  - Hora en negrita a la izquierda: "14:30"
  - Etiqueta del tipo, en su color: "Comida"
  - Contenido del registro (ver §4.9 para saber qué contiene cada tipo)
  - Si hay foto: una fila pulsable "Ver foto" / "Ocultar foto" con una flecha, que despliega la imagen dentro de la tarjeta (máximo 280 dp de alto)

**Estado vacío**
Texto centrado: *"Aún no has anotado nada. Cuando llegue una notificación, pulsa Sí para empezar."*

**Botón flotante**
FAB extendido abajo a la derecha: icono + y texto "Anotar" → abre P2.

---

### P2 — Diálogo "¿Qué quieres anotar?"

Se abre al pulsar el FAB. Tres botones a ancho completo, apilados:
- **Comida** (botón sólido, icono de plato) → P3
- **Caminata** (contorneado, icono de persona andando) → P4, entrando directamente en el paso de los minutos
- **Gimnasio** (contorneado, icono de mancuerna) → P5

Abajo: "Cancelar".

---

### P3 — Anotar comida

Pantalla completa, con barra superior "Anotar comida". Se llega desde P2 o desde la notificación (§4.10). Contenido, en orden y con scroll:

1. **Fila de fecha y hora**: icono de reloj, "Fecha y hora" en pequeño, debajo "Lunes 17 de agosto, 14:30", y a la derecha un botón "Cambiar" que abre el selector de fecha y luego el de hora. *No se permiten fechas futuras* (una comida olvidada siempre es de hoy o de antes).
2. **"¿Qué has comido?"** — campo de texto de 4 líneas mínimo. Placeholder: *"¿Qué has comido? Recuerda anotar la cantidad (ej.: 1 o 2 platos de macarrones)"*.
3. **"Foto del plato (opcional)"** — tres estados:
   - Sin foto: dos botones al 50%, "Cámara" y "Galería".
   - Procesando: spinner + "Guardando foto…"
   - Con foto: la miniatura a ancho completo (máx. 240 dp) y debajo, alineado a la derecha, "Quitar foto".
4. **"¿Cómo te has sentido? (opcional)"** — campo de texto de 3 líneas. Placeholder: *"Puedes dejarlo en blanco si no quieres anotarlo"*. Si se rellena, se guarda como un registro **aparte** de tipo "Estado de ánimo" con la misma hora.
5. Abajo a la derecha: "Cancelar" y "Guardar". Guardar está deshabilitado si el campo de comida está vacío o si la foto aún se está procesando.

---

### P4 — Caminata y estado de ánimo

Pantalla completa con barra superior "Seguimiento". Son **tres pasos dentro de la misma pantalla** (no hay indicador de progreso, y quizá debería haberlo):

- **Paso 1 — "¿Has ido a caminar?"** (solo si se entra desde la notificación): dos botones al 50%, "Sí" y "No". Con "No" se cierra sin guardar nada.
- **Paso 2 — "¿Cuánto tiempo has caminado?"**: la misma fila de fecha y hora de P3, un campo numérico "Minutos", y abajo "Atrás"/"Cancelar" + "Continuar" (deshabilitado si los minutos son 0).
- **Paso 3 — "¿Cómo te has sentido?"**: texto explicativo *"Si quieres, anota cómo te has sentido. Es opcional."*, campo de 4 líneas con placeholder *"Describe cómo te sientes ahora mismo"*, y abajo "Cancelar" + un botón que dice "Omitir" si el campo está vacío o "Guardar" si tiene texto.

---

### P5 — Diálogo de gimnasio

"¿Has ido al gimnasio hoy?" con dos botones a ancho completo: "Sí" (sólido, icono de mancuerna) y "No". Abajo, "Cancelar". Se guarda directamente, sin más pasos.

---

### P6 — Diálogo "Me voy a dormir"

Icono de luna, título "¿Te vas a dormir?", texto *"No recibirás más recordatorios de comida esta noche. Volverán solos mañana a las 8:00."*, y botones "Todavía no" / "Buenas noches". Al confirmar sale un aviso emergente: *"Buenas noches. Te avisaré mañana a las 8:00 🌙"*.

---

### P7 — Diálogo "Exportar PDF"

**Esta pantalla se queda obsoleta con lo nuevo — ver §5.4.** Hoy es un diálogo con dos opciones excluyentes:
- "Todo el historial"
- "Rango de fechas" → al elegirlo aparecen dos botones, "Desde" y "Hasta", que abren sendos selectores de fecha.

Abajo: "Cancelar" y "Exportar PDF". Si el rango está incompleto o al revés, el botón simplemente no hace nada — sin mensaje de error. (Otro punto flojo.)

---

### P8 — Ajustes

Pantalla completa con flecha de volver. Dos bloques:

**Actualizaciones**
- Fila con "Buscar actualizaciones" + "Comprueba automáticamente si hay una versión nueva." y un interruptor.
- Botón "Buscar actualizaciones" con el estado al lado: spinner, "Estás al día ✓", "Hay una versión nueva: 1.7", "Descargando… 45%", "Verificando…", "Instalando…", o un mensaje de error en rojo ("Sin conexión.", "Falló la descarga.", "La descarga estaba corrupta (se descartó).", …).
- Si hay versión nueva, aparece un botón "Descargar e instalar".

**Acerca de**
- "Crónicas del Apetito" y "Versión 1.6 (7)".

---

### P9 — Los cuatro tipos de registro (qué datos hay realmente)

Esto es importante para diseñar las vistas nuevas: **no hay más datos que estos**.

| Tipo | Qué guarda | Ejemplo |
|---|---|---|
| **Comida** | Texto libre + fecha/hora + foto opcional | "2 platos de macarrones" |
| **Caminata** | Un número de minutos + fecha/hora | "45 min" |
| **Estado de ánimo** | Texto libre + fecha/hora | "Ansioso después de comer" |
| **Gimnasio** | Solo "Sí" o "No" + fecha/hora | "Sí" |

No hay calorías, ni peso, ni categorías de comida, ni escala numérica de ánimo. Si tu diseño necesita alguno de esos datos para funcionar, dímelo antes de dibujarlo: es una decisión de producto, no de maquetación.

---

### P10 — Notificaciones (no son pantallas, pero se ven mucho)

- **Recordatorio de comida**, cada hora en punto de 8:00 a 00:00: "¿Has comido algo?" / "Toca Sí para anotarlo, Caminar si sales a andar, o No para descartar", con tres acciones: **Sí** (abre P3), **Caminar** (abre P4), **No** (descarta).
- **Recordatorio de gimnasio**, a las 22:00 entre semana: "¿Has ido al gimnasio hoy?" con **Sí** / **No**, que registran sin abrir la app. No se pregunta los fines de semana, ni si ya hay registro ese día, ni si ya se ha ido dos veces esa semana.

---

### P11 — El PDF exportado

También es un entregable visual, y hoy está feo. Es A4 generado a mano:
- Título "Crónicas del Apetito" en serif negrita.
- Subtítulo en cursiva gris: "Historial completo" o "Rango: 01/08/2026 – 15/08/2026".
- Por cada día: la fecha en marrón negrita, y debajo una línea por registro: hora en monoespaciada a la izquierda, luego `[Comida] 2 platos de macarrones`.
- Si el registro tiene foto, una miniatura cuadrada de 96 pt a la derecha, con un borde fino.

---

## 5. Lo nuevo que hay que diseñar

Aquí está el grueso del encargo. Hoy la app solo sabe hacer una cosa: **enseñarme todo el historial seguido, en una lista infinita**. Cuando llevas meses registrando, eso deja de servir: no puedo mirar "cómo fue la semana pasada" ni "qué pasó en julio" sin hacer scroll a ciegas.

Lo nuevo son **tres piezas que se combinan entre sí**: vistas, filtro y compartir.

### 5.1 Vistas: Día, Semana y Mes

Hay que poder cambiar entre tres formas de mirar los mismos datos.

**Vista Día** — es la que ya existe (P1): la lista cronológica agrupada por día, con todo el detalle de cada registro. Se queda como está en concepto, aunque la rediseñes visualmente.

**Vista Semana** — los siete días de una semana (lunes a domingo) de un vistazo. Tiene que responder de golpe a: *¿qué días comí y cuántas veces? ¿qué días caminé y cuánto? ¿fui al gimnasio? ¿cómo estuve de ánimo?* Debe permitir moverse a la semana anterior y a la siguiente. Y creo que pide un pequeño resumen de la semana (número de comidas anotadas, minutos totales caminados, días de gimnasio), pero la forma la decides tú.

**Vista Mes** — el mes entero, previsiblemente como una rejilla de calendario. Cada día tiene que mostrar de un vistazo si hubo actividad y de qué tipo, sin abrirlo. Al tocar un día, se ve el detalle de ese día. Navegación entre meses. Aquí también encaja un resumen del mes.

El reto de la vista Mes es que hay **cuatro tipos** de registro y una casilla de calendario es diminuta. Cuatro puntitos de colores es la solución obvia; probablemente haya otra mejor. Y tiene que seguir funcionando con el zoom de fuente a 1.5×.

**Cómo se cambia de vista** es decisión tuya: pestañas, un selector segmentado, un desplegable en la barra… Lo único que pido es que **se vea en qué vista estoy sin tener que abrir nada**, y que llegar a otra vista cueste un toque.

### 5.2 Filtro por rango de fechas

Un filtro "desde / hasta" que **acota qué registros se ven**. Hoy el rango de fechas solo existe escondido dentro del diálogo de exportar (P7); pasa a ser algo de primera clase, visible en la propia pantalla.

Necesita:
- Elegir fecha de inicio y fecha de fin. (Nunca fechas futuras.)
- Atajos rápidos, porque el 90% de las veces voy a querer lo mismo: "Últimos 7 días", "Últimos 30 días", "Este mes", "Todo". Los nombres exactos los podemos afinar.
- **Que se vea que hay un filtro activo** cuando lo hay — algo tipo etiqueta o chip con "1 – 15 de agosto" y una forma rápida de quitarlo. Este punto me importa: la peor versión de esto es mirar una pantalla medio vacía sin darme cuenta de que estoy filtrando.
- Un estado vacío propio: no es lo mismo *"aún no has anotado nada"* que *"no hay nada anotado en estas fechas"*.

### 5.3 Y se combinan

**Las vistas y el filtro no son alternativas: funcionan a la vez.** Vista Semana con un rango puesto, vista Mes con un rango puesto, vista Día con un rango puesto. Las seis combinaciones tienen que tener sentido.

Eso abre preguntas que necesito que resuelvas tú en el diseño:

- En vista **Mes** con un rango del 1 al 15: ¿los días del 16 en adelante se ven atenuados, vacíos, o se ocultan? Yo me inclino por atenuados —así el calendario sigue siendo un calendario—, pero decídelo tú y déjalo dibujado.
- Si el rango **cruza varios meses o semanas** (por ejemplo, del 20 de julio al 10 de agosto) y estoy en vista Mes: ¿qué mes se muestra primero? ¿la navegación entre meses queda limitada al rango o puedo salirme de él?
- ¿Cambiar de vista **conserva** el filtro? (Mi respuesta: sí. Pero enséñame cómo se comunica eso.)
- ¿Navegar a la semana o mes anterior **rompe** el filtro, o lo mueve? (Aquí no lo tengo claro; propón algo.)

### 5.4 Compartir lo que estás viendo

Hoy solo se puede exportar "todo" o "un rango", y siempre con la misma pinta de lista. Lo nuevo: **se comparte exactamente lo que estás mirando**, con la vista y el filtro que tengas puestos en ese momento.

- Vista Semana + semana del 10 al 16 de agosto → se comparte esa semana, con la pinta de la vista Semana.
- Vista Mes + agosto → se comparte agosto, con la pinta del calendario mensual.
- Vista Día + rango del 1 al 15 → se comparte esa lista, acotada.

Esto quiere decir que **cada vista nueva necesita también su versión "para compartir"**, no solo su versión en pantalla. No tienen por qué ser idénticas: en papel hay más sitio y no hay scroll ni toques.

Requisitos del documento que sale:
- Encabezado que diga **qué se está mirando**: "Semana del 10 al 16 de agosto de 2026", "Agosto 2026", "Del 1 al 15 de agosto de 2026". Quien lo recibe tiene que saber qué le ha llegado sin preguntar.
- **Legible impreso y en blanco y negro.** Es el punto que más me importa de todo el encargo: esto acaba en la consulta de la psicóloga. Si dos tipos de registro solo se distinguen por el color, en una fotocopia son el mismo.
- Las fotos de los platos deben seguir apareciendo cuando las hay.

Decisiones que te dejo:
- ¿Se mantiene solo el PDF, o tiene sentido compartir **una imagen** para las vistas Semana y Mes (que son visuales y caben en una pantalla)? Un PDF de una semana por WhatsApp es incómodo; una imagen se ve en la propia conversación.
- El diálogo de exportar actual (P7) queda redundante en cuanto el filtro vive en la pantalla. ¿Lo eliminamos y dejamos un solo botón "Compartir" que hereda vista + filtro, o hace falta conservar un "compartir todo el historial" aparte?

### 5.5 Y todo esto tiene que caber en la barra superior

Recordatorio de lo que la barra ya tiene que sostener hoy: título, ajustes, modo dormir y exportar. Ahora hay que añadir el selector de vista, el filtro y compartir, más la navegación entre semanas/meses. **No cabe tal cual.** Reorganizar la navegación de la pantalla principal es parte del encargo, no un detalle: mueve lo que haga falta a un menú, a una barra inferior, a donde tenga sentido.

---

## 6. Reglas que el diseño no puede romper

1. **Nada de culpabilizar.** Sin objetivos incumplidos, sin rojo de alarma, sin "llevas 3 días sin registrar". Un día vacío es un día vacío, no un fracaso.
2. **Los cuatro tipos siempre distinguibles**, también en blanco y negro y también para alguien que no distingue bien los colores.
3. **Anotar tiene que seguir siendo rapidísimo.** El camino notificación → texto → guardar es el corazón de la app. Si el rediseño mete un paso más ahí, no compensa.
4. **Se lee de un vistazo, con una mano, a menudo de pie.** Texto legible, nada de tipografías finas de 11 sp.
5. **No inventes datos que no existen** (ver P9).

---

## 7. Qué necesito que me entregues

- Las pantallas actuales rediseñadas: **P1, P2, P3, P4, P7 (o lo que la sustituya) y P8**, más los diálogos P5 y P6.
- Las pantallas nuevas: **vista Semana**, **vista Mes**, **el filtro por rango** (incluidos el estado con filtro activo y el vacío filtrado), y **cómo se cambia de vista**.
- Al menos un ejemplo de **combinación** vista + filtro (§5.3), con las decisiones dibujadas, no descritas.
- La versión **para compartir** de cada vista (§5.4).
- El **sistema**: paleta completa con sus roles de Material 3, escala tipográfica, tratamiento de los cuatro tipos, espaciados, radios, elevaciones. Necesito poder aplicarlo a pantallas que tú no hayas dibujado.
- **Estados**: vacío, con pocos datos (2-3 registros) y con muchos (un mes cargado). Las pantallas de portfolio siempre tienen la cantidad perfecta de datos; las reales, no.

Formato: lo que te sea cómodo (Figma, un ZIP de assets, o los dos), pero **los iconos y logos en vector**, y los colores con su hex escrito. Si la tipografía no es Roboto, dime cuál y con qué licencia.

**Un aviso por experiencia:** en cuanto tenga tu diseño lo voy a montar en Compose y lo voy a mirar en un móvil real con el zoom de fuente a 1.3× y a 1.5×. Ahí es donde se rompen los diseños. Si ya has pensado qué pasa cuando el texto crece, me ahorras una ronda entera.

---

## 8. Preguntas abiertas

Resumen de las decisiones que te dejo a ti y que espero ver resueltas en el diseño:

1. Cómo se cambia de vista Día / Semana / Mes (§5.1).
2. Cómo se representan cuatro tipos de registro en una casilla de calendario (§5.1).
3. Qué pasa con los días fuera del rango en las vistas Semana y Mes (§5.3).
4. Si cambiar de vista o navegar entre semanas/meses conserva, mueve o rompe el filtro (§5.3).
5. Si compartimos solo PDF o también imagen (§5.4).
6. Si el diálogo de exportar actual desaparece del todo (§5.4).
7. Cómo se reorganiza la barra superior para que quepa todo (§5.5).
8. Si propones modo oscuro — y en tal caso, presupuestado aparte (§2).

Cualquier cosa que no esté clara aquí, pregúntame antes de dibujarla. Es mucho más barato que rehacerla.
