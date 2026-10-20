package com.microbudget.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.microbudget.domain.money.AmountInputBuffer
import com.microbudget.domain.money.AmountValidation
import com.microbudget.domain.money.Money
import com.microbudget.wear.ui.theme.MbColors

/**
 * Teclado numérico grande adaptado a la pantalla redonda. Las proporciones se calculan
 * a partir del diámetro para que todas las teclas queden dentro del círculo visible.
 */
@Composable
fun KeypadScreen(
    title: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (cents: Long) -> Unit,
) {
    var buffer by remember { mutableStateOf(AmountInputBuffer(initial)) }
    var error by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(Modifier.fillMaxSize().background(MbColors.Background)) {
        val d = minOf(maxWidth, maxHeight)
        val keyW = d * 0.19f
        val keyH = d * 0.112f
        val gap = d * 0.012f
        val density = LocalDensity.current
        fun Dp.sp() = with(density) { this@sp.toSp() }

        fun press(k: Char) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            error = null
            buffer = when (k) {
                '<' -> buffer.backspace()
                else -> buffer.press(k)
            }
        }

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(d * 0.075f))
            Text(title, fontSize = (d * 0.055f).sp(), color = MbColors.TextSecondary)
            Text(
                text = error ?: buffer.display,
                fontSize = if (error != null) (d * 0.05f).sp() else (d * 0.085f).sp(),
                fontWeight = FontWeight.Bold,
                color = if (error != null) MbColors.Red else MbColors.Text,
                maxLines = 1,
                modifier = Modifier.height(d * 0.11f),
            )
            val rows = listOf("123", "456", "789", ".0<")
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { k ->
                        val label = if (k == '<') "⌫" else k.toString()
                        Box(
                            modifier = Modifier
                                .size(keyW, keyH)
                                .clip(RoundedCornerShape(50))
                                .background(if (k == '<') MbColors.SurfaceHigh else MbColors.Surface)
                                .clickable { press(k) }
                                .semantics { contentDescription = if (k == '<') "Borrar" else label },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = (keyH * 0.55f).sp(), fontWeight = FontWeight.SemiBold, color = MbColors.Text)
                        }
                    }
                }
                Spacer(Modifier.height(gap))
            }
            Spacer(Modifier.height(d * 0.01f))
            Button(
                onClick = {
                    when (val v = buffer.validate()) {
                        is AmountValidation.Valid -> onConfirm(Money.toCents(v.amount))
                        is AmountValidation.Invalid -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            error = v.reason
                        }
                    }
                },
                modifier = Modifier.size(d * 0.15f).semantics { contentDescription = confirmLabel },
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = MbColors.Purple),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
            }
        }
    }
}
