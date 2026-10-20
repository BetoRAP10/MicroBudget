package com.microbudget.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.microbudget.domain.budget.AffordVerdict
import com.microbudget.domain.budget.BudgetStatus

/** Paleta MicroBudget: morado principal, fondo oscuro, verde/naranja/rojo para estados. */
object MbColors {
    val Purple = Color(0xFF8B5CF6)
    val PurpleDark = Color(0xFF6D28D9)
    val PurpleLight = Color(0xFFC4B5FD)
    val Background = Color(0xFF0E0A1A)
    val Surface = Color(0xFF1E1832)
    val SurfaceHigh = Color(0xFF2C2446)
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB9B4C9)
    val Green = Color(0xFF22C55E)
    val Orange = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)

    fun forStatus(status: BudgetStatus?): Color = when (status) {
        BudgetStatus.OK, null -> Green
        BudgetStatus.WARNING -> Orange
        BudgetStatus.EXCEEDED -> Red
    }

    fun forVerdict(v: AffordVerdict): Color = when (v) {
        AffordVerdict.YES -> Green
        AffordVerdict.TIGHT -> Orange
        AffordVerdict.NO -> Red
    }
}

private val wearColors = Colors(
    primary = MbColors.Purple,
    primaryVariant = MbColors.PurpleDark,
    secondary = MbColors.PurpleLight,
    secondaryVariant = MbColors.PurpleDark,
    background = MbColors.Background,
    surface = MbColors.Surface,
    error = MbColors.Red,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = MbColors.Text,
    onSurface = MbColors.Text,
    onSurfaceVariant = MbColors.TextSecondary,
    onError = Color.Black,
)

@Composable
fun MicroBudgetWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = wearColors, content = content)
}
