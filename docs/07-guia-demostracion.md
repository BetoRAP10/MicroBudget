# 7. Guía para demostrar el proyecto en clase (≈ 10 minutos)

## Antes de clase (15 min)

1. Abre Android Studio y el proyecto. Inicia **Wear OS Large Round API 34** y **Medium Phone API 35** (Device Manager ▶).
2. Instala ambas apps (Run ▶ con `wear` y con `mobile`, o los comandos de [05-instalacion-windows.md](05-instalacion-windows.md#54-compilar-e-instalar-por-línea-de-comandos)).
3. Deja los datos limpios: en el reloj **Ajustes y sync → Borrar datos del reloj** y en el celular **Ajustes → Borrar todo**.
4. Ten una ventana de PowerShell abierta en la carpeta del proyecto.
5. Si la PC tiene poca RAM, cierra Chrome y otras apps: dos emuladores usan ~5 GB.

## Guion

| # | Qué hacer | Qué decir / qué se ve |
|---|---|---|
| 1 | Muestra el inicio del reloj | "Pantalla diseñada para reloj, no una copia del celular: anillo de progreso, disponible en grande, botón grande." |
| 2 | Reloj: **Ajustes y sync → Cargar datos demo** y vuelve (deslizar →) | $2,000 mensuales, gastado $1,450 (73 %), anillo verde. "Dinero ficticio, sin banco." |
| 3 | **Nuevo gasto** → `1` `5` `0` → ✓ → 🍔 Comida → ✓ | 5 toques. Check animado + vibración (en el emulador no se siente; por eso la confirmación visual). **Alerta del 80 %** en pantalla y como notificación. |
| 4 | Otro gasto de **$10** | Se guarda **sin alerta**: "el 80 % ya se notificó en este periodo; no se repite." |
| 5 | **¿Me alcanza?** → `3` `0` `0` → ✓ | "No te alcanza: te faltarían…" Explica que no guarda nada. |
| 6 | Gasto de **$450** en Compras | Alerta de **presupuesto agotado / exceso**; el anillo y el número se ponen rojos con "Excedido". |
| 7 | **Historial** del reloj | Últimos 10 con categoría, fecha/hora y periodo; `⟳` = no sincronizado. "Se registraron sin conexión al celular." |
| 8 | En PowerShell: `powershell -ExecutionPolicy Bypass -File tools\simular-sync.ps1` | Muestra los mensajes: exportados, importados, confirmados. Explica el protocolo PUSH/ACK. |
| 9 | Abre el celular | Indicador "Sincronizado", los gastos del reloj aparecen (ícono ⌚), resumen y dona actualizados. |
| 10 | Celular: **Historial** → filtra por 🍔 Comida y "7 días" | Filtros por categoría y fecha. |
| 11 | Toca el gasto de $450 → cámbialo a **$400** → Guardar; toca el de $10 → 🗑 Eliminar | Totales, % y gráficas se recalculan al instante; aparecen como "pendiente". |
| 12 | Ejecuta otra vez `simular-sync.ps1` y mira el reloj | El reloj muestra $2,000 gastados · 100 %: la edición y el borrado llegaron. |
| 13 | **Gráficas** en el celular | Dona por categoría y barras por día. |
| 14 | **Ajustes**: cambia a **Semanal** y guarda | El periodo, los totales y las alertas se reinician para la semana. |
| 15 | Cierra la app (deslizar en recientes) y ábrela | Los datos siguen ahí: Room/SQLite persistente. |
| 16 | (Opcional) Terminal: `.\gradlew.bat :core:domain:test` | 24 pruebas de lógica en verde, incluida la de conflictos. |

## Preguntas frecuentes del profesor

- **¿Y si el mismo gasto se edita en los dos?** Gana la edición más reciente (`updatedAt`). Si empatan exactamente, gana el borrado; si no, un desempate determinista. Probado en `SyncProtocolTest`.
- **¿Por qué ADB y no Bluetooth?** El emulador no tiene Bluetooth. La app incluye la sincronización real por Wearable Data Layer (funciona al emparejar con Play Services); el script usa **el mismo protocolo** con otro transporte para poder demostrarlo sin cuenta de Google.
- **¿Dónde está la nube?** No hay: se dice explícitamente en la app. Los datos están solo en los dispositivos.
- **¿Por qué no Double?** `0.1 + 0.2 ≠ 0.3` en coma flotante. Se usa BigDecimal y centavos.
