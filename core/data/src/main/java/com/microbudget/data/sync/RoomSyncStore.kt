package com.microbudget.data.sync

import androidx.room.withTransaction
import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.toDto
import com.microbudget.data.local.toEntity
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.sync.BudgetDto
import com.microbudget.domain.sync.CategoryDto
import com.microbudget.domain.sync.ExpenseDto
import com.microbudget.domain.sync.SyncStore
import java.time.Clock
import java.time.LocalDate

/** Implementación de [SyncStore] sobre Room. */
class RoomSyncStore(private val db: MicroBudgetDatabase, private val clock: Clock) : SyncStore {
    private val expenses = db.expenseDao()
    private val categories = db.categoryDao()
    private val budgets = db.budgetDao()

    override suspend fun <R> transaction(block: suspend () -> R): R = db.withTransaction { block() }

    override suspend fun pendingExpenses() = expenses.pending().map { it.toDto() }
    override suspend fun allExpenses() = expenses.all().map { it.toDto() }
    override suspend fun expense(id: String) = expenses.get(id)?.toDto()
    override suspend fun saveExpense(dto: ExpenseDto, status: SyncStatus) = expenses.upsert(dto.toEntity(status))
    override suspend fun markExpenseSynced(id: String, updatedAt: Long) = expenses.markSynced(id, updatedAt)

    override suspend fun pendingCategories() = categories.pending().map { it.toDto() }
    override suspend fun allCategories() = categories.all().map { it.toDto() }
    override suspend fun category(id: String) = categories.get(id)?.toDto()
    override suspend fun markCategorySynced(id: String, updatedAt: Long) = categories.markSynced(id, updatedAt)

    override suspend fun saveCategory(dto: CategoryDto, status: SyncStatus) {
        categories.upsert(dto.toEntity(status))
        // Misma regla que al borrar localmente: los gastos de una categoría eliminada pasan a «Otros».
        if (dto.deleted && dto.id != DefaultCategories.OTHER) {
            expenses.reassignCategory(dto.id, DefaultCategories.OTHER, clock.millis())
        }
    }

    override suspend fun budget(): BudgetDto? = budgets.get(Budget.MAIN_ID)?.toDto()
    override suspend fun isBudgetPending() = budgets.get(Budget.MAIN_ID)?.syncStatus == SyncStatus.PENDING.name
    override suspend fun saveBudget(dto: BudgetDto, status: SyncStatus) = budgets.upsert(dto.toEntity(status, LocalDate.now(clock)))
    override suspend fun markBudgetSynced(updatedAt: Long) = budgets.markSynced(Budget.MAIN_ID, updatedAt)
}
