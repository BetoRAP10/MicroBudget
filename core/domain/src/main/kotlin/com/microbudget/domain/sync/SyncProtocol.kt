package com.microbudget.domain.sync

import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.SyncStatus

/**
 * Almacenamiento local visto por el protocolo de sincronización.
 * En la app lo implementa Room ([com.microbudget.data.sync.RoomSyncStore]);
 * en las pruebas, una implementación en memoria.
 */
interface SyncStore {
    suspend fun <R> transaction(block: suspend () -> R): R

    suspend fun pendingExpenses(): List<ExpenseDto>
    suspend fun allExpenses(): List<ExpenseDto>
    suspend fun expense(id: String): ExpenseDto?
    suspend fun saveExpense(dto: ExpenseDto, status: SyncStatus)
    /** Marca como sincronizado solo si la versión local sigue siendo [updatedAt]. */
    suspend fun markExpenseSynced(id: String, updatedAt: Long)

    suspend fun pendingCategories(): List<CategoryDto>
    suspend fun allCategories(): List<CategoryDto>
    suspend fun category(id: String): CategoryDto?
    suspend fun saveCategory(dto: CategoryDto, status: SyncStatus)
    suspend fun markCategorySynced(id: String, updatedAt: Long)

    suspend fun budget(): BudgetDto?
    suspend fun isBudgetPending(): Boolean
    suspend fun saveBudget(dto: BudgetDto, status: SyncStatus)
    suspend fun markBudgetSynced(updatedAt: Long)
}

/**
 * Protocolo PUSH/ACK, independiente del transporte:
 *
 *  Dispositivo A                         Dispositivo B
 *  buildPush() -- PUSH(cambios pendientes) -->  handlePush(): fusiona con LWW
 *              <-- ACK(versión ganadora de cada id) --
 *  handleAck(): fusiona y marca SYNCED lo que B confirmó tener idéntico.
 *
 * Un registro solo pasa a SYNCED cuando el otro dispositivo demuestra tener la misma versión.
 * Si no hay conexión, los cambios quedan PENDING y se reenvían en el siguiente intento.
 */
class SyncProtocol(
    private val store: SyncStore,
    private val origin: DeviceSource,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun buildPush(full: Boolean): SyncBundle = store.transaction {
        SyncBundle(
            origin = origin.name,
            kind = SyncBundle.KIND_PUSH,
            sentAt = now(),
            requestFull = full,
            categories = if (full) store.allCategories() else store.pendingCategories(),
            expenses = if (full) store.allExpenses() else store.pendingExpenses(),
            budget = if (full || store.isBudgetPending()) store.budget() else null,
        )
    }

    /** Procesa un PUSH recibido y devuelve el ACK que se debe enviar de vuelta. */
    suspend fun handlePush(incoming: SyncBundle): SyncBundle = store.transaction {
        // Las categorías primero, para que los gastos que llegan ya tengan su categoría.
        val cats = incoming.categories.map { mergeCategory(it) }.toMutableList()
        val exps = incoming.expenses.map { mergeExpense(it) }.toMutableList()
        var budget = incoming.budget?.let { mergeBudget(it) }

        if (incoming.requestFull) {
            val sentCats = cats.map { it.id }.toSet()
            val sentExps = exps.map { it.id }.toSet()
            cats += store.allCategories().filter { it.id !in sentCats }
            exps += store.allExpenses().filter { it.id !in sentExps }
            if (budget == null) budget = store.budget()
        }
        SyncBundle(
            origin = origin.name,
            kind = SyncBundle.KIND_ACK,
            sentAt = now(),
            categories = cats,
            expenses = exps,
            budget = budget,
        )
    }

    /** Procesa el ACK: aplica las versiones ganadoras y marca como sincronizado lo confirmado. */
    suspend fun handleAck(ack: SyncBundle): MergeStats = store.transaction {
        val stats = MergeStats()
        ack.categories.forEach { stats.count(decideCategory(it)); mergeCategory(it) }
        ack.expenses.forEach { stats.count(decideExpense(it)); mergeExpense(it) }
        ack.budget?.let { stats.count(decideBudget(it)); mergeBudget(it) }
        stats
    }

    class MergeStats {
        var taken = 0; private set
        var confirmed = 0; private set
        var keptLocal = 0; private set
        fun count(d: MergeDecision) {
            when (d) {
                MergeDecision.TAKE_REMOTE -> taken++
                MergeDecision.SAME -> confirmed++
                MergeDecision.KEEP_LOCAL -> keptLocal++
            }
        }
    }

    private suspend fun decideExpense(r: ExpenseDto) = ConflictResolver.decide(store.expense(r.id), r)
    private suspend fun decideCategory(r: CategoryDto) = ConflictResolver.decide(store.category(r.id), r)
    private suspend fun decideBudget(r: BudgetDto) = ConflictResolver.decide(store.budget(), r)

    private suspend fun mergeExpense(remote: ExpenseDto): ExpenseDto {
        val local = store.expense(remote.id)
        return when (ConflictResolver.decide(local, remote)) {
            MergeDecision.TAKE_REMOTE -> remote.also { store.saveExpense(it, SyncStatus.SYNCED) }
            MergeDecision.SAME -> remote.also { store.markExpenseSynced(it.id, it.updatedAt) }
            MergeDecision.KEEP_LOCAL -> local!!
        }
    }

    private suspend fun mergeCategory(remote: CategoryDto): CategoryDto {
        val local = store.category(remote.id)
        return when (ConflictResolver.decide(local, remote)) {
            MergeDecision.TAKE_REMOTE -> remote.also { store.saveCategory(it, SyncStatus.SYNCED) }
            MergeDecision.SAME -> remote.also { store.markCategorySynced(it.id, it.updatedAt) }
            MergeDecision.KEEP_LOCAL -> local!!
        }
    }

    private suspend fun mergeBudget(remote: BudgetDto): BudgetDto {
        val local = store.budget()
        return when (ConflictResolver.decide(local, remote)) {
            MergeDecision.TAKE_REMOTE -> remote.also { store.saveBudget(it, SyncStatus.SYNCED) }
            MergeDecision.SAME -> remote.also { store.markBudgetSynced(it.updatedAt) }
            MergeDecision.KEEP_LOCAL -> local!!
        }
    }
}
