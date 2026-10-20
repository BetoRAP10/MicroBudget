# 6. Casos de prueba y resultados

Ejecutado el 19-oct-2026 en Windows 11, Android Studio Ladybug, emuladores **Wear OS Large Round API 34 (Wear OS 5)** y **Medium Phone API 35 (Android 15)**.

## 6.1 Pruebas automáticas

| Suite | Tipo | Dónde corre | Pruebas | Resultado |
|---|---|---|---|---|
| `AmountValidationTest` | Unitaria (JUnit) | PC | 5 | ✅ 5/5 |
| `BudgetCalculatorTest` | Unitaria | PC | 7 | ✅ 7/7 |
| `AlertEngineTest` | Unitaria | PC | 6 | ✅ 6/6 |
| `SyncProtocolTest` | Unitaria | PC | 6 | ✅ 6/6 |
| `PersistenceTest` (Room real) | Instrumentada | Reloj Wear OS 5 | 6 | ✅ 6/6 |
| `PersistenceTest` (Room real) | Instrumentada | Teléfono Android 15 | 6 | ✅ 6/6 |

```powershell
.\gradlew.bat :core:domain:test                      # unitarias
.\gradlew.bat :core:data:connectedDebugAndroidTest   # instrumentadas (emulador encendido)
```

## 6.2 Casos del enunciado

| # | Caso | Pasos | Resultado esperado | Evidencia | Resultado |
|---|---|---|---|---|---|
| 1 | Un gasto se guarda correctamente | Reloj: Nuevo gasto → 150 → Comida → ✓ | «¡Guardado! $150.00 · Comida»; aparece en historial como pendiente | `PersistenceTest.guardaGastoConCategoriaYEstadoPendiente`; captura `reloj-05` | ✅ |
| 2 | Categorías asignadas correctamente | Registrar en varias categorías; ver Gráficas | Cada gasto con su categoría; totales por categoría correctos | `BudgetCalculatorTest.agrupa por categoria…`; `cel-04` (Compras $799, Comida $400…) | ✅ |
| 3 | Presupuesto restante bien calculado | Presupuesto $2,000, gastos $800 | Disponible $1,200, 40 %, promedio $44.44/día (18 días) | `BudgetCalculatorTest.calcula gastado…`; exceso: `-30.25` → «Excedido por $30.25» | ✅ |
| 4 | Alertas al alcanzar umbrales | Demo (72.5 %) + $150 → 80 %; + $450 → 103 % | Alerta 80 % y luego «Presupuesto agotado · Exceso $60» | Emulador: capturas `reloj-05`, `reloj-06`; notificación del sistema visible | ✅ |
| 5 | Alertas no se duplican | Tras la alerta del 80 %, registrar $10 | Se guarda sin alerta | Emulador: `$10.00 · Transporte` sin tarjeta de alerta; `AlertEngineTest.avisa al 80 y al 100 una sola vez` | ✅ |
| 6 | Editar/eliminar actualiza totales | Celular: $450 → $400; eliminar $10 | Gastado $2,060 → $2,000; 103 % → 100 %; 12 → 11 gastos | Emulador (celular y, tras sincronizar, reloj); `PersistenceTest.editarYEliminarActualizaTotales` | ✅ |
| 7 | Gastos permanecen al cerrar la app | Cerrar BD y reabrir / cerrar app y abrir | El gasto sigue con el mismo importe | `PersistenceTest.losGastosPermanecenAlCerrarYReabrir`; reinstalaciones `install -r` conservaron los datos | ✅ |
| 8 | Cambios se sincronizan reloj ↔ celular | `tools\simular-sync.ps1` | Mismos datos en ambos; pendientes = 0 | Salida: «Confirmados 19, recibidos 0»; reloj «Todo sincronizado»; celular muestra los gastos ⌚ | ✅ (por ADB) |
| 9 | Gastos offline se sincronizan después | Reloj registra 12 gastos sin celular → luego sincroniza | Quedan «⟳ pendiente» y luego SYNCED | `SyncProtocolTest.gastos sin conexion…`; emulador: «10 sin sincronizar» → «Todo sincronizado» | ✅ |
| 10 | Importes inválidos rechazados | `0`, `-5`, `12.345`, `abc`, `1,000.00`, `1000000.01` | Mensaje de error, no se guarda | `AmountValidationTest`; celular: «Cantidad no válida (máx. 2 decimales)» (`cel-05`); reloj bloquea el 3er decimal | ✅ |

## 6.3 Pruebas adicionales

| Caso | Resultado |
|---|---|
| Un gasto cruza 80 % y 100 % a la vez → solo se muestra la más grave | ✅ `AlertEngineTest` |
| Límite personalizado ($300) avisa al llegar exactamente a $300.00 | ✅ `AlertEngineTest` |
| Borrar un gasto baja del umbral → el umbral se rearma | ✅ `AlertEngineTest` |
| Nuevo periodo (lunes siguiente) → alertas restablecidas | ✅ `AlertEngineTest` |
| Conflicto: reloj edita a $55 (t=200), celular a $60 (t=300) → ambos quedan en $60 | ✅ `SyncProtocolTest` |
| Borrado en el celular llega al reloj como lápida | ✅ `SyncProtocolTest` y emulador |
| Empate de `updatedAt`: el resultado es el mismo en ambos lados; el borrado gana | ✅ `SyncProtocolTest` |
| Eliminar categoría mueve sus gastos a «Otros»; «Otros» no se elimina | ✅ `PersistenceTest` |
| «¿Me alcanza?» Sí / Justo / No (incluye exactamente 100 % = Justo) | ✅ `BudgetCalculatorTest`; reloj «No te alcanza» (`reloj-08`) |
| Semana empieza en lunes; mes en día 1 | ✅ `BudgetCalculatorTest` |
| `0.10 + 0.20 = 0.30` exacto | ✅ `AmountValidationTest` |
| Celular sin app Wear OS → estado «Wear OS no disponible», sin fallar | ✅ emulador (logcat `Wearable API no disponible: 17`) |

## 6.4 No probado / limitaciones

- **Sincronización por Wearable Data Layer** entre emuladores emparejados: el código está implementado y compila, pero no se pudo probar en esta PC porque requiere emparejar con un teléfono con Google Play y sesión de Google. El protocolo se verificó con las pruebas unitarias y con el transporte ADB, que usa el mismo `SyncProtocol`.
- **Vibración**: el emulador no tiene vibrador físico; el código se ejecuta pero no se puede sentir.
- El AVD `Pixel_9_API_35` tiene PIN configurado y `Pixel_9_API_36` fue inestable con dos emuladores a la vez (se reiniciaba), por eso el celular se probó en `Medium_Phone_API_35`.
