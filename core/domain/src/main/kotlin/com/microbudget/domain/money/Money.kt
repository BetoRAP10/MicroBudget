package com.microbudget.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utilidades monetarias.
 * En lógica se usa BigDecimal con escala 2; en la base de datos se guardan centavos (Long),
 * que es un entero exacto: 45.10 -> 4510. Nunca se usa Double para dinero.
 */
object Money {
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2)

    fun normalize(amount: BigDecimal): BigDecimal = amount.setScale(2, RoundingMode.HALF_UP)

    fun toCents(amount: BigDecimal): Long = normalize(amount).movePointRight(2).longValueExact()

    fun fromCents(cents: Long): BigDecimal = BigDecimal.valueOf(cents, 2)

    private val symbols = DecimalFormatSymbols(Locale.US)

    /** "$1,234.50" o "-$12.00". */
    fun format(amount: BigDecimal): String {
        val df = DecimalFormat("#,##0.00", symbols)
        val n = normalize(amount)
        return if (n.signum() < 0) "-$" + df.format(n.negate()) else "$" + df.format(n)
    }

    /** Formato corto para el reloj: "$1,234" si no tiene centavos, si no "$1,234.50". */
    fun formatShort(amount: BigDecimal): String {
        val n = normalize(amount)
        val noCents = n.remainder(BigDecimal.ONE).signum() == 0
        val df = DecimalFormat(if (noCents) "#,##0" else "#,##0.00", symbols)
        return if (n.signum() < 0) "-$" + df.format(n.negate()) else "$" + df.format(n)
    }

    /** Texto plano para serializar: "45.10". */
    fun toPlain(amount: BigDecimal): String = normalize(amount).toPlainString()
}
