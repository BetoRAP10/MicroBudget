package com.microbudget.data.alerts

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibración de confirmación. En el emulador no se siente físicamente; por eso la app SIEMPRE
 * muestra además una confirmación visual animada.
 */
object Haptics {

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** @return true si el dispositivo declara tener vibrador. */
    fun success(context: Context): Boolean = vibrate(context, longArrayOf(0, 60))

    fun warning(context: Context): Boolean = vibrate(context, longArrayOf(0, 120, 80, 120))

    private fun vibrate(context: Context, pattern: LongArray): Boolean {
        val v = vibrator(context) ?: return false
        if (!v.hasVibrator()) return false
        return runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }.isSuccess
    }
}
