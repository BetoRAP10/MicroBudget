package com.microbudget.domain.sync

import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate

/** Registro sincronizable: versión = updatedAt; los borrados viajan como lápidas. */
interface SyncRecord {
    val id: String
    val updatedAt: Long
    val deleted: Boolean
}

@Serializable
data class ExpenseDto(
    override val id: String,
    /** Decimal en texto ("45.10") para no perder precisión en JSON. */
    val amount: String,
    val categoryId: String,
    val dateTime: Long,
    val description: String? = null,
    val createdFrom: String,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncRecord

@Serializable
data class CategoryDto(
    override val id: String,
    val name: String,
    val icon: String,
    val colorArgb: Long,
    val isDefault: Boolean = false,
    val sortOrder: Int = 100,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncRecord

@Serializable
data class BudgetDto(
    override val id: String,
    val amount: String,
    val period: String,
    val alertAt80: Boolean,
    val alertAt100: Boolean,
    val customAlertAmount: String? = null,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncRecord

/** Paquete que viaja entre reloj y celular (por Wearable Data Layer o por archivo vía ADB). */
@Serializable
data class SyncBundle(
    val protocolVersion: Int = 1,
    val origin: String,
    val kind: String,
    val sentAt: Long,
    /** Si es true, el receptor responde con TODOS sus registros (sincronización completa). */
    val requestFull: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList(),
    val budget: BudgetDto? = null,
) {
    val isEmpty: Boolean get() = categories.isEmpty() && expenses.isEmpty() && budget == null

    val recordCount: Int get() = categories.size + expenses.size + (if (budget != null) 1 else 0)

    companion object {
        const val KIND_PUSH = "PUSH"
        const val KIND_ACK = "ACK"
    }
}

object SyncCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(bundle: SyncBundle): ByteArray = json.encodeToString(SyncBundle.serializer(), bundle).toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): SyncBundle = json.decodeFromString(SyncBundle.serializer(), bytes.toString(Charsets.UTF_8))
}

// ---------- Conversión dominio <-> DTO ----------

fun Expense.toDto() = ExpenseDto(id, Money.toPlain(amount), categoryId, dateTime, description, createdFrom.name, updatedAt, deleted)

fun ExpenseDto.toDomain(status: SyncStatus) = Expense(
    id = id,
    amount = Money.normalize(BigDecimal(amount)),
    categoryId = categoryId,
    dateTime = dateTime,
    description = description,
    createdFrom = runCatching { DeviceSource.valueOf(createdFrom) }.getOrDefault(DeviceSource.PHONE),
    updatedAt = updatedAt,
    syncStatus = status,
    deleted = deleted,
)

fun Category.toDto() = CategoryDto(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted)

fun CategoryDto.toDomain(status: SyncStatus) =
    Category(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted, status)

fun Budget.toDto() = BudgetDto(
    id, Money.toPlain(amount), period.name, alertAt80, alertAt100,
    customAlertAmount?.let { Money.toPlain(it) }, updatedAt,
)

/** Las fechas del periodo se recalculan localmente al recibir el presupuesto. */
fun BudgetDto.toDomain(status: SyncStatus, startDate: LocalDate, endDate: LocalDate) = Budget(
    id = id,
    amount = Money.normalize(BigDecimal(amount)),
    period = runCatching { BudgetPeriod.valueOf(period) }.getOrDefault(BudgetPeriod.MONTHLY),
    startDate = startDate,
    endDate = endDate,
    alertAt80 = alertAt80,
    alertAt100 = alertAt100,
    customAlertAmount = customAlertAmount?.let { Money.normalize(BigDecimal(it)) },
    updatedAt = updatedAt,
    syncStatus = status,
)
