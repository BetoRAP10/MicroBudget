package com.microbudget.domain

import com.microbudget.domain.money.AmountInputBuffer
import com.microbudget.domain.money.AmountValidation
import com.microbudget.domain.money.AmountValidator
import com.microbudget.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Caso de prueba 10: los importes inválidos son rechazados. */
class AmountValidationTest {

    @Test fun `acepta importes positivos con hasta dos decimales`() {
        assertEquals(AmountValidation.Valid(BigDecimal("45.50")), AmountValidator.validate("45.5"))
        assertEquals(AmountValidation.Valid(BigDecimal("12.00")), AmountValidator.validate("12"))
        assertEquals(AmountValidation.Valid(BigDecimal("0.99")), AmountValidator.validate("0,99"))
    }

    @Test fun `rechaza cero, negativos, vacios, texto y demasiados decimales`() {
        listOf("", "   ", null, "0", "0.00", "-5", "abc", "12.345", "1,000.00", "1e3", "5..2").forEach {
            assertTrue("Debió rechazar '$it'", AmountValidator.validate(it) is AmountValidation.Invalid)
        }
    }

    @Test fun `rechaza importes mayores al maximo`() {
        assertTrue(AmountValidator.validate("1000000.01") is AmountValidation.Invalid)
        assertTrue(AmountValidator.validate("1000000") is AmountValidation.Valid)
    }

    @Test fun `centavos exactos sin errores de coma flotante`() {
        val sum = BigDecimal("0.10") + BigDecimal("0.20")
        assertEquals(30L, Money.toCents(sum))           // con Double daría 0.30000000000000004
        assertEquals(BigDecimal("45.10"), Money.fromCents(4510))
        assertEquals("$1,234.50", Money.format(BigDecimal("1234.5")))
        assertEquals("-$12.00", Money.format(BigDecimal("-12")))
    }

    @Test fun `teclado del reloj limita decimales y puntos`() {
        var b = AmountInputBuffer()
        "12.345".forEach { b = b.press(it) }
        assertEquals("12.34", b.text)
        b = AmountInputBuffer().press('.').press('.').press('5')
        assertEquals("0.5", b.text)
        b = AmountInputBuffer().press('0').press('7')
        assertEquals("7", b.text)
        assertEquals("1", AmountInputBuffer("12").backspace().text)
        assertTrue(AmountInputBuffer("").validate() is AmountValidation.Invalid)
        assertEquals(AmountValidation.Valid(BigDecimal("8.00")), AmountInputBuffer("8.").validate())
    }
}
