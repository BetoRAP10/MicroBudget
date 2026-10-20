package com.microbudget.data.demo

import androidx.room.withTransaction
import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.toEntity
import com.microbudget.data.repository.BudgetRepository
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Datos de prueba (dinero ficticio) para demostrar el proyecto en clase.
 * Escenario: presupuesto mensual de $2,000 con $1,450 gastados (72.5 %).
 * -> Registrar $150 dispara la alerta del 80 %; registrar después $450 dispara la del 100 %.
 */
class DemoDataSeeder(
    private val db: MicroBudgetDatabase,
    private val clock: Clock,
    private val source: DeviceSource,
    private val afterChange: suspend () -> Unit,
) {
    private data class Sample(val category: String, val amount: String, val description: String, val daysAgo: Long, val hour: Int)

    private val currentPeriod = listOf(
        Sample(DefaultCategories.FOOD, "185.50", "Tacos con amigos", 0, 9),
        Sample(DefaultCategories.TRANSPORT, "320.00", "Recarga tarjeta de transporte", 1, 8),
        Sample(DefaultCategories.ENTERTAINMENT, "249.00", "Cine", 2, 19),
        Sample(DefaultCategories.SHOPPING, "399.00", "Tenis en oferta", 4, 17),
        Sample(DefaultCategories.SERVICES, "180.00", "Plan de celular", 6, 10),
        Sample(DefaultCategories.FOOD, "64.50", "Café y pan", 9, 8),
        Sample(DefaultCategories.OTHER, "52.00", "Papelería", 12, 13),
    )
    private val previousPeriod = listOf(
        Sample(DefaultCategories.FOOD, "210.00", "Súper (mes anterior)", 0, 12),
        Sample(DefaultCategories.TRANSPORT, "95.00", "Taxi (mes anterior)", 3, 22),
    )

    suspend fun load(): String {
        val today = LocalDate.now(clock)
        val zone = clock.zone
        val range = PeriodCalculator.rangeFor(BudgetPeriod.MONTHLY, today)
        val now = clock.millis()

        fun build(s: Sample, base: LocalDate, min: LocalDate): Expense {
            val date = base.minusDays(s.daysAgo).let { if (it.isBefore(min)) min else it }
            var millis = date.atTime(LocalTime.of(s.hour, 15)).atZone(zone).toInstant().toEpochMilli()
            if (millis > now) millis = now - s.daysAgo * 60_000 // nunca en el futuro
            return Expense(
                UUID.randomUUID().toString(), BigDecimal(s.amount), s.category, millis, s.description,
                source, now, SyncStatus.PENDING,
            )
        }

        val prevEnd = range.start.minusDays(1)
        val expenses = currentPeriod.map { build(it, today, range.start) } +
            previousPeriod.map { build(it, prevEnd, prevEnd.withDayOfMonth(1)) }

        db.withTransaction {
            val current = db.budgetDao().get("main")
            val budget = BudgetRepository.defaultBudget(today).copy(
                amount = BigDecimal("2000.00"),
                updatedAt = maxOf(now, (current?.updatedAt ?: 0) + 1),
                syncStatus = SyncStatus.PENDING,
            )
            db.budgetDao().upsert(budget.toEntity())
            db.expenseDao().upsertAll(expenses.map { it.toEntity() })
            db.alertDao().deleteAll()
        }
        afterChange()
        return "Cargados ${expenses.size} gastos de prueba y presupuesto mensual de $2,000"
    }

    /** Borra todo en ESTE dispositivo (no se propaga) y vuelve a crear categorías y presupuesto iniciales. */
    suspend fun clearAll(): String {
        db.withTransaction {
            db.expenseDao().deleteAll()
            db.categoryDao().deleteAll()
            db.budgetDao().deleteAll()
            db.alertDao().deleteAll()
            db.categoryDao().insertIfMissing(DefaultCategories.all.map { it.toEntity() })
            db.budgetDao().insertIfMissing(BudgetRepository.defaultBudget(LocalDate.now(clock)).toEntity())
        }
        afterChange()
        return "Datos borrados en este dispositivo"
    }
}
