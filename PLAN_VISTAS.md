# Vistas Semana/Mes, filtro por rango y compartir — estado

Notas internas. El documento del diseñador es [`design-handoff/BRIEF.md`](design-handoff/BRIEF.md);
el tablero con el rediseño vino de Claude Design (proyecto «Aplicación móvil incompleta»).

**Estado: hecho y verificado en emulador.** Queda pendiente publicar (ver §Publicar).
Punto de partida v1.6 (versionCode 7) → ahora **v2.0 (versionCode 8)**, sin publicar.

## Lo que se implementó

**Datos y lógica**
- `MealEntryDao.observeInRange` para alimentar las vistas sin traer todo el historial cuando hay filtro.
- `data/Periodos.kt` — calendario con `java.time`: semana de lunes a domingo, casillas del mes
  con sus huecos, inicio/fin de día, y `RangoFechas` (que **se ordena solo** si viene al revés).
- `data/Resumen.kt` — agregados puros por día y por periodo. Sin Room ni Compose: por eso se
  pueden probar de verdad.
- `ui/MainViewModel.kt` — el estado es **vista × filtro × ancla de periodo**, no tres pantallas
  distintas. El filtro se conserva al cambiar de vista y al navegar; sobrevive a rotación
  (`SavedStateHandle`), no a cerrar la app — es un filtro de sesión, a propósito.

**Interfaz** (`Theme.kt`, `Comunes.kt`, `Vistas.kt`, `MainScreen.kt`, `FiltroSheet.kt`,
`CompartirSheet.kt`, y el rediseño de `EntryActivity`, `WalkMoodActivity`, `AddDialogs`,
`AjustesActivity`)
- Sistema del tablero: paleta marrón/crema afinada, Lora en títulos y cifras, Roboto en el cuerpo,
  y los cuatro tipos con **color + icono + glifo**.
- Barra reorganizada: título · luna · compartir · ⋮ (Ajustes, Exportar todo). El selector de vista,
  el filtro y la navegación de periodo viven en su propia zona bajo la barra.
- Tres señales de filtro activo: chip con el rango y su ✕, punto sobre el botón de filtro y
  contador de registros.
- Novedades pedibles sin tocar datos en el flujo de caminata: indicador de 3 pasos, atajos de
  minutos y steppers ±5.

**Compartir**
- `export/ImagenExporter.kt` — PNG de la semana y del mes dibujados **a mano en un Canvas del
  tamaño final**, no capturando la pantalla: el resultado no depende del móvil ni del zoom de
  fuente. El alto se ajusta al contenido.
- `export/PdfExporter.kt` — reescrito: cabecera con el periodo, columna de hora monoespaciada,
  etiqueta con glifo y **leyenda al pie de cada página**.
- El diálogo de exportar (P7) desapareció; «Exportar todo el historial (PDF)» vive en el menú ⋮.

## Decisiones del brief, resueltas

1. Cambio de vista: **segmentado bajo la barra**, siempre visible.
2. Casilla del mes: **glifos con forma propia** (●▲◆■), no solo color.
3. Días fuera de rango: **atenuados con sus marcas**, nunca ocultos.
4. El filtro **se conserva** al cambiar de vista y al navegar; navegar no lo rompe ni lo mueve.
5. Compartir: **imagen** para Semana y Mes, **PDF** para Día; el sheet ofrece ambas.
6. El diálogo de exportar **desaparece**.
7. Barra superior reorganizada.
8. Modo oscuro: **no** implementado (quedó como propuesta aparte en el tablero).

## Verificado en pantalla

Emulador limpio, 123 registros de ejemplo (28 días). Comprobadas las tres vistas, el filtro,
la combinación Mes + rango, las dos hojas, anotar comida, caminata, ajustes, el PDF (8 páginas)
y las dos imágenes.

Dos fallos que **solo aparecieron con el zoom de fuente a 1.5×** y ya están corregidos:
- el número del día se partía en dos líneas («M 1 / 8») por un ancho fijo en dp;
- las etiquetas largas del resumen se cortaban a mitad de palabra («caminad/os»).

La imagen compartida se revisó **en escala de grises**: los cuatro tipos siguen distinguiéndose
por forma, que era el requisito duro del brief.

## Publicar (pendiente, cuando lo decidas)

- `versionCode` ya está en 8 y `versionName` en 2.0.
- **Árbol limpio antes de `scripts/publicar_release.py`**: el script etiqueta la Release antes de
  commitear, así que publicar con cambios sin commitear deja el tag apuntando a otro código.
- El keystore de producción es crítico y no está en el repo.
- Al ser un cambio grande de UI, merece mirar la app en el móvil real antes de publicar.
