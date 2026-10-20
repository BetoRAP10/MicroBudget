package com.microbudget.domain.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de fecha en español usados por ambas apps. */
object DateFormats {
    private val es = Locale.forLanguageTag("es-MX")
    private val dayTime = DateTimeFormatter.ofPattern("d MMM, HH:mm", es)
    private val day = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
    private val shortDay = DateTimeFormatter.ofPattern("d MMM yyyy", es)
    private val time = DateTimeFormatter.ofPattern("HH:mm", es)

    fun dateTime(millis: Long, zone: ZoneId): String = dayTime.format(Instant.ofEpochMilli(millis).atZone(zone)).replace(".", "")

    fun time(millis: Long, zone: ZoneId): String = time.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun longDay(date: LocalDate): String = day.format(date).replaceFirstChar { it.titlecase(es) }

    fun shortDay(date: LocalDate): String = shortDay.format(date).replace(".", "")

    fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
