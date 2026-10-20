package com.microbudget.domain.money

import java.math.BigDecimal

sealed interface AmountValidation {
    data class Valid(val amount: BigDecimal) : AmountValidation
    data class Invalid(val reason: String) : AmountValidation
}

/**
 * Valida importes introducidos por el usuario.
 * Reglas: número positivo, máximo 2 decimales, máximo [MAX], sin separadores de miles.
 */
object AmountValidator {
    val MAX: BigDecimal = BigDecimal("1000000.00")
    private val pattern = Regex("""^\d{1,12}(\.\d{0,2})?$""")

    fun validate(input: String?): AmountValidation {
        val text = input?.trim()?.replace(',', '.').orEmpty()
        if (text.isEmpty()) return AmountValidation.Invalid("Introduce una cantidad")
        if (text.startsWith("-")) return AmountValidation.Invalid("La cantidad debe ser positiva")
        if (!pattern.matches(text)) return AmountValidation.Invalid("Cantidad no válida (máx. 2 decimales)")
        return validate(BigDecimal(text))
    }

    fun validate(amount: BigDecimal): AmountValidation {
        if (amount.signum() <= 0) return AmountValidation.Invalid("La cantidad debe ser mayor que 0")
        if (amount.stripTrailingZeros().scale() > 2) return AmountValidation.Invalid("Máximo 2 decimales")
        if (amount > MAX) return AmountValidation.Invalid("La cantidad máxima es ${Money.format(MAX)}")
        return AmountValidation.Valid(Money.normalize(amount))
    }
}
