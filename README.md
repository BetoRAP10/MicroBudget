# MicroBudget — Control de gastos instantáneo para smartwatch

Proyecto universitario: app **Wear OS** para registrar gastos desde el reloj en segundos + **app Android** complementaria. Desarrollado y probado íntegramente en **Windows** con Android Studio y emuladores (sin Mac, sin reloj físico, sin dinero real).

| Reloj | Celular |
|:-:|:-:|
| <img src="docs/img/reloj-01-inicio.png" width="220"> <img src="docs/img/reloj-05-alerta-80.png" width="220"> | <img src="docs/img/cel-01-inicio.png" width="200"> <img src="docs/img/cel-04-graficas.png" width="200"> |

## Inicio rápido (Windows)

```powershell
cd C:\Users\aaron\Downloads\apppSmartWatch
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :core:domain:test                       # 24 pruebas de lógica
.\gradlew.bat :wear:assembleDebug :mobile:assembleDebug
```

O en Android Studio: **File → Open** esta carpeta → elegir `wear` + *Wear OS Large Round API 34* → **Run ▶**. Guía completa: [docs/05-instalacion-windows.md](docs/05-instalacion-windows.md).

Sincronizar los emuladores sin emparejarlos:

```powershell
powershell -ExecutionPolicy Bypass -File tools\simular-sync.ps1
```

## Entregables

| # | Entregable | Dónde |
|---|---|---|
| 1 | Descripción y justificación | [docs/01-proyecto-y-requisitos.md](docs/01-proyecto-y-requisitos.md) |
| 2 | Requisitos funcionales y no funcionales | [docs/01-proyecto-y-requisitos.md](docs/01-proyecto-y-requisitos.md#14-requisitos-funcionales) |
| 3 | Historias de usuario | [docs/01-proyecto-y-requisitos.md](docs/01-proyecto-y-requisitos.md#16-historias-de-usuario) |
| 4 | Diagrama de arquitectura (incl. sincronización y conflictos) | [docs/02-arquitectura.md](docs/02-arquitectura.md) |
| 5 | Diseño de pantallas (capturas reales) | [docs/03-diseno-de-pantallas.md](docs/03-diseno-de-pantallas.md) |
| 6 | Modelo de datos y reglas | [docs/04-modelo-de-datos.md](docs/04-modelo-de-datos.md) |
| 7 | Estructura de carpetas | abajo |
| 8 | Código fuente | `core/`, `wear/`, `mobile/` |
| 9 | Instalación y ejecución | [docs/05-instalacion-windows.md](docs/05-instalacion-windows.md) |
| 10 | Casos de prueba y resultados | [docs/06-pruebas.md](docs/06-pruebas.md) |
| 11 | Guía de demostración en clase | [docs/07-guia-demostracion.md](docs/07-guia-demostracion.md) |

## Estructura

```
apppSmartWatch/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradle/libs.versions.toml            Versiones (compatibles con Android Studio Ladybug)
├── core/
│   ├── domain/                          LÓGICA DE NEGOCIO (Kotlin puro, sin Android)
│   │   └── src/main/kotlin/com/microbudget/domain/
│   │       ├── model/      Models.kt (Expense, Budget, Category), DefaultCategories.kt
│   │       ├── money/      Money.kt, AmountValidator.kt, AmountInputBuffer.kt (teclado)
│   │       ├── budget/     PeriodCalculator, BudgetCalculator, AlertEngine, AffordabilityChecker
│   │       ├── stats/      StatsCalculator.kt (por categoría/día, filtros)
│   │       ├── format/     DateFormats.kt
│   │       └── sync/       SyncDtos.kt, ConflictResolver.kt, SyncProtocol.kt
│   │   └── src/test/…      24 pruebas JUnit
│   └── data/                            ANDROID COMPARTIDO (reloj y celular)
│       └── src/main/java/com/microbudget/data/
│           ├── local/      Room: Entities, Daos, MicroBudgetDatabase, Mappers
│           ├── repository/ ExpenseRepository, BudgetRepository, CategoryRepository
│           ├── alerts/     AlertService, AlertNotifier (notificaciones), Haptics (vibración)
│           ├── sync/       SyncManager (Data Layer), MicroBudgetListenerService,
│           │               RoomSyncStore, AdbSyncReceiver (alternativa de prueba)
│           ├── demo/       DemoDataSeeder (datos ficticios)
│           └── MicroBudgetGraph.kt  (inyección de dependencias manual)
│       └── src/androidTest/…  6 pruebas instrumentadas con Room real
├── wear/                                APP DEL RELOJ (Compose for Wear OS)
│   └── src/main/java/com/microbudget/wear/
│       ├── WearApp.kt, MainActivity.kt, WearViewModel.kt
│       └── ui/  WearNavHost.kt, theme/, components/,
│               screens/ Home, Keypad, ExpenseFlow (categoría/confirmar/éxito),
│                        History, Budget (+ ¿Me alcanza?), Settings
├── mobile/                              APP DEL CELULAR (Compose + Material 3)
│   └── src/main/java/com/microbudget/mobile/
│       ├── MobileApp.kt, MainActivity.kt, MobileViewModel.kt
│       └── ui/  MobileNavHost.kt, theme/, components/ (tarjetas, gráficas),
│               screens/ Dashboard, History, Stats, ExpenseEditor, Settings, Categories
├── tools/simular-sync.ps1               Sincronización reloj ↔ celular por ADB
└── docs/                                Documentación y capturas
```

## Limitaciones honestas

- **Vibración**: el emulador no vibra; se usa además una confirmación visual animada.
- **Sincronización real (Data Layer)** requiere emparejar el reloj con un teléfono con Google Play y la app Wear OS; sin eso, la app muestra "Wear OS no disponible" y se usa el script ADB, que ejecuta el mismo protocolo.
- **Sin nube**: los datos solo existen en los dispositivos; la app lo indica.
- **Conflictos** por "última modificación gana": depende de que la hora de ambos dispositivos sea correcta.

## Posibles ampliaciones

Tiles y complicaciones de Wear OS para registrar desde la esfera, entrada por voz, presupuestos por categoría, exportar a CSV, respaldo cifrado opcional en la nube, widgets en el celular.
