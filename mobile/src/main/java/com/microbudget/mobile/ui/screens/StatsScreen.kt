package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.domain.money.Money
import com.microbudget.domain.stats.StatsCalculator
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.components.CategoryIcon
import com.microbudget.mobile.ui.components.DailyBarChart
import com.microbudget.mobile.ui.components.DonutChart
import com.microbudget.mobile.ui.components.DonutSlice
import com.microbudget.mobile.ui.components.MbCard
import com.microbudget.mobile.ui.components.SectionTitle
import com.microbudget.mobile.ui.components.Stat
import com.microbudget.mobile.ui.theme.MbColors
import java.time.LocalDate

/** Gráficas del periodo actual. Se recalculan solas al crear, editar o eliminar gastos. */
@Composable
fun StatsScreen(vm: MobileViewModel) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val periodExpenses by vm.periodExpenses.collectAsStateWithLifecycle()
    val totals = StatsCalculator.byCategory(periodExpenses)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Gráficas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(summary?.periodLabel.orEmpty(), color = MbColors.PurpleLight)
        }
        item {
            MbCard {
                SectionTitle("Por categoría")
                if (totals.isEmpty()) {
                    Text("Sin gastos en este periodo.", color = MbColors.TextSecondary)
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(
                            totals.map { t -> vm.category(t.categoryId).let { DonutSlice(it.name, t.total.toFloat(), Color(it.colorArgb)) } },
                            summary?.spent ?: Money.ZERO,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    totals.forEach { t ->
                        val c = vm.category(t.categoryId)
                        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcon(c, 34)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(c.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                    Text(Money.format(t.total), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MbColors.SurfaceHigh)) {
                                    Box(Modifier.fillMaxWidth(t.share.toFloat()).fillMaxHeight().background(Color(c.colorArgb)))
                                }
                                Text("${(t.share * 100).toInt()} % · ${t.count} ${if (t.count == 1) "gasto" else "gastos"}", color = MbColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        summary?.let { s ->
            item {
                MbCard {
                    SectionTitle("Gasto por día")
                    DailyBarChart(StatsCalculator.byDay(periodExpenses, s.range, vm.zone), LocalDate.now(vm.zone))
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Stat("Promedio diario", Money.format(s.dailyAverage), Modifier.weight(1f))
                        Stat("Gastos", s.expenseCount.toString(), Modifier.weight(1f))
                        Stat("Consumido", "${s.percentUsed} %", Modifier.weight(1f), MbColors.forStatus(s.status))
                    }
                }
            }
        }
    }
}
