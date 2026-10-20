package com.microbudget.data.repository

import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.toDomain
import com.microbudget.data.local.toEntity
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.AmountValidation
import com.microbudget.domain.money.AmountValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

sealed interface SaveResult {
    data class Success(val expense: Expense, val alerts: List<BudgetAlert>) : SaveResult
    data class Error(val message: String) : SaveResult
}

/**
 * Reglas al crear / editar / eliminar un gasto:
 * 1. Se valida el importe (positivo, máx. 2 decimales) y que la categoría exista.
 * 2. Se guarda con updatedAt = ahora y syncStatus = PENDING.
 * 3. Eliminar = marcar `deleted` (lápida) para que el borrado llegue al otro dispositivo.
 * 4. Después de cada cambio se reevalúan las alertas y se intenta sincronizar.
 *    Los totales y estadísticas no se guardan: se recalculan desde la tabla, así que
 *    siempre reflejan el cambio.
 */
class ExpenseRepository(
    private val db: MicroBudgetDatabase,
    private val clock: Clock,
    private val source: DeviceSource,
    private val afterLocalChange: suspend () -> List<BudgetAlert>,
) {
    private val dao = db.expenseDao()

    fun observeActive(): Flow<List<Expense>> = dao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeRecent(limit: Int): Flow<List<Expense>> = dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: String): Expense? = dao.get(id)?.takeIf { !it.deleted }?.toDomain()

    suspend fun add(
        amount: BigDecimal,
        categoryId: String,
        dateTime: Long = clock.millis(),
        description: String? = null,
    ): SaveResult {
        val valid = when (val v = AmountValidator.validate(amount)) {
            is AmountValidation.Invalid -> return SaveResult.Error(v.reason)
            is AmountValidation.Valid -> v.amount
        }
        if (!categoryExists(categoryId)) return SaveResult.Error("Categoría no válida")
        val now = clock.millis()
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amount = valid,
            categoryId = categoryId,
            dateTime = dateTime,
            description = description?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DESCRIPTION),
            createdFrom = source,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
        )
        dao.upsert(expense.toEntity())
        return SaveResult.Success(expense, afterLocalChange())
    }

    suspend fun update(
        id: String,
        amount: BigDecimal,
        categoryId: String,
        dateTime: Long,
        description: String?,
    ): SaveResult {
        val current = dao.get(id)?.takeIf { !it.deleted }?.toDomain() ?: return SaveResult.Error("El gasto ya no existe")
        val valid = when (val v = AmountValidator.validate(amount)) {
            is AmountValidation.Invalid -> return SaveResult.Error(v.reason)
            is AmountValidation.Valid -> v.amount
        }
        if (!categoryExists(categoryId)) return SaveResult.Error("Categoría no válida")
        val updated = current.copy(
            amount = valid,
            categoryId = categoryId,
            dateTime = dateTime,
            description = description?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DESCRIPTION),
            updatedAt = nextVersion(current.updatedAt),
            syncStatus = SyncStatus.PENDING,
        )
        dao.upsert(updated.toEntity())
        return SaveResult.Success(updated, afterLocalChange())
    }

    suspend fun delete(id: String): SaveResult {
        val current = dao.get(id)?.takeIf { !it.deleted }?.toDomain() ?: return SaveResult.Error("El gasto ya no existe")
        val tomb = current.copy(deleted = true, updatedAt = nextVersion(current.updatedAt), syncStatus = SyncStatus.PENDING)
        dao.upsert(tomb.toEntity())
        return SaveResult.Success(tomb, afterLocalChange())
    }

    private suspend fun categoryExists(id: String) = db.categoryDao().get(id)?.deleted == false

    /** La nueva versión siempre es mayor que la anterior aunque el reloj del sistema retroceda. */
    private fun nextVersion(previous: Long) = maxOf(clock.millis(), previous + 1)

    companion object {
        const val MAX_DESCRIPTION = 80
    }
}
