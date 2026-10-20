package com.microbudget.wear

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.data.alerts.Haptics
import com.microbudget.data.repository.SaveResult
import com.microbudget.data.sync.SyncUiState
import com.microbudget.domain.budget.AffordabilityChecker
import com.microbudget.domain.budget.AffordabilityResult
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.budget.BudgetSummary
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.Expense
import com.microbudget.domain.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Estado y acciones de la app del reloj. Toda la lógica vive en core:domain / core:data. */
class WearViewModel(private val graph: MicroBudgetGraph) : ViewModel() {
    val zone: ZoneId get() = graph.clock.zone

    val summary: StateFlow<BudgetSummary?> = graph.budgetRepository
        .observeSummary(graph.expenseRepository.observeActive())
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val budget: StateFlow<Budget?> = graph.budgetRepository.budget.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** El reloj solo muestra los 10 movimientos más recientes. */
    val recent: StateFlow<List<Expense>> = graph.expenseRepository.observeRecent(RECENT_LIMIT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> = graph.categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sync: StateFlow<SyncUiState> = graph.syncManager.state

    data class SaveOutcome(val expense: Expense, val alerts: List<BudgetAlert>)

    private val _lastSave = MutableStateFlow<SaveOutcome?>(null)
    val lastSave: StateFlow<SaveOutcome?> = _lastSave.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun category(id: String): Category =
        categories.value.firstOrNull { it.id == id }
            ?: DefaultCategories.all.firstOrNull { it.id == id }
            ?: DefaultCategories.fallback

    fun saveExpense(cents: Long, categoryId: String, onSaved: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            when (val r = graph.expenseRepository.add(Money.fromCents(cents), categoryId)) {
                is SaveResult.Success -> {
                    _lastSave.value = SaveOutcome(r.expense, r.alerts)
                    if (r.alerts.isEmpty()) Haptics.success(graph.appContext) else Haptics.warning(graph.appContext)
                    onSaved()
                }
                is SaveResult.Error -> toast(r.message)
            }
            _busy.value = false
        }
    }

    fun affordability(cents: Long): AffordabilityResult? =
        summary.value?.let { AffordabilityChecker.check(it, Money.fromCents(cents)) }

    fun deleteExpense(id: String, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = graph.expenseRepository.delete(id)) {
            is SaveResult.Success -> { toast("Gasto eliminado"); onDone() }
            is SaveResult.Error -> toast(r.message)
        }
    }

    fun setBudgetAmount(cents: Long) = viewModelScope.launch {
        val period = budget.value?.period ?: BudgetPeriod.MONTHLY
        graph.budgetRepository.setBudget(Money.fromCents(cents), period)
            .onSuccess { toast("Presupuesto: ${Money.format(it.amount)}") }
            .onFailure { toast(it.message ?: "Error") }
    }

    fun togglePeriod() = viewModelScope.launch {
        val next = if (budget.value?.period == BudgetPeriod.WEEKLY) BudgetPeriod.MONTHLY else BudgetPeriod.WEEKLY
        graph.budgetRepository.setPeriod(next)
    }

    fun setAlerts(at80: Boolean? = null, at100: Boolean? = null, custom: Long? = null, clearCustom: Boolean = false) =
        viewModelScope.launch {
            val b = budget.value ?: return@launch
            val customAmount = when {
                clearCustom -> null
                custom != null -> Money.fromCents(custom)
                else -> b.customAlertAmount
            }
            graph.budgetRepository.setAlerts(at80 ?: b.alertAt80, at100 ?: b.alertAt100, customAmount)
                .onFailure { toast(it.message ?: "Error") }
        }

    fun syncNow() = viewModelScope.launch {
        val s = graph.syncManager.syncNow()
        toast(s.message)
    }

    fun loadDemo() = viewModelScope.launch { toast(graph.demoData.load()) }

    fun clearAll() = viewModelScope.launch { toast(graph.demoData.clearAll()) }

    fun resetAlerts() = viewModelScope.launch {
        graph.alertService.resetNotified()
        toast("Alertas rearmadas para este periodo")
    }

    fun testNotification() {
        val shown = graph.notifier.notifyTest()
        val vibrated = Haptics.warning(graph.appContext)
        toast(
            (if (shown) "Notificación enviada" else "Sin permiso de notificaciones") +
                (if (vibrated) "" else " · sin vibrador"),
        )
    }

    fun hasNotificationPermission() = graph.notifier.hasPermission()

    private fun toast(msg: String) {
        viewModelScope.launch { Toast.makeText(graph.appContext, msg, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        const val RECENT_LIMIT = 10
    }
}
