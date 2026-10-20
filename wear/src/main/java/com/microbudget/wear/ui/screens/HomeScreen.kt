package com.microbudget.wear.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.microbudget.domain.budget.BudgetStatus
import com.microbudget.domain.money.Money
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.components.MicroBudgetLogo
import com.microbudget.wear.ui.theme.MbColors

/**
 * Pantalla principal: anillo de progreso del presupuesto en el borde, dinero disponible,
 * total gastado y porcentaje, botón grande «Nuevo gasto» y accesos rápidos.
 */
@Composable
fun HomeScreen(
    vm: WearViewModel,
    onNewExpense: () -> Unit,
    onHistory: () -> Unit,
    onAfford: () -> Unit,
    onBudget: () -> Unit,
    onSettings: () -> Unit,
) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val color = MbColors.forStatus(summary?.status)
    val progress by animateFloatAsState(
        targetValue = (summary?.ratio ?: 0.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(700), label = "ring",
    )
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)

    Scaffold(
        modifier = Modifier.background(MbColors.Background),
        timeText = { TimeText() },
    ) {
        Box(Modifier.fillMaxSize()) {
            // Indicador visual del presupuesto consumido (hueco arriba para la hora).
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize().padding(3.dp),
                startAngle = 300f,
                endAngle = 240f,
                indicatorColor = color,
                trackColor = MbColors.SurfaceHigh,
                strokeWidth = 6.dp,
            )
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item { MicroBudgetLogo() }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val s = summary
                        Text(
                            if (s?.isExceeded == true) "Excedido" else "Disponible",
                            fontSize = 12.sp, color = MbColors.TextSecondary,
                        )
                        Text(
                            text = s?.let { Money.formatShort(if (it.isExceeded) it.overspent else it.available) } ?: "—",
                            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
                        )
                    }
                }
                item {
                    val s = summary
                    Text(
                        text = if (s == null) "" else "Gastado ${Money.formatShort(s.spent)} · ${s.percentUsed} %",
                        fontSize = 13.sp, color = MbColors.Text,
                    )
                }
                item {
                    Chip(
                        onClick = onNewExpense,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        label = { Text("Nuevo gasto", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Purple),
                    )
                }
                item {
                    Chip(
                        onClick = onHistory, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Historial") },
                        icon = { Text("🕘", fontSize = 16.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
                item {
                    Chip(
                        onClick = onAfford, modifier = Modifier.fillMaxWidth(),
                        label = { Text("¿Me alcanza?") },
                        icon = { Text("🤔", fontSize = 16.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
                item {
                    Chip(
                        onClick = onBudget, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Presupuesto") },
                        secondaryLabel = { Text(summary?.let { "Promedio ${Money.formatShort(it.dailyAverage)}/día" } ?: "") },
                        icon = { Text("📊", fontSize = 16.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
                item {
                    Chip(
                        onClick = onSettings, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ajustes y sync") },
                        secondaryLabel = {
                            Text(
                                if (sync.pendingCount == 0) "Todo sincronizado" else "${sync.pendingCount} sin sincronizar",
                                color = if (sync.pendingCount == 0) MbColors.Green else MbColors.Orange,
                            )
                        },
                        icon = { Text("⚙", fontSize = 16.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
                if (summary?.status == BudgetStatus.EXCEEDED) {
                    item {
                        Text(
                            "Superaste tu presupuesto",
                            fontSize = 12.sp, color = MbColors.Red, modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
