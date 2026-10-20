package com.microbudget.domain.budget

import com.microbudget.domain.model.BudgetPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Rango de fechas [start, end] (ambas inclusive) de un periodo de presupuesto. */
data class PeriodRange(val start: LocalDate, val end: LocalDate) {
    val totalDays: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun startMillis(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Último milisegundo del periodo. */
    fun endMillis(zone: ZoneId): Long = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
}

/**
 * Calcula periodos: la semana empieza el lunes (ISO-8601) y el mes el día 1.
 */
object PeriodCalculator {
    private val es = Locale.forLanguageTag("es-MX")
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", es)
    private val shortMonthYear = DateTimeFormatter.ofPattern("MMM yyyy", es)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", es)

    fun rangeFor(period: BudgetPeriod, date: LocalDate): PeriodRange = when (period) {
        BudgetPeriod.WEEKLY -> {
            val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            PeriodRange(start, start.plusDays(6))
        }
        BudgetPeriod.MONTHLY -> PeriodRange(
            date.withDayOfMonth(1),
            date.with(TemporalAdjusters.lastDayOfMonth()),
        )
    }

    /** Clave única del periodo; se usa para recordar qué alertas ya se enviaron. */
    fun periodKey(period: BudgetPeriod, range: PeriodRange): String = "${period.name}:${range.start}"

    /** "Octubre 2026" o "Semana 42 (12 oct – 18 oct)". */
    fun label(period: BudgetPeriod, range: PeriodRange): String = when (period) {
        BudgetPeriod.MONTHLY -> monthYear.format(range.start).replaceFirstChar { it.titlecase(es) }
        BudgetPeriod.WEEKLY ->
            "Semana ${range.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} " +
                "(${dayMonth.format(range.start)} – ${dayMonth.format(range.end)})"
    }

    /** Etiqueta corta del periodo al que pertenece un gasto: "Oct 2026" o "Sem 42·2026". */
    fun shortLabelFor(period: BudgetPeriod, epochMillis: Long, zone: ZoneId): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val range = rangeFor(period, date)
        return when (period) {
            BudgetPeriod.MONTHLY -> shortMonthYear.format(range.start)
                .replace(".", "").replaceFirstChar { it.titlecase(es) }
            BudgetPeriod.WEEKLY ->
                "Sem ${range.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}·${range.start.get(IsoFields.WEEK_BASED_YEAR)}"
        }
    }

    fun displayName(period: BudgetPeriod): String = when (period) {
        BudgetPeriod.WEEKLY -> "Semanal"
        BudgetPeriod.MONTHLY -> "Mensual"
    }
}
