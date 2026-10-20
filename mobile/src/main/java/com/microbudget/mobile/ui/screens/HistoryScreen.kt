@file:OptIn(ExperimentalMaterial3Api::class)

package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.domain.format.DateFormats
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.money.Money
import com.microbudget.mobile.DatePreset
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.components.ExpenseRow
import com.microbudget.mobile.ui.theme.MbColors
import java.time.Instant
import java.time.ZoneOffset

/** Historial completo con filtros por categoría y fecha. Tocar un gasto abre su edición. */
@Composable
fun HistoryScreen(vm: MobileViewModel, onOpenExpense: (String) -> Unit) {
    val filtered by vm.filtered.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()
    var showRange by remember { mutableStateOf(false) }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbColors.Purple, selectedLabelColor = MbColors.Text)

    Column(Modifier.fillMaxSize()) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePreset.entries.forEach { p ->
                FilterChip(
                    selected = filter.preset == p,
                    onClick = { if (p == DatePreset.CUSTOM) showRange = true else vm.setPreset(p) },
                    label = {
                        Text(
                            if (p == DatePreset.CUSTOM && filter.preset == DatePreset.CUSTOM && filter.customFrom != null)
                                "${DateFormats.shortDay(filter.customFrom!!)} – ${filter.customTo?.let { DateFormats.shortDay(it) } ?: "…"}"
                            else p.label,
                        )
                    },
                    colors = chipColors,
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter.categoryIds.isEmpty(), onClick = { vm.clearCategoryFilter() }, label = { Text("Todas") }, colors = chipColors)
            categories.forEach { c ->
                FilterChip(
                    selected = c.id in filter.categoryIds,
                    onClick = { vm.toggleCategoryFilter(c.id) },
                    label = { Text("${c.icon} ${c.name}") },
                    colors = chipColors,
                )
            }
        }
        val total = filtered.fold(Money.ZERO) { acc, e -> acc + e.amount }
        Text(
            "${filtered.size} ${if (filtered.size == 1) "gasto" else "gastos"} · ${Money.format(total)}",
            color = MbColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp)) {
            val byDay = filtered.groupBy { DateFormats.localDate(it.dateTime, vm.zone) }
            byDay.forEach { (day, list) ->
                item(key = day.toString()) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp)) {
                        Text(DateFormats.longDay(day), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = MbColors.PurpleLight)
                        Text(Money.format(list.fold(Money.ZERO) { a, e -> a + e.amount }), color = MbColors.TextSecondary, fontSize = 13.sp)
                    }
                }
                items(list, key = { it.id }) { e ->
                    ExpenseRow(e, vm.category(e.categoryId), vm.zone, budget?.period ?: BudgetPeriod.MONTHLY) { onOpenExpense(e.id) }
                }
            }
            if (filtered.isEmpty()) {
                item { Text("No hay gastos con estos filtros.", color = MbColors.TextSecondary, modifier = Modifier.padding(top = 24.dp)) }
            }
        }
    }

    if (showRange) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val from = state.selectedStartDateMillis
                        if (from != null) {
                            val to = state.selectedEndDateMillis ?: from
                            // El selector devuelve medianoche UTC del día elegido.
                            vm.setCustomRange(
                                Instant.ofEpochMilli(from).atZone(ZoneOffset.UTC).toLocalDate(),
                                Instant.ofEpochMilli(to).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
                        }
                        showRange = false
                    },
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showRange = false }) { Text("Cancelar") } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f), title = { Text("Rango de fechas", Modifier.padding(16.dp)) })
        }
    }
}
