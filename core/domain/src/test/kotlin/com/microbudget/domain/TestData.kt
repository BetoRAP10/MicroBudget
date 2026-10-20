package com.microbudget.domain

import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

object TestData {
    val zone: ZoneId = ZoneId.of("America/Mexico_City")
    val today: LocalDate = LocalDate.of(2026, 10, 18)

    fun millis(date: LocalDate, hour: Int = 12): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    fun budget(amount: String = "2000.00", period: BudgetPeriod = BudgetPeriod.MONTHLY, custom: String? = null): Budget {
        val range = PeriodCalculator.rangeFor(period, today)
        return Budget(
            amount = BigDecimal(amount), period = period,
            startDate = range.start, endDate = range.end,
            customAlertAmount = custom?.let { BigDecimal(it) },
        )
    }

    fun expense(
        amount: String,
        category: String = "food",
        date: LocalDate = today,
        id: String = UUID.randomUUID().toString(),
        updatedAt: Long = 1_000L,
        source: DeviceSource = DeviceSource.WATCH,
    ) = Expense(id, BigDecimal(amount), category, millis(date), null, source, updatedAt, SyncStatus.PENDING)
}
