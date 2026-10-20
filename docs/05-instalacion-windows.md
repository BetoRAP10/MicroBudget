# 5. Instalación y ejecución en Windows

Todo se hace desde una PC con Windows; **no se necesita Mac ni reloj físico**.

## 5.1 Lo que ya tienes instalado (verificado en tu PC)

| Herramienta | Estado |
|---|---|
| Android Studio Ladybug 2024.2 | `C:\Program Files\Android\Android Studio` |
| Android SDK | `C:\Users\aaron\AppData\Local\Android\Sdk` (platforms 34/35/36, build-tools 34/35/36) |
| Emulador | 37.1 |
| Imagen Wear OS 5 (API 34, x86_64) | **Instalada en esta sesión** |
| Reloj virtual `Wear_OS_Large_Round_API_34` | **Creado en esta sesión** (redondo, 454×454, 2 GB RAM) |
| Teléfonos virtuales | `Medium_Phone_API_35` (**recomendado**, probado), `Pixel_9_API_35`*, `Pixel_9_API_36`** |

\* `Pixel_9_API_35` tiene un PIN/patrón de bloqueo configurado: hay que desbloquearlo a mano antes de instalar nada.
\*\* `Pixel_9_API_36` se reiniciaba solo al tener también el reloj encendido (GPU desactivada, 2 GB de RAM).

> No había ningún emulador de reloj en el equipo al iniciar: solo teléfonos y BlueStacks (que no es un reloj). Por eso se eligió Wear OS y se creó el AVD.

## 5.2 Si empiezas en otra PC (pasos desde cero)

1. Instala Android Studio (Ladybug o más nuevo) y ábrelo una vez para que descargue el SDK.
2. **Tools → SDK Manager → SDK Platforms**: marca *Android 15 (API 35)* y *Android 14 (API 34)*.
3. Pestaña **SDK Tools**: marca *Android Emulator*, *Android SDK Platform-Tools*, *Android SDK Command-line Tools*.
4. **Tools → Device Manager → + → Create Virtual Device → categoría Wear OS → Wear OS Large Round** → imagen **Wear OS 5 (API 34) x86_64** → Finish.

Equivalente por línea de comandos (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --install "system-images;android-34;android-wear;x86_64" "platforms;android-35"
& "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n Wear_OS_Large_Round_API_34 -k "system-images;android-34;android-wear;x86_64" -d wearos_large_round
```

## 5.3 Abrir y ejecutar desde Android Studio (recomendado)

1. **File → Open…** → selecciona `C:\Users\aaron\Downloads\apppSmartWatch` → *Trust Project*.
2. Espera a que termine **Gradle Sync** (la primera vez descarga Gradle 8.11.1 y librerías, ~5 min). Si pide actualizar AGP, elige **"Don't ask again"**: las versiones están elegidas para Ladybug.
3. **Device Manager** → ▶ en `Wear OS Large Round API 34`.
4. En la barra superior, selector de configuración: elige **`wear`** y el dispositivo `Wear OS Large Round API 34` → **Run ▶** (Shift+F10).
5. Para el celular: inicia `Medium_Phone_API_35`, elige la configuración **`mobile`** y ese dispositivo → **Run ▶**.
6. La primera vez cada app pide permiso de notificaciones: acéptalo.

### Revisar errores

- **Build** (pestaña inferior): errores de compilación con enlace al archivo y línea.
- **Logcat**: filtra por `package:com.microbudget`. Etiquetas útiles: `MicroBudgetSync` (sincronización) y `AndroidRuntime` (fallos).
- **App Inspection → Database Inspector**: abre `microbudget.db` y ve las tablas `expenses`, `budget`, `categories`, `notified_alerts` en vivo.

## 5.4 Compilar e instalar por línea de comandos

Desde PowerShell en la carpeta del proyecto:

```powershell
cd C:\Users\aaron\Downloads\apppSmartWatch
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# 1) Iniciar emuladores (cada uno abre su ventana)
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Wear_OS_Large_Round_API_34
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_35

# 2) Ver números de serie (normalmente reloj = emulator-5554, celular = emulator-5556)
& $adb devices

# 3) Compilar ambas apps
.\gradlew.bat :wear:assembleDebug :mobile:assembleDebug

# 4) Instalar
& $adb -s emulator-5554 install -r wear\build\outputs\apk\debug\wear-debug.apk
& $adb -s emulator-5556 install -r mobile\build\outputs\apk\debug\mobile-debug.apk

# 5) Abrir
& $adb -s emulator-5554 shell am start -n com.microbudget/com.microbudget.wear.MainActivity
& $adb -s emulator-5556 shell am start -n com.microbudget/com.microbudget.mobile.MainActivity
```

### Pruebas automáticas

```powershell
# Lógica de negocio (24 pruebas, no necesita emulador)
.\gradlew.bat :core:domain:test
# Base de datos real en el emulador (6 pruebas; con el reloj encendido)
.\gradlew.bat :core:data:connectedDebugAndroidTest
```

Reportes HTML: `core\domain\build\reports\tests\test\index.html` y `core\data\build\reports\androidTests\connected\debug\index.html`.

## 5.5 Sincronización entre reloj y celular

### Opción A — Sin emparejar (funciona siempre): script por ADB

Con ambos emuladores encendidos y MicroBudget abierto al menos una vez en cada uno:

```powershell
powershell -ExecutionPolicy Bypass -File tools\simular-sync.ps1
```

El script detecta qué emulador es el reloj, exporta los cambios de cada lado, los importa en el otro y aplica las confirmaciones. Usa el mismo protocolo y la misma resolución de conflictos que la sincronización real.

### Opción B — Emparejamiento real con Wearable Data Layer

Requiere un teléfono virtual **con Google Play** e iniciar sesión con una cuenta de Google en él.

1. Inicia un teléfono con Play Store (p. ej. `Pixel_9_API_35`, desbloquéalo con su PIN) e inicia sesión en Play Store.
2. **Device Manager** → menú ⋮ del reloj → **Pair Wearable** → elige el teléfono → sigue el asistente (instala la app *Pixel Watch / Wear OS* en el teléfono).
3. Instala ambas apps (`wear` en el reloj, `mobile` en el teléfono) desde la **misma PC** (misma firma debug).
4. En la app del celular, **Ajustes → Sincronizar**: el estado cambia a «Conectado».

Si el asistente no está disponible o falla, usa la opción A. La app lo indica honestamente: «Wear OS no disponible en este teléfono» o «Reloj no conectado».

## 5.6 Problemas comunes

| Síntoma | Solución |
|---|---|
| `Activity class ... does not exist` al abrir la app | El teléfono está bloqueado con PIN y su almacenamiento sigue cifrado: desbloquéalo en la ventana del emulador. |
| El emulador no arranca / pantalla negra | Device Manager → ⋮ → **Cold Boot Now**. Cierra otros emuladores si la PC tiene poca RAM. |
| La hora del reloj virtual no coincide | El emulador usa UTC. Inícialo con `-timezone America/Mexico_City` o cambia la zona en Ajustes del reloj. |
| `SDK location not found` | Revisa `local.properties` (`sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk`). |
| Gradle pide JDK | Usa el JDK de Android Studio: *Settings → Build Tools → Gradle → Gradle JDK = jbr-21*. |
| No se ven notificaciones | Acepta el permiso o usa *Ajustes → Permitir notificaciones*; en el reloj desliza hacia arriba para ver la lista. |
| No vibra | El emulador no tiene motor de vibración; la confirmación visual y la notificación reemplazan esa función. |
