package com.microbudget.data.sync

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.microbudget.data.MicroBudgetGraph
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Servicio que Google Play Services despierta cuando llega un mensaje del otro dispositivo
 * o cuando el otro dispositivo aparece (reconexión): así los gastos guardados sin conexión
 * se envían en cuanto vuelve la conexión.
 */
class MicroBudgetListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (!messageEvent.path.startsWith("/microbudget")) return
        val graph = MicroBudgetGraph.get()
        // Los callbacks de este servicio se ejecutan en un hilo de fondo; se puede bloquear.
        runBlocking {
            withTimeoutOrNull(20_000) {
                graph.syncManager.handleMessage(messageEvent.sourceNodeId, messageEvent.path, messageEvent.data)
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.nodes.isNotEmpty()) MicroBudgetGraph.get().syncManager.requestSync()
    }
}
