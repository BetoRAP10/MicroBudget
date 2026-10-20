package com.microbudget.mobile

import android.Manifest
import android.graphics.Color
import androidx.activity.SystemBarStyle
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.mobile.ui.MobileNavHost
import com.microbudget.mobile.ui.theme.MicroBudgetTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema siempre oscuro: íconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("mobile_ui", MODE_PRIVATE)
        if (!prefs.getBoolean("asked_notifications", false)) {
            prefs.edit().putBoolean("asked_notifications", true).apply()
            requestNotifications()
        }
        setContent {
            MicroBudgetTheme {
                val vm: MobileViewModel = viewModel { MobileViewModel(MicroBudgetGraph.get()) }
                MobileNavHost(vm, onRequestNotifications = ::requestNotifications)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MicroBudgetGraph.get().onAppForeground()
    }
}
