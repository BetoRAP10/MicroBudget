package com.microbudget.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.screens.CategoriesScreen
import com.microbudget.mobile.ui.screens.DashboardScreen
import com.microbudget.mobile.ui.screens.ExpenseEditorScreen
import com.microbudget.mobile.ui.screens.HistoryScreen
import com.microbudget.mobile.ui.screens.SettingsScreen
import com.microbudget.mobile.ui.screens.StatsScreen
import com.microbudget.mobile.ui.theme.MbColors

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Filled.Home),
    Tab("history", "Historial", Icons.Filled.ReceiptLong),
    Tab("stats", "Gráficas", Icons.Filled.BarChart),
    Tab("settings", "Ajustes", Icons.Filled.Settings),
)

@Composable
fun MobileNavHost(vm: MobileViewModel, onRequestNotifications: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isTab = tabs.any { it.route == route }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    fun openEditor(id: String? = null) = nav.navigate(if (id == null) "edit" else "edit?id=$id")

    Scaffold(
        containerColor = MbColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTab) {
                NavigationBar(containerColor = MbColors.Surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MbColors.Text, indicatorColor = MbColors.Purple,
                                unselectedIconColor = MbColors.TextSecondary, unselectedTextColor = MbColors.TextSecondary,
                                selectedTextColor = MbColors.Text,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == "home" || route == "history") {
                ExtendedFloatingActionButton(
                    onClick = { openEditor() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo gasto") },
                    containerColor = MbColors.Purple, contentColor = MbColors.Text,
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                DashboardScreen(vm, onOpenExpense = { openEditor(it) }, onSeeAll = { nav.navigate("history") { launchSingleTop = true } })
            }
            composable("history") { HistoryScreen(vm, onOpenExpense = { openEditor(it) }) }
            composable("stats") { StatsScreen(vm) }
            composable("settings") {
                SettingsScreen(vm, onCategories = { nav.navigate("categories") }, onRequestNotifications = onRequestNotifications)
            }
            composable("categories") { CategoriesScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                "edit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                ExpenseEditorScreen(vm, entry.arguments?.getString("id"), onClose = { nav.popBackStack() })
            }
        }
    }
}
