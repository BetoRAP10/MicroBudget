package com.microbudget.domain.budget

import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.Expense
import com.microbudget.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BudgetStatus {
    /** Menos del 80 % consumido (verde). */
    OK,

    /** 80 % o más, sin exceder (naranja). */
    WARNING,

    /** Gasto mayor que el presupuesto (rojo). */
    EXCEEDED,
}

data class BudgetSummary(
    val budgetAmount: BigDecimal,
    val spent: BigDecimal,
    /** Presupuesto - gastado. Puede ser negativo (exceso). */
    val available: BigDecimal,
    /** Porcentaje consumido, redondeado. Puede ser > 100. */
    val percentUsed: Int,
    /** Proporción exacta 0..n, útil para barras de progreso. */
    val ratio: Double,
    val dailyAverage: BigDecimal,
    /** Lo que se puede gastar por día en lo que resta del periodo (0 si ya no queda). */
    val dailyAllowance: BigDecimal,
    val daysElapsed: Int,
    /** Días restantes incluyendo hoy. */
    val daysRemaining: Int,
    val expenseCount: Int,
    val status: BudgetStatus,
    val range: PeriodRange,
    val periodLabel: String,
) {
    val isExceeded: Boolean get() = available.signum() < 0

    /** Monto excedido (positivo) o cero. */
    val overspent: BigDecimal get() = if (isExceeded) available.negate() else Money.ZERO
}

/**
 * Cálculos financieros del periodo actual.
 * Regla: los totales SIEMPRE se calculan a partir de los gastos guardados (no se acumulan
 * contadores), así que crear, editar o eliminar un gasto actualiza todo automáticamente.
 */
object BudgetCalculator {

    fun expensesInRange(expenses: List<Expense>, range: PeriodRange, zone: ZoneId): List<Expense> =
        expenses.filter { !it.deleted && Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate() in range }

    fun total(expenses: List<Expense>): BigDecimal =
        expenses.filter { !it.deleted }.fold(Money.ZERO) { acc, e -> acc + e.amount }

    fun summarize(budget: Budget, allExpenses: List<Expense>, today: LocalDate, zone: ZoneId): BudgetSummary {
        val range = PeriodCalculator.rangeFor(budget.period, today)
        val inPeriod = expensesInRange(allExpenses, range, zone)
        val spent = total(inPeriod)
        val amount = Money.normalize(budget.amount)
        val available = amount - spent

        val ratio = if (amount.signum() > 0) spent.toDouble() / amount.toDouble() else if (spent.signum() > 0) 9.99 else 0.0
        val percent = if (amount.signum() > 0) {
            spent.multiply(BigDecimal(100)).divide(amount, 0, RoundingMode.HALF_UP).toInt()
        } else if (spent.signum() > 0) 999 else 0

        val elapsed = (ChronoUnit.DAYS.between(range.start, today) + 1).toInt().coerceIn(1, range.totalDays)
        val remaining = (range.totalDays - elapsed + 1).coerceAtLeast(1)
        val dailyAverage = spent.divide(BigDecimal(elapsed), 2, RoundingMode.HALF_UP)
        val dailyAllowance = if (available.signum() > 0) {
            available.divide(BigDecimal(remaining), 2, RoundingMode.DOWN)
        } else Money.ZERO

        val status = when {
            spent > amount -> BudgetStatus.EXCEEDED
            amount.signum() > 0 && spent.multiply(BigDecimal(100)) >= amount.multiply(BigDecimal(80)) -> BudgetStatus.WARNING
            else -> BudgetStatus.OK
        }

        return BudgetSummary(
            budgetAmount = amount,
            spent = spent,
            available = available,
            percentUsed = percent,
            ratio = ratio,
            dailyAverage = dailyAverage,
            dailyAllowance = dailyAllowance,
            daysElapsed = elapsed,
            daysRemaining = remaining,
            expenseCount = inPeriod.size,
            status = status,
            range = range,
            periodLabel = PeriodCalculator.label(budget.period, range),
        )
    }
}
