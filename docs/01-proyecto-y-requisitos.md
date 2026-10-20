# 1. Descripción, justificación y requisitos

## 1.1 Descripción

**MicroBudget** es un sistema de control de gastos personales formado por:

- **App para reloj Wear OS**: registra un gasto en ~5 toques (Nuevo gasto → cantidad → categoría → confirmar), muestra el dinero disponible con un anillo de color y avisa al acercarse al límite.
- **App Android complementaria**: panel, historial completo con filtros, gráficas, edición/eliminación, presupuestos, categorías, alertas e indicador de sincronización.

Los gastos se registran **manualmente**: no hay conexión bancaria ni se piden credenciales.

## 1.2 Justificación

Los gastos pequeños (café, transporte, snacks) son los que más se olvidan porque registrarlos en el celular "da flojera". El reloj está siempre en la muñeca: si registrar cuesta menos de 10 segundos, el usuario lo hace en el momento. El proyecto demuestra:

| Competencia | Dónde se ve |
|---|---|
| Interfaz para pantalla pequeña | Teclado numérico proporcional al diámetro, anillo de progreso, pocas pantallas |
| Almacenamiento persistente | Room (SQLite) en ambos dispositivos |
| Cálculos financieros | BigDecimal / centavos, disponible, % consumido, promedio diario, «¿Me alcanza?» |
| Notificaciones | Alertas de 80 %, 100 % y límite personalizado, sin duplicados |
| Sincronización | Wearable Data Layer + protocolo PUSH/ACK con resolución de conflictos |

## 1.3 Plataforma elegida

El equipo de desarrollo es una **PC con Windows** sin Mac, con Android Studio Ladybug instalado. Por eso se eligió **Wear OS** (Kotlin + Jetpack Compose for Wear OS), que se compila, instala y depura por completo desde Windows con el emulador oficial. Apple Watch se descartó porque watchOS solo se compila con Xcode en macOS.

## 1.4 Requisitos funcionales

| ID | Requisito | Reloj | Celular |
|---|---|:-:|:-:|
| RF-01 | Registrar un gasto con cantidad, categoría, fecha/hora y descripción opcional | ✔ (sin descripción) | ✔ |
| RF-02 | Validar importes: positivos, ≤ 2 decimales, ≤ $1,000,000 | ✔ | ✔ |
| RF-03 | Clasificar en 6 categorías iniciales (Comida, Transporte, Entretenimiento, Compras, Servicios, Otros) | ✔ | ✔ |
| RF-04 | Crear, editar y eliminar categorías; los gastos de una categoría eliminada pasan a «Otros» | | ✔ |
| RF-05 | Configurar presupuesto semanal (lunes–domingo) o mensual | ✔ | ✔ |
| RF-06 | Calcular presupuesto inicial, gastado, disponible (o exceso), % consumido, promedio diario y gasto diario sugerido | ✔ | ✔ |
| RF-07 | Alertas configurables al 80 %, 100 % y límite personalizado, una vez por periodo | ✔ | ✔ |
| RF-08 | Historial reciente (10 movimientos) con cantidad, categoría, fecha/hora y periodo | ✔ | |
| RF-09 | Historial completo con filtros por categoría y fecha | | ✔ |
| RF-10 | Editar y eliminar gastos recalculando totales y estadísticas | eliminar | ✔ |
| RF-11 | Gráficas por categoría y por día | | ✔ |
| RF-12 | «¿Me alcanza?»: simular un gasto sin guardarlo | ✔ | ✔ |
| RF-13 | Guardar sin conexión y sincronizar después | ✔ | ✔ |
| RF-14 | Indicar qué cambios no se han sincronizado | ✔ | ✔ |
| RF-15 | Confirmación visual y vibración al guardar | ✔ | |
| RF-16 | Datos de prueba para la demostración | ✔ | ✔ |

## 1.5 Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | **Rapidez**: registrar un gasto en el reloj en ≤ 5 toques y < 10 s. |
| RNF-02 | **Legibilidad**: textos ≥ 11 sp en el reloj, contraste alto (blanco/verde/naranja/rojo sobre fondo #0E0A1A). |
| RNF-03 | **Precisión monetaria**: nunca se usa `Double` para dinero; BigDecimal en la lógica y centavos (`Long`) en la base de datos. |
| RNF-04 | **Persistencia**: los datos sobreviven a cerrar la app y reiniciar el dispositivo. |
| RNF-05 | **Funcionamiento offline**: el reloj funciona sin celular (app *standalone*). |
| RNF-06 | **Privacidad**: sin credenciales bancarias, sin nube, solo los datos mínimos. |
| RNF-07 | **Mantenibilidad**: lógica de negocio en un módulo Kotlin puro con pruebas unitarias. |
| RNF-08 | **Compatibilidad**: Wear OS 3+ (API 30+), Android 8+ (API 26+); se desarrolla en Windows. |
| RNF-09 | **Honestidad del estado**: la app nunca dice "respaldado" ni "sincronizado" si no lo está. |

## 1.6 Historias de usuario

| ID | Como… | quiero… | para… | Criterios de aceptación |
|---|---|---|---|---|
| HU-01 | usuario con prisa | registrar un gasto desde el reloj en pocos toques | no olvidarlo | Desde el inicio: Nuevo gasto → teclado → categoría → ✓. Se ve «¡Guardado!» y vibra (si hay vibrador). |
| HU-02 | usuario | ver de un vistazo cuánto me queda | decidir si gasto | El inicio muestra disponible, gastado y %, con anillo verde/naranja/rojo. |
| HU-03 | usuario | saber si me alcanza antes de comprar | no pasarme | «¿Me alcanza?» responde Sí / Justo / No con lo que quedaría. |
| HU-04 | usuario | recibir un aviso al llegar al 80 % y 100 % | frenar a tiempo | Una notificación por umbral y periodo; no se repite con cada gasto. |
| HU-05 | usuario | elegir presupuesto semanal o mensual | adaptarlo a cómo cobro | Cambiar el periodo recalcula fechas, totales y reinicia alertas. |
| HU-06 | usuario | corregir o borrar un gasto equivocado | que los totales sean reales | Al editar/eliminar se actualizan disponible, % y gráficas. |
| HU-07 | usuario | ver en el celular gráficas por categoría | entender en qué gasto | Dona + barras por categoría y por día del periodo. |
| HU-08 | usuario | filtrar el historial por categoría y fechas | revisar un periodo | Chips de categoría y rangos (hoy, periodo, 7/30 días, personalizado). |
| HU-09 | usuario sin señal | registrar gastos aunque el reloj no esté conectado | no perderlos | Se guardan localmente como «pendientes» y se envían al reconectar. |
| HU-10 | usuario | crear mis propias categorías | clasificar mejor | Nombre, emoji y color; se sincronizan al reloj. |
