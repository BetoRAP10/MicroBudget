package com.microbudget.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Wearable
import com.microbudget.domain.sync.SyncBundle
import com.microbudget.domain.sync.SyncCodec
import com.microbudget.domain.sync.SyncProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

enum class ConnectionState {
    /** Todavía no se ha intentado. */
    UNKNOWN,

    /** Hay un reloj/celular con MicroBudget conectado. */
    CONNECTED,

    /** El Data Layer funciona pero no hay otro dispositivo conectado. */
    NO_PEER,

    /** Este dispositivo no tiene Wear OS / Google Play Services para Wear (p. ej. celular sin emparejar). */
    API_UNAVAILABLE,
}

data class SyncUiState(
    val pendingCount: Int = 0,
    val connection: ConnectionState = ConnectionState.UNKNOWN,
    val peerName: String? = null,
    val lastSyncAt: Long? = null,
    val inProgress: Boolean = false,
    val message: String = "Aún no se ha sincronizado",
) {
    val isFullySynced: Boolean get() = pendingCount == 0
}

/**
 * Sincronización reloj <-> celular.
 *
 * Transporte principal: Wearable Data Layer (MessageClient), disponible cuando el emulador del reloj
 * está emparejado con un teléfono que tiene la app Wear OS.
 * Transporte alternativo de prueba: archivos intercambiados por ADB (ver tools/simular-sync.ps1).
 * Ambos usan el mismo [SyncProtocol] (PUSH/ACK + Last-Writer-Wins).
 */
