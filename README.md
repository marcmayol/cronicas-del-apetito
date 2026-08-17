# Crónicas del Apetito

App Android personal para registrar lo que comes a lo largo del día, pensada para acompañar el trabajo con la psicóloga sobre ansiedad y comida.

## Qué hace

- Te envía una notificación **cada hora en punto de 8:00 a 00:00** preguntando "¿Has comido algo?", con acciones **Sí** (anotar comida), **Caminar** y **No**.
- A las **22:00 entre semana** pregunta si has ido al gimnasio, y se registra desde la propia notificación.
- Guarda cuatro tipos de registro: **comida** (texto + foto opcional), **caminata** (minutos), **estado de ánimo** (texto) y **gimnasio** (sí/no).
- La pantalla principal se puede mirar de **tres formas**: Día, Semana y Mes.
- **Filtro por rango de fechas** que acota las tres vistas, con atajos (últimos 7 / 30 días, este mes, todo).
- Botón **Compartir**: manda exactamente lo que estás viendo —vista + periodo + filtro— como **imagen** (Semana y Mes) o **PDF** (Día y rangos largos).
- **Modo "me voy a dormir"**: silencia los avisos hasta las 8:00 del día siguiente.

Todo se guarda en local en el móvil. Sin nube ni cuentas.

## Las tres vistas

| Vista | Qué muestra |
|---|---|
| **Día** | La lista cronológica agrupada por día, con el detalle de cada registro y las fotos. |
| **Semana** | Los siete días (lunes a domingo) de un vistazo, con el resumen de la semana en cuatro cifras. Tocar un día lo abre en vista Día. |
| **Mes** | El calendario del mes; cada día lleva los glifos de los tipos que tuvo. Tocar un día muestra su resumen; "Ver día completo" salta a vista Día. |

Las vistas y el filtro **se combinan**: el filtro se conserva al cambiar de vista y al navegar entre semanas o meses, y lo que queda fuera del rango se ve **atenuado** —nunca oculto— y no cuenta en el resumen ni en lo que se comparte.

## Los cuatro tipos, y por qué llevan glifo

| Tipo | Color | Glifo |
|---|---|---|
| Comida | `#9A5B2F` | ● |
| Caminata | `#5C7549` | ▲ |
| Estado de ánimo | `#7B5C90` | ◆ |
| Gimnasio | `#47698C` | ■ |

**Regla del sistema: el color de un tipo nunca aparece sin su glifo o su icono.** Lo que se exporta acaba impreso o fotocopiado en la consulta, y en blanco y negro dos colores distintos son el mismo gris. Los glifos, las etiquetas y la leyenda al pie de cada documento son lo que mantiene la información legible ahí.

## Auto-actualización

La app se distribuye fuera de Play Store y se **actualiza sola** desde GitHub Releases: comprueba un manifiesto en GitHub Pages (al abrir, periódicamente y a mano desde Ajustes), descarga el APK, **verifica su SHA-256** e instala por `PackageInstaller`. La lógica vive en el módulo reutilizable [`actualizador`](actualizador/README.md). Para publicar una versión, ver [PUBLICAR.md](PUBLICAR.md).

## Cómo instalarlo en tu móvil

### 1. Compilar el APK

Necesitas tener el Android SDK instalado (en `C:\Users\marcm\Android\Sdk` según `local.properties`).

Desde esta carpeta, en PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

El APK se generará en `app\build\outputs\apk\debug\app-debug.apk`.

### 2. Instalarlo en el móvil

**Opción A — Por USB (recomendada):** activa "Opciones de desarrollador" y "Depuración USB" en el móvil, conéctalo y ejecuta `adb install app\build\outputs\apk\debug\app-debug.apk`.

**Opción B — Copiar el APK al móvil** y abrirlo desde el explorador de archivos, permitiendo "Instalar apps desconocidas".

### 3. Al abrir la app por primera vez

- Concede el permiso de **notificaciones**.
- Si Android te lo pide, permite **"Alarmas y recordatorios"** (necesario en Android 12+ para que las horas sean exactas).

## Estructura del código

```
app/src/main/java/com/marcm/cronicasapetito/
├─ CronicasApp.kt              Application: crea canal, programa primera alarma
├─ MainActivity.kt             Arranque: permisos + MainScreen
├─ data/
│  ├─ MealEntry.kt             Entidad Room (los cuatro tipos)
│  ├─ MealEntryDao.kt          DAO, con observeInRange para las vistas
│  ├─ AppDatabase.kt           Database singleton
│  ├─ MealRepository.kt        Acceso a datos
│  ├─ Periodos.kt              Calendario (java.time): semanas, meses, rangos
│  └─ Resumen.kt               Agregados por día y por periodo (funciones puras)
├─ notifications/              Alarmas y notificaciones de comida y gimnasio
├─ ui/
│  ├─ Theme.kt                 Paleta, tipografía (Lora + Roboto) y los cuatro tipos
│  ├─ MainViewModel.kt         Estado: vista × filtro × periodo
│  ├─ MainScreen.kt            Barra, selector, chip de filtro y hojas
│  ├─ Vistas.kt                VistaDia, VistaSemana, VistaMes
│  ├─ Comunes.kt               Piezas del sistema (tarjetas, chips, fechas)
│  ├─ FiltroSheet.kt           Filtro por rango de fechas
│  ├─ CompartirSheet.kt        Elegir imagen o PDF
│  ├─ EntryActivity.kt         Anotar comida
│  ├─ WalkMoodActivity.kt      Caminata + ánimo en tres pasos
│  └─ AjustesActivity.kt       Actualizaciones y "Acerca de"
└─ export/
   ├─ PdfExporter.kt           PDF A4 con glifos y leyenda
   ├─ ImagenExporter.kt        PNG de la semana y del mes
   └─ Share.kt                 FileProvider + ACTION_SEND
```

El rediseño y el encargo de las vistas están documentados en [`design-handoff/BRIEF.md`](design-handoff/BRIEF.md); las notas de implementación, en [PLAN_VISTAS.md](PLAN_VISTAS.md).

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

Cubren la lógica que se rompe en los bordes: semanas a caballo entre meses, huecos del calendario, febrero bisiesto, rangos invertidos o de un solo día, y los agregados por día (incluido que un "No" de gimnasio es día respondido, no día sin dato).

## Notas

- La hora exacta no la garantiza Android al 100% en modo Doze. Por eso usamos `setExactAndAllowWhileIdle` cuando hay permiso, y reprogramamos la siguiente alarma cada vez que llega la actual.
- Si el móvil mata la app en segundo plano (Xiaomi/Huawei son agresivas), añádela a la "lista blanca" de batería.
- La tipografía **Lora** va empaquetada en `res/font/lora.ttf` (SIL OFL, fuente variable) y solo se usa en títulos y cifras; el cuerpo va en Roboto, que es lo que crece con el zoom del sistema.
