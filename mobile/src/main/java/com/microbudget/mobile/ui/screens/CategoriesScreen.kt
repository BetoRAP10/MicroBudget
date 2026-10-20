@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.components.CategoryIcon
import com.microbudget.mobile.ui.components.MbCard
import com.microbudget.mobile.ui.theme.MbColors

/** Administración de categorías: crear, editar (nombre, icono, color) y eliminar. */
@Composable
fun CategoriesScreen(vm: MobileViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Categorías") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
            actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, contentDescription = "Nueva categoría") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MbColors.Background),
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { c ->
                MbCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(c)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(if (c.isDefault) "Predeterminada" else "Personalizada", color = MbColors.TextSecondary, fontSize = 12.sp)
                        }
                        IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, contentDescription = "Editar ${c.name}") }
                        if (c.id != DefaultCategories.OTHER) {
                            IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${c.name}", tint = MbColors.Red) }
                        }
                    }
                }
            }
            item {
                Text("Al eliminar una categoría, sus gastos pasan a «Otros».", color = MbColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }

    if (creating || editing != null) {
        CategoryDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, icon, color ->
                vm.saveCategory(editing?.id, name, icon, color) { creating = false; editing = null }
            },
        )
    }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar «${c.name}»?") },
            text = { Text("Sus gastos se moverán a «Otros». Los totales no cambian.") },
            confirmButton = { TextButton(onClick = { vm.deleteCategory(c.id); deleting = null }) { Text("Eliminar", color = MbColors.Red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CategoryDialog(initial: Category?, onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: DefaultCategories.iconChoices.first()) }
    var color by remember { mutableStateOf(initial?.colorArgb ?: DefaultCategories.colorChoices.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Icono", color = MbColors.TextSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DefaultCategories.iconChoices.forEach { e ->
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (e == icon) MbColors.Purple else MbColors.SurfaceHigh)
                                .clickable { icon = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 18.sp) }
                    }
                }
                Text("Color", color = MbColors.TextSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultCategories.colorChoices.forEach { c ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                                .border(if (c == color) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = c },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, icon, color) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
