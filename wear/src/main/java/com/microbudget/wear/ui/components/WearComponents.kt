package com.microbudget.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.microbudget.domain.model.Category
import com.microbudget.wear.ui.theme.MbColors

/** Lista desplazable estándar de Wear OS con hora, indicador de posición y viñeta. */
@Composable
fun WearListScreen(
    showTime: Boolean = true,
    content: ScalingLazyListScope.() -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        modifier = Modifier.background(MbColors.Background),
        timeText = { if (showTime) TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** Logotipo: círculo morado con "$" + nombre. */
@Composable
fun MicroBudgetLogo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).background(MbColors.Purple),
            contentAlignment = Alignment.Center,
        ) { Text("$", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.width(5.dp))
        Text("MicroBudget", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MbColors.PurpleLight)
    }
}

/** Emoji de la categoría dentro de un círculo de su color. */
@Composable
fun CategoryBadge(category: Category, size: Int = 26) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(category.colorArgb).copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) { Text(category.icon, fontSize = (size * 0.55f).sp) }
}

/** Fila etiqueta / valor para las pantallas de resumen. */
@Composable
fun StatRow(label: String, value: String, valueColor: Color = MbColors.Text) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = MbColors.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
    }
}

/** Tarjeta de aviso (alertas de presupuesto). */
@Composable
fun AlertCard(title: String, message: String, color: Color) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.18f)).padding(10.dp),
    ) {
        Text(
            text = "⚠ $title\n$message",
            fontSize = 12.sp, color = MbColors.Text, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(), maxLines = 5, overflow = TextOverflow.Ellipsis,
        )
    }
}
