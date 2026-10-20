package com.microbudget.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.microbudget.data.MicroBudgetGraph
import kotlinx.coroutines.launch

/**
 * Alternativa de prueba cuando reloj y celular NO están emparejados en los emuladores.
 * Se invoca desde la PC con:
 *   adb -s <serial> shell am broadcast -a com.microbudget.sync.EXPORT -n com.microbudget/com.microbudget.data.sync.AdbSyncReceiver
 * (el script tools/simular-sync.ps1 lo hace todo automáticamente).
 */
class AdbSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ordered = isOrderedBroadcast // leer antes de goAsync()
        val pending = goAsync()
        val graph = MicroBudgetGraph.get()
        graph.appScope.launch {
            val result = runCatching {
                when (intent.action) {
                    ACTION_EXPORT -> graph.syncManager.adbExport()
                    ACTION_IMPORT -> graph.syncManager.adbImport()
                    ACTION_ACK -> graph.syncManager.adbAck()
                    else -> "Acción desconocida"
                }
            }.getOrElse { "ERROR: ${it.message}" }
            Log.i("MicroBudgetSync", result)
            if (ordered) {
                pending.resultCode = if (result.startsWith("ERROR") || result.startsWith("No existe")) 2 else 1
                pending.resultData = result
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_EXPORT = "com.microbudget.sync.EXPORT"
        const val ACTION_IMPORT = "com.microbudget.sync.IMPORT"
        const val ACTION_ACK = "com.microbudget.sync.ACK"
    }
}
