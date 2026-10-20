package com.microbudget.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.microbudget.domain.money.Money
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.screens.AffordResultScreen
import com.microbudget.wear.ui.screens.BudgetScreen
import com.microbudget.wear.ui.screens.CategoryPickerScreen
import com.microbudget.wear.ui.screens.ConfirmScreen
import com.microbudget.wear.ui.screens.ExpenseDetailScreen
import com.microbudget.wear.ui.screens.HistoryScreen
import com.microbudget.wear.ui.screens.HomeScreen
import com.microbudget.wear.ui.screens.KeypadScreen
import com.microbudget.wear.ui.screens.SettingsScreen
import com.microbudget.wear.ui.screens.SuccessScreen

/** Pocas pantallas; deslizar a la derecha vuelve atrás (gesto estándar de Wear OS). */
object Routes {
    const val HOME = "home"
    const val KEYPAD = "keypad/{mode}"
    const val CATEGORY = "category/{cents}"
    const val CONFIRM = "confirm/{cents}/{categoryId}"
    const val SUCCESS = "success"
    const val HISTORY = "history"
    const val DETAIL = "detail/{id}"
    const val BUDGET = "budget"
    const val AFFORD = "afford/{cents}"
    const val SETTINGS = "settings"

    fun keypad(mode: KeypadMode) = "keypad/${mode.name}"
    fun category(cents: Long) = "category/$cents"
    fun confirm(cents: Long, categoryId: String) = "confirm/$cents/$categoryId"
    fun detail(id: String) = "detail/$id"
    fun afford(cents: Long) = "afford/$cents"
}

enum class KeypadMode(val title: String, val confirm: String) {
    EXPENSE("Nuevo gasto", "Siguiente"),
    AFFORD("¿Me alcanza?", "Calcular"),
    BUDGET("Presupuesto", "Guardar"),
    CUSTOM_ALERT("Límite de alerta", "Guardar"),
}

@Composable
fun WearNavHost(vm: WearViewModel, onRequestNotifications: () -> Unit) {
    val nav = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onNewExpense = { nav.navigate(Routes.keypad(KeypadMode.EXPENSE)) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onAfford = { nav.navigate(Routes.keypad(KeypadMode.AFFORD)) },
                onBudget = { nav.navigate(Routes.BUDGET) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.KEYPAD, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
            val mode = KeypadMode.valueOf(entry.arguments?.getString("mode") ?: KeypadMode.EXPENSE.name)
            val budget by vm.budget.collectAsStateWithLifecycle()
            val initial = when (mode) {
                KeypadMode.BUDGET -> budget?.amount?.let { Money.toPlain(it).removeSuffix(".00") }.orEmpty()
                KeypadMode.CUSTOM_ALERT -> budget?.customAlertAmount?.let { Money.toPlain(it).removeSuffix(".00") }.orEmpty()
                else -> ""
            }
            KeypadScreen(title = mode.title, confirmLabel = mode.confirm, initial = initial) { cents ->
                when (mode) {
                    KeypadMode.EXPENSE -> nav.navigate(Routes.category(cents))
                    KeypadMode.AFFORD -> nav.navigate(Routes.afford(cents)) { popUpTo(Routes.HOME) }
                    KeypadMode.BUDGET -> { vm.setBudgetAmount(cents); nav.popBackStack() }
                    KeypadMode.CUSTOM_ALERT -> { vm.setAlerts(custom = cents); nav.popBackStack() }
                }
            }
        }
        composable(Routes.CATEGORY, arguments = listOf(navArgument("cents") { type = NavType.LongType })) { entry ->
            val cents = entry.arguments?.getLong("cents") ?: 0L
            CategoryPickerScreen(vm, cents) { categoryId -> nav.navigate(Routes.confirm(cents, categoryId)) }
        }
        composable(
            Routes.CONFIRM,
            arguments = listOf(
                navArgument("cents") { type = NavType.LongType },
                navArgument("categoryId") { type = NavType.StringType },
            ),
        ) { entry ->
            val cents = entry.arguments?.getLong("cents") ?: 0L
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            ConfirmScreen(
                vm = vm, cents = cents, categoryId = categoryId,
                onCancel = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onSave = {
                    vm.saveExpense(cents, categoryId) {
                        nav.navigate(Routes.SUCCESS) { popUpTo(Routes.HOME) }
                    }
                },
            )
        }
        composable(Routes.SUCCESS) {
            SuccessScreen(vm) { nav.popBackStack(Routes.HOME, inclusive = false) }
        }
        composable(Routes.HISTORY) {
            HistoryScreen(vm) { id -> nav.navigate(Routes.detail(id)) }
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            ExpenseDetailScreen(vm, entry.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
        }
        composable(Routes.BUDGET) {
            BudgetScreen(
                vm = vm,
                onEditAmount = { nav.navigate(Routes.keypad(KeypadMode.BUDGET)) },
                onEditCustomAlert = { nav.navigate(Routes.keypad(KeypadMode.CUSTOM_ALERT)) },
            )
        }
        composable(Routes.AFFORD, arguments = listOf(navArgument("cents") { type = NavType.LongType })) { entry ->
            val cents = entry.arguments?.getLong("cents") ?: 0L
            AffordResultScreen(
                vm = vm, cents = cents,
                onRegister = { nav.navigate(Routes.category(cents)) },
                onBack = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm, onRequestNotifications)
        }
    }
}
