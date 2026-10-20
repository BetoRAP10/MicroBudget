package com.microbudget.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.data.repository.SaveResult
import com.microbudget.data.sync.SyncUiState
import com.microbudget.domain.budget.AffordabilityChecker
import com.microbudget.domain.budget.AffordabilityResult
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.budget.BudgetCalculator
import com.microbudget.domain.budget.BudgetSummary
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.Expense
import com.microbudget.domain.money.AmountValidation
import com.microbudget.domain.money.AmountValidator
import com.microbudget.domain.stats.ExpenseFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Filtro rápido de fechas del historial. */
enum class DatePreset(val label: String) {
    ALL("Todo"), TODAY("Hoy"), PERIOD("Este periodo"), LAST7("7 días"), LAST30("30 días"), CUSTOM("Rango…")
}

data class HistoryFilterState(
    val categoryIds: Set<String> = emptySet(),
    val preset: DatePreset = DatePreset.ALL,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
)

class MobileViewModel(private val graph: MicroBudgetGraph) : ViewModel() {
    val zone: ZoneId get() = graph.clock.zone

    val expenses: StateFlow<List<Expense>> = graph.expenseRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val budget: StateFlow<Budget?> = graph.budgetRepository.budget.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val summary: StateFlow<BudgetSummary?> = graph.budgetRepository.observeSummary(graph.expenseRepository.observeActive())
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Gastos del periodo actual (para gráficas). */
    val periodExpenses: StateFlow<List<Expense>> = combine(expenses, summary) { list, s ->
        if (s == null) emptyList() else BudgetCalculator.expensesInRange(list, s.range, zone)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> = graph.categoryRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sync: StateFlow<SyncUiState> = graph.syncManager.state
    val latestAlert: StateFlow<BudgetAlert?> = graph.alertService.latest

    private val _filter = MutableStateFlow(HistoryFilterState())
    val filter: StateFlow<HistoryFilterState> = _filter.asStateFlow()

    val filtered: StateFlow<List<Expense>> = combine(expenses, _filter, summary) { list, f, s ->
        val today = LocalDate.now(graph.clock)
        val (from, to) = when (f.preset) {
            DatePreset.ALL -> null to null
            DatePreset.TODAY -> today to today
            DatePreset.PERIOD -> (s?.range?.start ?: today) to (s?.range?.end ?: today)
            DatePreset.LAST7 -> today.minusDays(6) to today
            DatePreset.LAST30 -> today.minusDays(29) to today
            DatePreset.CUSTOM -> f.customFrom to f.customTo
        }
        ExpenseFilter(f.categoryIds, from, to).apply(list, zone)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun say(msg: String) { _messages.tryEmit(msg) }

    fun category(id: String): Category =
        categories.value.firstOrNull { it.id == id }
            ?: DefaultCategories.all.firstOrNull { it.id == id }
            ?: DefaultCategories.fallback

    // ---------- Gastos ----------

    /** Crea (id == null) o edita un gasto. Devuelve un error de validación o null si se guardó. */
    suspend fun saveExpense(id: String?, amountText: String, categoryId: String?, dateTime: Long, description: String): String? {
        val amount = when (val v = AmountValidator.validate(amountText)) {
            is AmountValidation.Invalid -> return v.reason
            is AmountValidation.Valid -> v.amount
        }
        if (categoryId == null) return "Elige una categoría"
        val result = if (id == null) {
            graph.expenseRepository.add(amount, categoryId, dateTime, description)
        } else {
            graph.expenseRepository.update(id, amount, categoryId, dateTime, description)
        }
        return when (result) {
            is SaveResult.Error -> result.message
            is SaveResult.Success -> {
                say(if (id == null) "Gasto guardado" else "Gasto actualizado")
                null
            }
        }
    }

    fun deleteExpense(id: String) = viewModelScope.launch {
        when (val r = graph.expenseRepository.delete(id)) {
            is SaveResult.Success -> say("Gasto eliminado")
            is SaveResult.Error -> say(r.message)
        }
    }

    // ---------- Filtros ----------

    fun toggleCategoryFilter(id: String) = _filter.value.let { f ->
        _filter.value = f.copy(categoryIds = if (id in f.categoryIds) f.categoryIds - id else f.categoryIds + id)
    }

    fun clearCategoryFilter() { _filter.value = _filter.value.copy(categoryIds = emptySet()) }

    fun setPreset(p: DatePreset) { _filter.value = _filter.value.copy(preset = p) }

    fun setCustomRange(from: LocalDate, to: LocalDate) {
        _filter.value = _filter.value.copy(preset = DatePreset.CUSTOM, customFrom = from, customTo = to)
    }

    // ---------- Presupuesto y alertas ----------

    fun setBudget(amountText: String, period: BudgetPeriod) = viewModelScope.launch {
        when (val v = AmountValidator.validate(amountText)) {
            is AmountValidation.Invalid -> say(v.reason)
            is AmountValidation.Valid -> graph.budgetRepository.setBudget(v.amount, period)
                .onSuccess { say("Presupuesto guardado") }
                .onFailure { say(it.message ?: "Error") }
        }
    }

    fun setAlerts(at80: Boolean, at100: Boolean, customEnabled: Boolean, customText: String) = viewModelScope.launch {
        val custom = if (!customEnabled) null else when (val v = AmountValidator.validate(customText)) {
            is AmountValidation.Invalid -> { say("Límite personalizado: ${v.reason}"); return@launch }
            is AmountValidation.Valid -> v.amount
        }
        graph.budgetRepository.setAlerts(at80, at100, custom)
            .onSuccess { say("Alertas guardadas") }
            .onFailure { say(it.message ?: "Error") }
    }

    fun affordability(text: String): Result<AffordabilityResult> {
        val s = summary.value ?: return Result.failure(IllegalStateException("Cargando…"))
        return when (val v = AmountValidator.validate(text)) {
            is AmountValidation.Invalid -> Result.failure(IllegalArgumentException(v.reason))
            is AmountValidation.Valid -> Result.success(AffordabilityChecker.check(s, v.amount))
        }
    }

    fun dismissAlert() = graph.alertService.dismissLatest()

    fun resetAlerts() = viewModelScope.launch {
        graph.alertService.resetNotified()
        say("Alertas rearmadas para este periodo")
    }

    fun testNotification() {
        say(if (graph.notifier.notifyTest()) "Notificación enviada" else "Sin permiso de notificaciones")
    }

    fun hasNotificationPermission() = graph.notifier.hasPermission()

    // ---------- Categorías ----------

    fun saveCategory(id: String?, name: String, icon: String, color: Long, onDone: () -> Unit) = viewModelScope.launch {
        val r = if (id == null) graph.categoryRepository.add(name, icon, color) else graph.categoryRepository.update(id, name, icon, color)
        r.onSuccess { say("Categoría guardada"); onDone() }.onFailure { say(it.message ?: "Error") }
    }

    fun deleteCategory(id: String) = viewModelScope.launch {
        graph.categoryRepository.delete(id)
            .onSuccess { moved -> say(if (moved > 0) "Categoría eliminada; $moved gastos pasaron a «Otros»" else "Categoría eliminada") }
            .onFailure { say(it.message ?: "Error") }
    }

    // ---------- Sincronización y datos de prueba ----------

    fun syncNow(full: Boolean = false) = viewModelScope.launch { say(graph.syncManager.syncNow(full).message) }

    fun loadDemo() = viewModelScope.launch { say(graph.demoData.load()) }

    fun clearAll() = viewModelScope.launch { say(graph.demoData.clearAll()) }
}
