package com.microbudget.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microbudget.domain.money.Money
import com.microbudget.domain.stats.DayTotal
import com.microbudget.mobile.ui.theme.MbColors
import java.math.BigDecimal
import java.time.LocalDate

data class DonutSlice(val label: String, val value: Float, val color: Color)

/** Gráfica de dona: gasto por categoría. */
@Composable
fun DonutChart(slices: List<DonutSlice>, total: BigDecimal, sizeDp: Int = 170) {
    val progress by animateFloatAsState(1f, tween(800), label = "donut")
    val sum = slices.sumOf { it.value.toDouble() }.toFloat()
    val description = slices.joinToString { "${it.label} ${(it.value / sum * 100).toInt()} %" }
    Box(Modifier.size(sizeDp.dp).semantics { contentDescription = "Gasto por categoría: $description" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp.dp)) {
            val stroke = size.minDimension * 0.16f
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(MbColors.SurfaceHigh, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (sum <= 0f) return@Canvas
            var start = -90f
            slices.forEach { s ->
                val sweep = s.value / sum * 360f * progress
                drawArc(s.color, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", color = MbColors.TextSecondary, fontSize = 12.sp)
            Text(Money.format(total), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

/** Barras de gasto por día del periodo; el día de hoy se resalta. */
@Composable
fun DailyBarChart(days: List<DayTotal>, today: LocalDate, modifier: Modifier = Modifier) {
    val max = days.maxOfOrNull { it.total }?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = "Gasto por día del periodo" }) {
            if (days.isEmpty()) return@Canvas
            val slot = size.width / days.size
            val barW = (slot * 0.62f).coerceAtLeast(2f)
            days.forEachIndexed { i, d ->
                val h = (d.total.toFloat() / max.toFloat()) * size.height
                val color = when {
                    d.date == today -> MbColors.Purple
                    d.date.isAfter(today) -> MbColors.SurfaceHigh
                    else -> MbColors.PurpleLight.copy(alpha = 0.7f)
                }
                drawRoundRect(
                    color = if (h < 1f) MbColors.SurfaceHigh else color,
                    topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h.coerceAtLeast(3f)),
                    size = Size(barW, h.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(barW / 3, barW / 3),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(days.firstOrNull()?.date?.dayOfMonth?.toString().orEmpty(), color = MbColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Máx. ${Money.format(max)}", color = MbColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(days.lastOrNull()?.date?.dayOfMonth?.toString().orEmpty(), color = MbColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
