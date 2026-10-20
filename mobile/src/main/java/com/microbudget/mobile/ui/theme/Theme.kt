package com.microbudget.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.microbudget.domain.budget.AffordVerdict
import com.microbudget.domain.budget.BudgetStatus

object MbColors {
    val Purple = Color(0xFF8B5CF6)
    val PurpleDark = Color(0xFF6D28D9)
    val PurpleLight = Color(0xFFC4B5FD)
    val Background = Color(0xFF0E0A1A)
    val Surface = Color(0xFF1A1529)
    val SurfaceHigh = Color(0xFF261F3B)
    val Outline = Color(0xFF3B3256)
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

private val scheme = darkColorScheme(
    primary = MbColors.Purple,
    onPrimary = Color.White,
    primaryContainer = MbColors.PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = MbColors.PurpleLight,
    onSecondary = Color.Black,
    secondaryContainer = MbColors.SurfaceHigh,
    onSecondaryContainer = MbColors.PurpleLight,
    background = MbColors.Background,
    onBackground = MbColors.Text,
    surface = MbColors.Background,
    onSurface = MbColors.Text,
    surfaceVariant = MbColors.SurfaceHigh,
    onSurfaceVariant = MbColors.TextSecondary,
    surfaceContainerLowest = MbColors.Background,
    surfaceContainerLow = MbColors.Surface,
    surfaceContainer = MbColors.Surface,
    surfaceContainerHigh = MbColors.SurfaceHigh,
    surfaceContainerHighest = MbColors.SurfaceHigh,
    outline = MbColors.Outline,
    outlineVariant = MbColors.Outline,
    error = MbColors.Red,
    onError = Color.Black,
)

@Composable
fun MicroBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
