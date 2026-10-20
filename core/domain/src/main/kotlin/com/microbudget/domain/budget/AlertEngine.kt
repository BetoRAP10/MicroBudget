package com.microbudget.domain.budget

import com.microbudget.domain.model.Budget
import com.microbudget.domain.money.Money
import java.math.BigDecimal

enum class AlertType { PERCENT_80, PERCENT_100, CUSTOM }

data class BudgetAlert(
    /** Clave estable dentro del periodo ("P80", "P100", "CUSTOM:150000"). */
    val key: String,
    val type: AlertType,
    val title: String,
    val message: String,
    /** Versión corta para la pantalla del reloj. */
    val shortMessage: String = message,
)

data class AlertEvaluation(
    /** Alertas nuevas que hay que mostrar ahora. */
    val toNotify: List<BudgetAlert>,
    /** Claves que se deben guardar como "ya notificadas" en este periodo. */
    val toMarkNotified: Set<String>,
    /** Claves que dejan de cumplirse (p. ej. se borró un gasto) y se rearman. */
    val toRearm: Set<String>,
)

/**
 * Motor de alertas sin duplicados.
 *
 * Reglas:
 * 1. Cada umbral se notifica UNA sola vez por periodo (se guarda su clave junto con la clave del periodo).
 * 2. Si un gasto hace cruzar 80 % y 100 % a la vez, solo se muestra la más grave, pero ambas
 *    quedan marcadas para no volver a mostrarse.
 * 3. Si el gasto baja de un umbral (se editó o eliminó un gasto), el umbral se rearma y podrá
 *    avisar de nuevo si se vuelve a cruzar.
 * 4. Al empezar un periodo nuevo cambia la clave del periodo, así que todo se restablece.
 */
object AlertEngine {
    const val KEY_80 = "P80"
    const val KEY_100 = "P100"
    private const val CUSTOM_PREFIX = "CUSTOM:"

    fun customKey(amount: BigDecimal) = CUSTOM_PREFIX + Money.toCents(amount)

    fun evaluate(budget: Budget, summary: BudgetSummary, alreadyNotified: Set<String>): AlertEvaluation {
        val amount = summary.budgetAmount
        val spent = summary.spent
        val reached = mutableMapOf<String, BudgetAlert>()

        if (budget.alertAt80 && amount.signum() > 0 &&
            spent.multiply(BigDecimal(100)) >= amount.multiply(BigDecimal(80))
        ) {
            val left = summary.available.max(Money.ZERO)
            reached[KEY_80] = BudgetAlert(
                KEY_80, AlertType.PERCENT_80,
                "80 % del presupuesto",
                "Llevas ${Money.format(spent)} de ${Money.format(amount)} (${summary.percentUsed} %). " +
                    "Te quedan ${Money.format(left)}.",
                "Te quedan ${Money.format(left)}",
            )
        }
        if (budget.alertAt100 && amount.signum() > 0 && spent >= amount) {
            val (msg, short) = if (summary.isExceeded) {
                "Excediste tu presupuesto por ${Money.format(summary.overspent)}." to "Exceso: ${Money.format(summary.overspent)}"
            } else {
                "Alcanzaste el 100 % de tu presupuesto (${Money.format(amount)})." to "Llegaste al 100 %"
            }
            reached[KEY_100] = BudgetAlert(KEY_100, AlertType.PERCENT_100, "Presupuesto agotado", msg, short)
        }
        val custom = budget.customAlertAmount
        if (custom != null && custom.signum() > 0 && spent >= custom) {
            val key = customKey(custom)
            reached[key] = BudgetAlert(
                key, AlertType.CUSTOM,
                "Límite personalizado",
                "Tu gasto (${Money.format(spent)}) alcanzó tu límite de ${Money.format(custom)}.",
                "Llegaste a ${Money.format(custom)}",
            )
        }

        val newOnes = reached.filterKeys { it !in alreadyNotified }
        val percentNew = listOfNotNull(newOnes[KEY_100] ?: newOnes[KEY_80]) // solo la más grave
        val customNew = newOnes.values.filter { it.type == AlertType.CUSTOM }

        return AlertEvaluation(
            toNotify = percentNew + customNew,
            toMarkNotified = newOnes.keys,
            toRearm = alreadyNotified - reached.keys,
        )
    }
}
