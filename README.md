# Crónicas del Apetito

App Android personal para registrar lo que comes a lo largo del día, pensada para acompañar el trabajo con la psicóloga sobre ansiedad y comida.

## Qué hace

- Te envía una notificación **cada hora en punto de 8:00 a 00:00** preguntando "¿Has comido algo?".
- Si pulsas **No**, la notificación se descarta sin más.
- Si pulsas **Sí**, se abre la app con un cuadro de texto y el recordatorio: *"¿Qué has comido? Recuerda anotar la cantidad (ej.: 1 o 2 platos de macarrones)"*.
- La pantalla principal muestra el historial **agrupado por día y hora**, con hoy arriba.
- Botón **Exportar PDF**: genera un PDF con todo el historial o un rango de fechas y lo comparte (WhatsApp, email, etc.).

Todo se guarda en local en el móvil. Sin nube ni cuentas.

## Cómo instalarlo en tu móvil

### 1. Compilar el APK

Necesitas tener el Android SDK instalado (en `C:\Users\marcm\Android\Sdk` según `local.properties`).

Desde esta carpeta, en PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

El APK se generará en:

```
app\build\outputs\apk\debug\app-debug.apk
```

### 2. Instalarlo en el móvil

**Opción A — Por USB (recomendada):**

1. Activa "Opciones de desarrollador" en el móvil (Ajustes → Información → toca 7 veces el número de compilación).
2. Activa "Depuración USB" dentro de Opciones de desarrollador.
3. Conecta el móvil al PC por USB y acepta el aviso de "Permitir depuración USB".
4. Desde esta carpeta:

```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
```

**Opción B — Copiar el APK al móvil:**

Copia `app-debug.apk` al móvil (cable, Drive, Telegram a ti mismo…) y ábrelo desde el explorador de archivos. Tendrás que permitir "Instalar apps desconocidas" para tu explorador.

### 3. Al abrir la app por primera vez

- Concede el permiso de **notificaciones** que te pida.
- Si Android te lo pide, también permite **"Alarmas y recordatorios"** (necesario en Android 12+ para que las horas sean exactas). La app te abrirá la pantalla de ajustes automáticamente si hace falta.

## Estructura del código

```
app/src/main/java/com/marcm/cronicasapetito/
├─ CronicasApp.kt              Application: crea canal, programa primera alarma
├─ MainActivity.kt             Pantalla principal: historial + exportar PDF
├─ data/
│  ├─ MealEntry.kt             Entidad Room
│  ├─ MealEntryDao.kt          DAO
│  ├─ AppDatabase.kt           Database singleton
│  └─ MealRepository.kt        Acceso a datos
├─ notifications/
│  ├─ MealAlarmScheduler.kt    Programa la siguiente hora en punto (8-24h)
│  ├─ MealAlarmReceiver.kt     Recibe la alarma → muestra notif + reprograma
│  ├─ MealNotifier.kt          Construye notificación con acciones Sí/No
│  ├─ DismissReceiver.kt       Acción "No" → descarta la notificación
│  └─ BootReceiver.kt          Reprograma alarmas al reiniciar el móvil
├─ ui/
│  ├─ Theme.kt                 Tema Material 3 (cálido, tonos marrón/crema)
│  ├─ MainScreen.kt            Lista del historial agrupada por día/hora
│  ├─ EntryActivity.kt         Cuadro de texto al pulsar "Sí"
│  └─ ExportDialog.kt          Diálogo de rango de fechas
└─ export/
   ├─ PdfExporter.kt           Genera el PDF con PdfDocument nativo
   └─ Share.kt                 FileProvider + ACTION_SEND
```

## Notas

- La hora exacta no la garantiza Android al 100% en modo Doze (ahorro batería). Por eso usamos `setExactAndAllowWhileIdle` cuando hay permiso, y reprogramamos la siguiente alarma cada vez que llega la actual.
- Si el móvil mata la app en segundo plano (algunas marcas como Xiaomi/Huawei son agresivas), añade Crónicas del Apetito a la "lista blanca" de batería en los ajustes del fabricante.
