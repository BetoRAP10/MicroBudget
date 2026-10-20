package com.microbudget.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microbudget.data.sync.ConnectionState
import com.microbudget.data.sync.SyncUiState
import com.microbudget.domain.budget.BudgetSummary
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.format.DateFormats
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.Money
import com.microbudget.mobile.ui.theme.MbColors
import java.time.ZoneId

/** Tarjeta con esquinas redondeadas de la app. */
@Composable
fun MbCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.fillMaxWidth()
    val colors = CardDefaults.cardColors(containerColor = MbColors.Surface, contentColor = MbColors.Text)
    val shape = RoundedCornerShape(20.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = m, shape = shape, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
    } else {
        Card(modifier = m, shape = shape, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun AppLogo(size: Int = 32) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(MbColors.Purple), contentAlignment = Alignment.Center) {
        Text("$", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.55f).sp)
    }
}

@Composable
fun CategoryIcon(category: Category, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(category.colorArgb).copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) { Text(category.icon, fontSize = (size * 0.5f).sp) }
}

/** Resumen del presupuesto: disponible / exceso, barra de progreso y estadísticas. */
@Composable
fun SummaryCard(s: BudgetSummary) {
    val color = MbColors.forStatus(s.status)
    MbCard {
        Text(s.periodLabel, color = MbColors.PurpleLight, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text(if (s.isExceeded) "Excedido por" else "Disponible", color = MbColors.TextSecondary)
        Text(
            Money.format(if (s.isExceeded) s.overspent else s.available),
            color = color, fontSize = 38.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { s.ratio.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = color, trackColor = MbColors.SurfaceHigh, strokeCap = StrokeCap.Round, gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Presupuesto", Money.format(s.budgetAmount), Modifier.weight(1f))
            Stat("Gastado", Money.format(s.spent), Modifier.weight(1f))
            Stat("Consumido", "${s.percentUsed} %", Modifier.weight(0.8f), color)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Promedio diario", Money.format(s.dailyAverage), Modifier.weight(1f))
            Stat("Puedes gastar/día", Money.format(s.dailyAllowance), Modifier.weight(1f))
            Stat("Días restantes", s.daysRemaining.toString(), Modifier.weight(0.8f))
        }
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MbColors.Text) {
    Column(modifier) {
        Text(label, color = MbColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

/** Indicador compacto del estado de sincronización. */
@Composable
fun SyncPill(sync: SyncUiState, onClick: () -> Unit) {
    val (icon, text, color) = when {
        sync.pendingCount > 0 -> Triple(Icons.Filled.SyncProblem, "${sync.pendingCount} sin sincronizar", MbColors.Orange)
        sync.connection == ConnectionState.CONNECTED || sync.lastSyncAt != null ->
            Triple(Icons.Filled.Sync, "Sincronizado", MbColors.Green)
        else -> Triple(Icons.Filled.CloudOff, "Solo local", MbColors.TextSecondary)
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ExpenseRow(e: Expense, category: Category, zone: ZoneId, period: BudgetPeriod, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.description ?: category.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (e.createdFrom == DeviceSource.WATCH) Icons.Filled.Watch else Icons.Filled.PhoneAndroid,
                    contentDescription = if (e.createdFrom == DeviceSource.WATCH) "Desde el reloj" else "Desde el celular",
                    tint = MbColors.TextSecondary, modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${category.name} · ${DateFormats.dateTime(e.dateTime, zone)} · ${PeriodCalculator.shortLabelFor(period, e.dateTime, zone)}",
                    color = MbColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.format(e.amount), fontWeight = FontWeight.SemiBold)
            if (e.syncStatus == SyncStatus.PENDING) {
                Text("pendiente", color = MbColors.Orange, fontSize = 11.sp)
            }
        }
    }
}

/** Aviso con fondo de color (alertas y advertencias). */
@Composable
fun Banner(text: String, color: Color, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.16f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = MbColors.Text, modifier = Modifier.weight(1f), fontSize = 14.sp)
        action?.invoke()
    }
}
