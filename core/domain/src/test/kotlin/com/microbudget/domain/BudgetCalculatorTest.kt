package com.microbudget.domain

import com.microbudget.domain.TestData.budget
import com.microbudget.domain.TestData.expense
import com.microbudget.domain.TestData.today
import com.microbudget.domain.TestData.zone
import com.microbudget.domain.budget.AffordVerdict
import com.microbudget.domain.budget.AffordabilityChecker
import com.microbudget.domain.budget.BudgetCalculator
import com.microbudget.domain.budget.BudgetStatus
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.stats.ExpenseFilter
import com.microbudget.domain.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetCalculatorTest {

    /** Caso 3: el presupuesto restante se calcula bien. */
    @Test fun `calcula gastado, disponible, porcentaje y promedio diario`() {
        val expenses = listOf(expense("500.00"), expense("250.50"), expense("49.50"))
        val s = BudgetCalculator.summarize(budget("2000.00"), expenses, today, zone)
        assertEquals(BigDecimal("2000.00"), s.budgetAmount)
        assertEquals(BigDecimal("800.00"), s.spent)
        assertEquals(BigDecimal("1200.00"), s.available)
        assertEquals(40, s.percentUsed)
        assertEquals(18, s.daysElapsed)                       // 1 al 18 de octubre
        assertEquals(BigDecimal("44.44"), s.dailyAverage)     // 800 / 18
        assertEquals(14, s.daysRemaining)                     // 18 al 31, incluye hoy
        assertEquals(BudgetStatus.OK, s.status)
    }

    @Test fun `muestra el exceso cuando el resultado es negativo`() {
        val s = BudgetCalculator.summarize(budget("100.00"), listOf(expense("130.25")), today, zone)
        assertEquals(BigDecimal("-30.25"), s.available)
        assertTrue(s.isExceeded)
        assertEquals(BigDecimal("30.25"), s.overspent)
        assertEquals(130, s.percentUsed)
        assertEquals(BudgetStatus.EXCEEDED, s.status)
        assertEquals(BigDecimal("0.00"), s.dailyAllowance)
    }

    @Test fun `ignora gastos de otros periodos y gastos eliminados`() {
        val expenses = listOf(
            expense("100.00"),
            expense("999.00", date = LocalDate.of(2026, 9, 30)),     // mes anterior
            expense("50.00").copy(deleted = true),                   // eliminado
        )
        val s = BudgetCalculator.summarize(budget("1000.00"), expenses, today, zone)
        assertEquals(BigDecimal("100.00"), s.spent)
        assertEquals(1, s.expenseCount)
    }

    @Test fun `periodo semanal empieza en lunes`() {
        val range = PeriodCalculator.rangeFor(BudgetPeriod.WEEKLY, today) // domingo 18 oct 2026
        assertEquals(LocalDate.of(2026, 10, 12), range.start)
        assertEquals(LocalDate.of(2026, 10, 18), range.end)
        val s = BudgetCalculator.summarize(
            budget("700.00", BudgetPeriod.WEEKLY),
            listOf(expense("70.00", date = LocalDate.of(2026, 10, 12)), expense("70.00", date = LocalDate.of(2026, 10, 11))),
            today, zone,
        )
        assertEquals(BigDecimal("70.00"), s.spent)
        assertEquals(7, s.daysElapsed)
    }

    /** Caso 6: editar o eliminar un gasto actualiza los totales. */
    @Test fun `editar y eliminar recalcula totales y estadisticas`() {
        val a = expense("300.00", "food", id = "a")
        val b = expense("200.00", "transport", id = "b")
        val before = BudgetCalculator.summarize(budget("1000.00"), listOf(a, b), today, zone)
        assertEquals(BigDecimal("500.00"), before.spent)

        val edited = listOf(a.copy(amount = BigDecimal("100.00"), categoryId = "shopping"), b)
        val afterEdit = BudgetCalculator.summarize(budget("1000.00"), edited, today, zone)
        assertEquals(BigDecimal("300.00"), afterEdit.spent)
        assertEquals(listOf("transport", "shopping"), StatsCalculator.byCategory(edited).map { it.categoryId })

        val deleted = listOf(edited[0], b.copy(deleted = true))
        val afterDelete = BudgetCalculator.summarize(budget("1000.00"), deleted, today, zone)
        assertEquals(BigDecimal("100.00"), afterDelete.spent)
        assertEquals(1, StatsCalculator.byCategory(deleted).size)
    }

    /** Caso 2: las categorías se asignan y agrupan correctamente. */
    @Test fun `agrupa por categoria y filtra por categoria y fecha`() {
        val list = listOf(
            expense("10.00", "food"), expense("15.00", "food"),
            expense("40.00", "transport", date = LocalDate.of(2026, 10, 1)),
        )
        val totals = StatsCalculator.byCategory(list).associateBy { it.categoryId }
        assertEquals(BigDecimal("25.00"), totals["food"]!!.total)
        assertEquals(2, totals["food"]!!.count)
        assertEquals(BigDecimal("40.00"), totals["transport"]!!.total)

        assertEquals(2, ExpenseFilter(categoryIds = setOf("food")).apply(list, zone).size)
        assertEquals(1, ExpenseFilter(from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 2)).apply(list, zone).size)
    }

    @Test fun `me alcanza - si, justo y no`() {
        val s = BudgetCalculator.summarize(budget("1000.00"), listOf(expense("600.00")), today, zone)
        assertEquals(AffordVerdict.YES, AffordabilityChecker.check(s, BigDecimal("100")).verdict)
        assertEquals(AffordVerdict.TIGHT, AffordabilityChecker.check(s, BigDecimal("250")).verdict)
        assertEquals(AffordVerdict.TIGHT, AffordabilityChecker.check(s, BigDecimal("400")).verdict) // exactamente 100 %
        val no = AffordabilityChecker.check(s, BigDecimal("450.50"))
        assertEquals(AffordVerdict.NO, no.verdict)
        assertEquals(BigDecimal("-50.50"), no.availableAfter)
    }
}
