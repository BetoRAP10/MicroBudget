package com.microbudget.domain.money

/**
 * Estado del teclado numérico del reloj. Inmutable para que sea fácil de probar.
 * Impide escribir más de 2 decimales, más de un punto o más de 7 dígitos enteros.
 */
data class AmountInputBuffer(val text: String = "") {

    fun press(key: Char): AmountInputBuffer = when {
        key == '.' -> when {
            text.contains('.') -> this
            text.isEmpty() -> copy(text = "0.")
            else -> copy(text = "$text.")
        }
        key.isDigit() -> {
            val dot = text.indexOf('.')
            when {
                dot >= 0 && text.length - dot - 1 >= 2 -> this          // ya hay 2 decimales
                dot < 0 && text.length >= 7 -> this                     // límite de enteros
                text == "0" -> copy(text = key.toString())              // evita "05"
                else -> copy(text = text + key)
            }
        }
        else -> this
    }

    fun backspace(): AmountInputBuffer = copy(text = text.dropLast(1))

    fun clear(): AmountInputBuffer = AmountInputBuffer()

    /** Texto mostrado en pantalla. */
    val display: String get() = if (text.isEmpty()) "$0" else "$$text"

    fun validate(): AmountValidation = AmountValidator.validate(text.removeSuffix("."))
}
