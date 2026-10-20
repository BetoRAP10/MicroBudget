package com.microbudget.data.alerts

import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.NotifiedAlertEntity
import com.microbudget.data.local.toDomain
import com.microbudget.domain.budget.AlertEngine
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.budget.BudgetCalculator
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

/**
 * Evalúa los umbrales después de cada cambio y muestra cada alerta una sola vez por periodo.
 * El registro de umbrales notificados se guarda en la tabla `notified_alerts`.
 */
class AlertService(
    private val db: MicroBudgetDatabase,
    private val clock: Clock,
    private val notifier: AlertNotifier,
) {
    private val mutex = Mutex()
    private val _latest = MutableStateFlow<BudgetAlert?>(null)

    /** Última alerta disparada; la UI la muestra como aviso aunque no haya permiso de notificaciones. */
    val latest: StateFlow<BudgetAlert?> = _latest.asStateFlow()

    fun dismissLatest() { _latest.value = null }

    suspend fun evaluateAndNotify(): List<BudgetAlert> = mutex.withLock {
        val budget: Budget = db.budgetDao().get(Budget.MAIN_ID)?.toDomain() ?: return emptyList()
        val expenses = db.expenseDao().active().map { it.toDomain() }
        val summary = BudgetCalculator.summarize(budget, expenses, LocalDate.now(clock), clock.zone)
        val periodKey = PeriodCalculator.periodKey(budget.period, summary.range)
        val alertDao = db.alertDao()

        alertDao.deleteOtherPeriods(periodKey) // nuevo periodo -> se restablecen los umbrales
        val evaluation = AlertEngine.evaluate(budget, summary, alertDao.notifiedKeys(periodKey).toSet())
        if (evaluation.toMarkNotified.isNotEmpty()) {
            alertDao.insert(evaluation.toMarkNotified.map { NotifiedAlertEntity(periodKey, it, clock.millis()) })
        }
        if (evaluation.toRearm.isNotEmpty()) alertDao.delete(periodKey, evaluation.toRearm.toList())

        evaluation.toNotify.forEach { notifier.notify(it) }
        evaluation.toNotify.lastOrNull()?.let { _latest.value = it }
        evaluation.toNotify
    }

    /** Para pruebas en clase: olvida qué alertas se enviaron en este periodo. */
    suspend fun resetNotified() = mutex.withLock { db.alertDao().deleteAll() }
}
