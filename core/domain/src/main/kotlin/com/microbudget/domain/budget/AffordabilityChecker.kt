package com.microbudget.domain.budget

import com.microbudget.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

enum class AffordVerdict { YES, TIGHT, NO }

data class AffordabilityResult(
    val verdict: AffordVerdict,
    val amount: BigDecimal,
    val availableAfter: BigDecimal,
    val percentAfter: Int,
    val dailyAllowanceAfter: BigDecimal,
    val title: String,
    val message: String,
)

/**
 * Función «¿Me alcanza?»: simula un gasto SIN guardarlo.
 * - YES: después del gasto se queda por debajo del 80 %.
 * - TIGHT: alcanza, pero se llega al 80 %-100 %.
 * - NO: el gasto haría exceder el presupuesto.
 */
object AffordabilityChecker {

    fun check(summary: BudgetSummary, amount: BigDecimal): AffordabilityResult {
        val a = Money.normalize(amount)
        val budget = summary.budgetAmount
        val spentAfter = summary.spent + a
        val availableAfter = budget - spentAfter
        val percentAfter = if (budget.signum() > 0) {
            spentAfter.multiply(BigDecimal(100)).divide(budget, 0, RoundingMode.HALF_UP).toInt()
        } else 999
        val dailyAfter = if (availableAfter.signum() > 0) {
            availableAfter.divide(BigDecimal(summary.daysRemaining), 2, RoundingMode.DOWN)
        } else Money.ZERO

        val verdict = when {
            spentAfter > budget -> AffordVerdict.NO
            spentAfter.multiply(BigDecimal(100)) >= budget.multiply(BigDecimal(80)) -> AffordVerdict.TIGHT
            else -> AffordVerdict.YES
        }
        val (title, message) = when (verdict) {
            AffordVerdict.YES -> "Sí te alcanza" to
                "Te quedarían ${Money.format(availableAfter)} (${Money.format(dailyAfter)}/día por ${summary.daysRemaining} días)."
            AffordVerdict.TIGHT -> "Te alcanza, justo" to
                "Quedarías al $percentAfter % con ${Money.format(availableAfter)} para ${summary.daysRemaining} días."
            AffordVerdict.NO -> "No te alcanza" to
                "Te faltarían ${Money.format(availableAfter.negate())}. Disponible: ${Money.format(summary.available.max(Money.ZERO))}."
        }
        return AffordabilityResult(verdict, a, availableAfter, percentAfter, dailyAfter, title, message)
    }
}
