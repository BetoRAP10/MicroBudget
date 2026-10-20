package com.microbudget.wear.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.microbudget.domain.budget.AffordabilityChecker
import com.microbudget.domain.budget.AlertType
import com.microbudget.domain.model.Category
import com.microbudget.domain.money.Money
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.components.AlertCard
import com.microbudget.wear.ui.components.CategoryBadge
import com.microbudget.wear.ui.components.WearListScreen
import com.microbudget.wear.ui.theme.MbColors
import kotlinx.coroutines.delay

/** Paso 3: elegir categoría con botones grandes (2 por fila). Un toque avanza. */
@Composable
fun CategoryPickerScreen(vm: WearViewModel, cents: Long, onPick: (String) -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    WearListScreen {
        item { ListHeader { Text("${Money.formatShort(Money.fromCents(cents))} · Categoría", textAlign = TextAlign.Center) } }
        categories.chunked(2).forEach { row ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { c -> CategoryButton(c) { onPick(c.id) } }
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(category: Category, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color(category.colorArgb).copy(alpha = 0.30f)),
            contentAlignment = Alignment.Center,
        ) { Text(category.icon, fontSize = 26.sp) }
        Text(category.name, fontSize = 11.sp, color = MbColors.Text, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Paso 4: confirmar. Muestra cuánto quedaría disponible antes de guardar. */
@Composable
fun ConfirmScreen(vm: WearViewModel, cents: Long, categoryId: String, onCancel: () -> Unit, onSave: () -> Unit) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val category = vm.category(categoryId)
    val amount = Money.fromCents(cents)
    val after = summary?.let { AffordabilityChecker.check(it, amount) }

    Column(
        Modifier.fillMaxSize().background(MbColors.Background).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Confirmar gasto", fontSize = 13.sp, color = MbColors.TextSecondary)
        Text(Money.format(amount), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MbColors.Text, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category, 24)
            Spacer(Modifier.width(6.dp))
            Text(category.name, fontSize = 15.sp, color = MbColors.Text)
        }
        if (after != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (after.availableAfter.signum() >= 0) "Quedarían ${Money.formatShort(after.availableAfter)}"
                else "Excederías por ${Money.formatShort(after.availableAfter.negate())}",
                fontSize = 12.sp, color = MbColors.forVerdict(after.verdict),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = onCancel, colors = ButtonDefaults.secondaryButtonColors()) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar")
            }
            Button(
                onClick = onSave, enabled = !busy,
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = MbColors.Green),
            ) { Icon(Icons.Filled.Check, contentDescription = "Guardar", tint = Color.Black) }
        }
    }
}

/** Paso 5: confirmación visual animada (+ vibración, lanzada en el ViewModel). */
@Composable
fun SuccessScreen(vm: WearViewModel, onDone: () -> Unit) {
    val outcome by vm.lastSave.collectAsStateWithLifecycle()
    val scale = remember { Animatable(0.3f) }
    val alert = outcome?.alerts?.lastOrNull()

    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(alert) {
        delay(if (alert == null) 1_800 else 5_000)
        onDone()
    }

    Column(
        Modifier.fillMaxSize().background(MbColors.Background).clickable(onClick = onDone).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(56.dp).scale(scale.value).clip(CircleShape).background(MbColors.Green),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(6.dp))
        Text("¡Guardado!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MbColors.Text)
        outcome?.let {
            Text(
                "${Money.format(it.expense.amount)} · ${vm.category(it.expense.categoryId).name}",
                fontSize = 13.sp, color = MbColors.TextSecondary, maxLines = 1,
            )
        }
        if (alert != null) {
            Spacer(Modifier.height(6.dp))
            AlertCard(alert.title, alert.shortMessage, if (alert.type == AlertType.PERCENT_80) MbColors.Orange else MbColors.Red)
        }
    }
}
