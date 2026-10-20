package com.microbudget.data.repository

import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.toDomain
import com.microbudget.data.local.toEntity
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.budget.BudgetCalculator
import com.microbudget.domain.budget.BudgetSummary
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.AmountValidation
import com.microbudget.domain.money.AmountValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

class BudgetRepository(
    private val db: MicroBudgetDatabase,
    private val clock: Clock,
    private val afterLocalChange: suspend () -> List<BudgetAlert>,
) {
    private val dao = db.budgetDao()

    val budget: Flow<Budget> = dao.observe(Budget.MAIN_ID).map { it?.toDomain() ?: defaultBudget(today()) }

    fun observeSummary(expenses: Flow<List<Expense>>): Flow<BudgetSummary> =
        combine(budget, expenses) { b, list -> BudgetCalculator.summarize(b, list, today(), clock.zone) }

    suspend fun current(): Budget = dao.get(Budget.MAIN_ID)?.toDomain() ?: defaultBudget(today())

    /** Crea el presupuesto inicial y actualiza startDate/endDate si empezó un periodo nuevo. */
    suspend fun ensureSeededAndCurrent() {
        dao.insertIfMissing(defaultBudget(today()).toEntity())
        val b = current()
        val range = PeriodCalculator.rangeFor(b.period, today())
        if (b.startDate != range.start || b.endDate != range.end) {
            // Cambio de periodo automático: no modifica la versión (updatedAt) porque es determinista.
            dao.upsert(b.copy(startDate = range.start, endDate = range.end).toEntity())
        }
    }

    suspend fun setBudget(amount: BigDecimal, period: BudgetPeriod): Result<Budget> {
        val valid = when (val v = AmountValidator.validate(amount)) {
            is AmountValidation.Invalid -> return Result.failure(IllegalArgumentException(v.reason))
            is AmountValidation.Valid -> v.amount
        }
        return save { it.copy(amount = valid, period = period) }
    }

    suspend fun setPeriod(period: BudgetPeriod): Result<Budget> = save { it.copy(period = period) }

    suspend fun setAlerts(alertAt80: Boolean, alertAt100: Boolean, customAmount: BigDecimal?): Result<Budget> {
        val custom = customAmount?.let {
            when (val v = AmountValidator.validate(it)) {
                is AmountValidation.Invalid -> return Result.failure(IllegalArgumentException("Límite personalizado: ${v.reason}"))
                is AmountValidation.Valid -> v.amount
            }
        }
        return save { it.copy(alertAt80 = alertAt80, alertAt100 = alertAt100, customAlertAmount = custom) }
    }

    private suspend fun save(change: (Budget) -> Budget): Result<Budget> {
        val current = current()
        val changed = change(current)
        val range = PeriodCalculator.rangeFor(changed.period, today())
        val updated = changed.copy(
            startDate = range.start,
            endDate = range.end,
            updatedAt = maxOf(clock.millis(), current.updatedAt + 1),
            syncStatus = SyncStatus.PENDING,
        )
        dao.upsert(updated.toEntity())
        afterLocalChange()
        return Result.success(updated)
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    companion object {
        val DEFAULT_AMOUNT = BigDecimal("2000.00")

        /** Presupuesto inicial idéntico en ambos dispositivos (updatedAt = 0). */
        fun defaultBudget(today: LocalDate): Budget {
            val range = PeriodCalculator.rangeFor(BudgetPeriod.MONTHLY, today)
            return Budget(
                amount = DEFAULT_AMOUNT, period = BudgetPeriod.MONTHLY,
                startDate = range.start, endDate = range.end,
                alertAt80 = true, alertAt100 = true, customAlertAmount = null,
                updatedAt = 0L, syncStatus = SyncStatus.SYNCED,
            )
        }
    }
}
