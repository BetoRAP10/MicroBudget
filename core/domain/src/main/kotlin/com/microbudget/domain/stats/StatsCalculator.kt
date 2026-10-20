package com.microbudget.domain.stats

import com.microbudget.domain.budget.PeriodRange
import com.microbudget.domain.model.Expense
import com.microbudget.domain.money.Money
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CategoryTotal(
    val categoryId: String,
    val total: BigDecimal,
    val count: Int,
    /** Proporción 0..1 sobre el total del conjunto. */
    val share: Double,
)

data class DayTotal(val date: LocalDate, val total: BigDecimal)

/** Estadísticas para historial y gráficas. Se recalculan desde los gastos guardados. */
object StatsCalculator {

    fun byCategory(expenses: List<Expense>): List<CategoryTotal> {
        val active = expenses.filter { !it.deleted }
        val grand = active.fold(Money.ZERO) { acc, e -> acc + e.amount }
        return active.groupBy { it.categoryId }
            .map { (cat, list) ->
                val t = list.fold(Money.ZERO) { acc, e -> acc + e.amount }
                CategoryTotal(cat, t, list.size, if (grand.signum() > 0) t.toDouble() / grand.toDouble() else 0.0)
            }
            .sortedByDescending { it.total }
    }

    fun byDay(expenses: List<Expense>, range: PeriodRange, zone: ZoneId): List<DayTotal> {
        val totals = expenses.filter { !it.deleted }
            .groupBy { Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate() }
            .mapValues { (_, l) -> l.fold(Money.ZERO) { acc, e -> acc + e.amount } }
        return generateSequence(range.start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(range.end) }
            .map { DayTotal(it, totals[it] ?: Money.ZERO) }
            .toList()
    }
}

/** Filtros del historial completo (celular). Fechas inclusive; null = sin límite. */
data class ExpenseFilter(
    val categoryIds: Set<String> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    fun apply(expenses: List<Expense>, zone: ZoneId): List<Expense> = expenses.filter { e ->
        if (e.deleted) return@filter false
        if (categoryIds.isNotEmpty() && e.categoryId !in categoryIds) return@filter false
        val d = Instant.ofEpochMilli(e.dateTime).atZone(zone).toLocalDate()
        (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to))
    }
}
