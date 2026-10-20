package com.microbudget.wear

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.wear.ui.WearNavHost
import com.microbudget.wear.ui.theme.MicroBudgetWearTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* el estado se consulta al mostrar Ajustes */ }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("wear_ui", MODE_PRIVATE)
        if (!prefs.getBoolean("asked_notifications", false)) {
            prefs.edit().putBoolean("asked_notifications", true).apply()
            requestNotifications()
        }
        setContent {
            MicroBudgetWearTheme {
                val vm: WearViewModel = viewModel { WearViewModel(MicroBudgetGraph.get()) }
                WearNavHost(vm, onRequestNotifications = ::requestNotifications)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MicroBudgetGraph.get().onAppForeground()
    }
}
