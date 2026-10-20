package com.microbudget.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** Periodo del presupuesto. */
enum class BudgetPeriod { WEEKLY, MONTHLY }

/** Dispositivo en el que se creó un movimiento. */
enum class DeviceSource { WATCH, PHONE }

/** Estado de sincronización de un registro local. */
enum class SyncStatus {
    /** Cambio local que el otro dispositivo todavía no ha confirmado. */
    PENDING,

    /** El otro dispositivo confirmó tener exactamente esta versión. */
    SYNCED,
}

/**
 * Gasto registrado manualmente por el usuario.
 * - [amount] usa BigDecimal (2 decimales) para evitar errores de coma flotante.
 * - [dateTime] y [updatedAt] son milisegundos desde epoch (UTC).
 * - [deleted] es una "lápida": los borrados se sincronizan como un cambio más.
 */
data class Expense(
    val id: String,
    val amount: BigDecimal,
    val categoryId: String,
    val dateTime: Long,
    val description: String?,
    val createdFrom: DeviceSource,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deleted: Boolean = false,
)

data class Category(
    val id: String,
    val name: String,
    /** Emoji usado como icono (se sincroniza como texto, no requiere recursos). */
    val icon: String,
    /** Color ARGB, p. ej. 0xFFF97316. */
    val colorArgb: Long,
    val isDefault: Boolean = false,
    val sortOrder: Int = 100,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

/**
 * Presupuesto activo. Solo existe uno (id = [MAIN_ID]).
 * [startDate]/[endDate] se recalculan automáticamente al cambiar de periodo.
 * Umbrales de alerta configurados: 80 %, 100 % y un límite personalizado en dinero.
 */
data class Budget(
    val id: String = MAIN_ID,
    val amount: BigDecimal,
    val period: BudgetPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val alertAt80: Boolean = true,
    val alertAt100: Boolean = true,
    val customAlertAmount: BigDecimal? = null,
    val updatedAt: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
) {
    companion object {
        const val MAIN_ID = "main"
    }
}
