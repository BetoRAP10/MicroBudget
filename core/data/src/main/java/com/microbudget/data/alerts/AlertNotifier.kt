package com.microbudget.data.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.microbudget.data.R
import com.microbudget.domain.budget.AlertType
import com.microbudget.domain.budget.BudgetAlert

/** Notificaciones locales del dispositivo (no se reenvían al otro: setLocalOnly). */
class AlertNotifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Alertas de presupuesto", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos al alcanzar el 80 %, el 100 % o tu límite personalizado"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(): Boolean {
        val runtimeOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Devuelve false si no se pudo mostrar (sin permiso); la app muestra el aviso dentro de la pantalla. */
    @SuppressLint("MissingPermission")
    fun notify(alert: BudgetAlert): Boolean {
        if (!hasPermission()) return false
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_microbudget)
            .setContentTitle("MicroBudget · ${alert.title}")
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(if (alert.type == AlertType.PERCENT_80) 0xFFF59E0B.toInt() else 0xFFEF4444.toInt())
            .setLocalOnly(true)
            .setAutoCancel(true)
            .apply { if (pending != null) setContentIntent(pending) }
            .build()
        NotificationManagerCompat.from(context).notify(alert.key.hashCode(), notification)
        return true
    }

    fun notifyTest(): Boolean = notify(
        BudgetAlert("TEST", AlertType.PERCENT_80, "Notificación de prueba", "Las alertas de MicroBudget funcionan en este dispositivo."),
    )

    companion object {
        const val CHANNEL_ID = "budget_alerts"
    }
}