class SyncManager(
    private val context: Context,
    private val protocol: SyncProtocol,
    private val scope: CoroutineScope,
    pendingCount: Flow<Int>,
    private val onRemoteChanges: suspend () -> Unit,
) {
    private val prefs = context.getSharedPreferences("microbudget_sync", Context.MODE_PRIVATE)
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val mutex = Mutex()

    private val status = MutableStateFlow(
        SyncUiState(lastSyncAt = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }),
    )

    val state: StateFlow<SyncUiState> = combine(pendingCount, status) { pending, s -> s.copy(pendingCount = pending) }
        .stateIn(scope, SharingStarted.Eagerly, status.value)

    /** Lanza una sincronización en segundo plano (después de cada cambio, al abrir la app, etc.). */
    fun requestSync(full: Boolean = false) {
        scope.launch { syncNow(full) }
    }

    suspend fun syncNow(full: Boolean = false): SyncUiState = mutex.withLock {
        status.update { it.copy(inProgress = true) }
        try {
            val nodes = withTimeoutOrNull(NODE_TIMEOUT_MS) { nodeClient.connectedNodes.await() }
            if (nodes.isNullOrEmpty()) {
                status.update {
                    it.copy(
                        connection = ConnectionState.NO_PEER, peerName = null,
                        message = "Sin conexión con el otro dispositivo. Los cambios se guardan aquí y se enviarán después.",
                    )
                }
                return@withLock state.value
            }
            val peer = nodes.first()
            val bundle = protocol.buildPush(full)
            if (bundle.isEmpty && !full) {
                markSynced(peer.displayName, "Todo sincronizado con ${peer.displayName}")
                return@withLock state.value
            }
            for (chunk in chunk(bundle)) {
                val bytes = SyncCodec.encode(chunk)
                nodes.forEach { node -> messageClient.sendMessage(node.id, PATH_PUSH, bytes).await() }
            }
            status.update {
                it.copy(
                    connection = ConnectionState.CONNECTED, peerName = peer.displayName,
                    message = "Enviados ${bundle.recordCount} cambios a ${peer.displayName}; esperando confirmación…",
                )
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Wearable API no disponible: ${e.statusCode}")
            status.update {
                it.copy(
                    connection = ConnectionState.API_UNAVAILABLE, peerName = null,
                    message = "Wear OS Data Layer no disponible en este dispositivo. Empareja el reloj o usa la sincronización por ADB.",
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Error al sincronizar", e)
            status.update { it.copy(message = "Error al sincronizar: ${e.message ?: e.javaClass.simpleName}") }
        } finally {
            status.update { it.copy(inProgress = false) }
        }
        state.value
    }

    /** Llamado por [MicroBudgetListenerService] al recibir un mensaje del otro dispositivo. */
    suspend fun handleMessage(sourceNodeId: String, path: String, data: ByteArray) = mutex.withLock {
        try {
            val bundle = SyncCodec.decode(data)
            when (path) {
                PATH_PUSH -> {
                    val ack = protocol.handlePush(bundle)
                    for (chunk in chunk(ack)) {
                        messageClient.sendMessage(sourceNodeId, PATH_ACK, SyncCodec.encode(chunk)).await()
                    }
                    markSynced(peerLabel(bundle.origin), "Recibidos ${bundle.recordCount} cambios del ${peerLabel(bundle.origin)}")
                }
                PATH_ACK -> {
                    val stats = protocol.handleAck(bundle)
                    markSynced(peerLabel(bundle.origin), "Sincronizado con el ${peerLabel(bundle.origin)} (${stats.confirmed} confirmados, ${stats.taken} recibidos)")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Mensaje de sincronización inválido en $path", e)
        }
        onRemoteChanges()
    }

    // ---------------- Alternativa de prueba por ADB (emuladores sin emparejar) ----------------

    private fun syncDir(): File = (context.getExternalFilesDir("sync") ?: File(context.filesDir, "sync")).apply { mkdirs() }

    /** Escribe outbox.json con TODOS los registros. */
    suspend fun adbExport(): String = mutex.withLock {
        val bundle = protocol.buildPush(full = true)
        File(syncDir(), "outbox.json").writeBytes(SyncCodec.encode(bundle))
        "Exportados ${bundle.recordCount} registros a ${File(syncDir(), "outbox.json").absolutePath}"
    }

    /** Lee inbox.json (PUSH del otro dispositivo), fusiona y escribe response.json (ACK). */
    suspend fun adbImport(): String {
        val msg = mutex.withLock {
            val inbox = File(syncDir(), "inbox.json")
            if (!inbox.exists()) return@withLock "No existe ${inbox.absolutePath}"
            val push = SyncCodec.decode(inbox.readBytes())
            val ack = protocol.handlePush(push)
            File(syncDir(), "response.json").writeBytes(SyncCodec.encode(ack))
            inbox.delete()
            markSynced("${peerLabel(push.origin)} (ADB)", "Recibidos ${push.recordCount} registros del ${peerLabel(push.origin)} por ADB")
            "Importados ${push.recordCount} registros; respuesta con ${ack.recordCount}"
        }
        onRemoteChanges()
        return msg
    }

    /** Lee ack.json (respuesta del otro dispositivo) y marca como sincronizado lo confirmado. */
    suspend fun adbAck(): String {
        val msg = mutex.withLock {
            val file = File(syncDir(), "ack.json")
            if (!file.exists()) return@withLock "No existe ${file.absolutePath}"
            val ack = SyncCodec.decode(file.readBytes())
            val stats = protocol.handleAck(ack)
            file.delete()
            markSynced("${peerLabel(ack.origin)} (ADB)", "Sincronizado por ADB con el ${peerLabel(ack.origin)}")
            "Confirmados ${stats.confirmed}, recibidos ${stats.taken}, conservados ${stats.keptLocal}"
        }
        onRemoteChanges()
        return msg
    }

    // -------------------------------------------------------------------------------------------

    private fun markSynced(peer: String?, message: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
        status.update { it.copy(connection = ConnectionState.CONNECTED, peerName = peer, lastSyncAt = now, message = message) }
    }

    private fun peerLabel(origin: String) = if (origin == "WATCH") "reloj" else "celular"

    /** Divide paquetes grandes: el MessageClient admite mensajes de tamaño limitado. */
    private fun chunk(bundle: SyncBundle): List<SyncBundle> {
        if (bundle.expenses.size <= CHUNK) return listOf(bundle)
        return bundle.expenses.chunked(CHUNK).mapIndexed { i, part ->
            if (i == 0) bundle.copy(expenses = part) else bundle.copy(expenses = part, categories = emptyList(), budget = null)
        }
    }

    companion object {
        private const val TAG = "MicroBudgetSync"
        const val PATH_PUSH = "/microbudget/sync/push"
        const val PATH_ACK = "/microbudget/sync/ack"
        private const val KEY_LAST_SYNC = "last_sync_at"
        private const val NODE_TIMEOUT_MS = 5_000L
        private const val CHUNK = 150
    }
}
