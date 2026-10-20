package com.microbudget.domain

import com.microbudget.domain.TestData.budget
import com.microbudget.domain.TestData.expense
import com.microbudget.domain.TestData.today
import com.microbudget.domain.TestData.zone
import com.microbudget.domain.budget.AlertEngine
import com.microbudget.domain.budget.AlertType
import com.microbudget.domain.budget.BudgetCalculator
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Casos 4 y 5: las alertas aparecen al alcanzar umbrales y no se duplican. */
class AlertEngineTest {

    /** Simula lo que hace AlertService: recuerda claves notificadas por periodo. */
    private class Simulator(var budget: Budget) {
        val notified = mutableMapOf<String, MutableSet<String>>()
        val expenses = mutableListOf<Expense>()

        fun add(e: Expense, date: LocalDate = today): List<AlertType> {
            expenses += e
            return evaluate(date)
        }

        fun evaluate(date: LocalDate = today): List<AlertType> {
            val s = BudgetCalculator.summarize(budget, expenses, date, zone)
            val key = PeriodCalculator.periodKey(budget.period, s.range)
            val set = notified.getOrPut(key) { mutableSetOf() }
            val ev = AlertEngine.evaluate(budget, s, set)
            set += ev.toMarkNotified
            set -= ev.toRearm
            return ev.toNotify.map { it.type }
        }
    }

    @Test fun `avisa al 80 y al 100 una sola vez`() {
        val sim = Simulator(budget("1000.00"))
        assertEquals(emptyList<AlertType>(), sim.add(expense("700.00")))
        assertEquals(listOf(AlertType.PERCENT_80), sim.add(expense("100.00")))     // 80 % exacto
        assertEquals(emptyList<AlertType>(), sim.add(expense("50.00")))           // 85 %: no repite
        assertEquals(emptyList<AlertType>(), sim.add(expense("10.00")))
        assertEquals(listOf(AlertType.PERCENT_100), sim.add(expense("140.00")))   // 100 %
        assertEquals(emptyList<AlertType>(), sim.add(expense("5.00")))            // excedido: no repite
    }

    @Test fun `si un gasto cruza 80 y 100 a la vez solo muestra la mas grave`() {
        val sim = Simulator(budget("100.00"))
        assertEquals(listOf(AlertType.PERCENT_100), sim.add(expense("120.00")))
        assertEquals(emptyList<AlertType>(), sim.add(expense("1.00")))
    }

    @Test fun `limite personalizado configurable`() {
        val sim = Simulator(budget("1000.00", custom = "300.00"))
        assertEquals(emptyList<AlertType>(), sim.add(expense("299.99")))
        assertEquals(listOf(AlertType.CUSTOM), sim.add(expense("0.01")))
        assertEquals(emptyList<AlertType>(), sim.add(expense("10.00")))
    }

    @Test fun `alertas desactivadas no se envian`() {
        val sim = Simulator(budget("100.00").copy(alertAt80 = false, alertAt100 = false))
        assertEquals(emptyList<AlertType>(), sim.add(expense("150.00")))
    }

    @Test fun `se rearma si se elimina el gasto y se vuelve a cruzar`() {
        val sim = Simulator(budget("100.00"))
        val big = expense("85.00")
        assertEquals(listOf(AlertType.PERCENT_80), sim.add(big))
        sim.expenses.remove(big)
        assertEquals(emptyList<AlertType>(), sim.evaluate())                      // baja a 0 %: se rearma
        assertEquals(listOf(AlertType.PERCENT_80), sim.add(expense("81.00")))
    }

    @Test fun `se restablece al comenzar el siguiente periodo`() {
        val sim = Simulator(budget("100.00", BudgetPeriod.WEEKLY))
        assertEquals(listOf(AlertType.PERCENT_80), sim.add(expense("80.00")))
        val nextMonday = LocalDate.of(2026, 10, 19)
        sim.budget = sim.budget.copy(startDate = nextMonday, endDate = nextMonday.plusDays(6))
        assertEquals(emptyList<AlertType>(), sim.evaluate(nextMonday))             // semana nueva: 0 %
        val types = sim.add(expense("90.00", date = nextMonday), nextMonday)
        assertEquals(listOf(AlertType.PERCENT_80), types)
        assertTrue(sim.notified.keys.size == 2)
    }
}
