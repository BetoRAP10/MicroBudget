package com.microbudget.domain

import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.sync.BudgetDto
import com.microbudget.domain.sync.CategoryDto
import com.microbudget.domain.sync.ConflictResolver
import com.microbudget.domain.sync.ExpenseDto
import com.microbudget.domain.sync.MergeDecision
import com.microbudget.domain.sync.SyncCodec
import com.microbudget.domain.sync.SyncProtocol
import com.microbudget.domain.sync.SyncStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos 8 y 9: sincronización reloj <-> celular y gastos registrados sin conexión. */
class SyncProtocolTest {

    /** Almacenamiento en memoria que imita a Room. */
    private class MemoryStore : SyncStore {
        val expenses = linkedMapOf<String, Pair<ExpenseDto, SyncStatus>>()
        val categories = linkedMapOf<String, Pair<CategoryDto, SyncStatus>>()
        var budget: Pair<BudgetDto, SyncStatus>? = null

        override suspend fun <R> transaction(block: suspend () -> R): R = block()
        override suspend fun pendingExpenses() = expenses.values.filter { it.second == SyncStatus.PENDING }.map { it.first }
        override suspend fun allExpenses() = expenses.values.map { it.first }
        override suspend fun expense(id: String) = expenses[id]?.first
        override suspend fun saveExpense(dto: ExpenseDto, status: SyncStatus) { expenses[dto.id] = dto to status }
        override suspend fun markExpenseSynced(id: String, updatedAt: Long) {
            expenses[id]?.let { if (it.first.updatedAt == updatedAt) expenses[id] = it.first to SyncStatus.SYNCED }
        }
        override suspend fun pendingCategories() = categories.values.filter { it.second == SyncStatus.PENDING }.map { it.first }
        override suspend fun allCategories() = categories.values.map { it.first }
        override suspend fun category(id: String) = categories[id]?.first
        override suspend fun saveCategory(dto: CategoryDto, status: SyncStatus) { categories[dto.id] = dto to status }
        override suspend fun markCategorySynced(id: String, updatedAt: Long) {
            categories[id]?.let { if (it.first.updatedAt == updatedAt) categories[id] = it.first to SyncStatus.SYNCED }
        }
        override suspend fun budget() = budget?.first
        override suspend fun isBudgetPending() = budget?.second == SyncStatus.PENDING
        override suspend fun saveBudget(dto: BudgetDto, status: SyncStatus) { budget = dto to status }
        override suspend fun markBudgetSynced(updatedAt: Long) {
            budget?.let { if (it.first.updatedAt == updatedAt) budget = it.first to SyncStatus.SYNCED }
        }

        fun localEdit(e: ExpenseDto) { expenses[e.id] = e to SyncStatus.PENDING }
        fun status(id: String) = expenses[id]!!.second
    }

    private class Device(source: DeviceSource) {
        val store = MemoryStore()
        val protocol = SyncProtocol(store, source) { 0L }
    }

    /** Transporte simulado: serializa a bytes igual que el Data Layer real. */
    private fun sync(from: Device, to: Device, full: Boolean = false) = runBlocking {
        val push = SyncCodec.decode(SyncCodec.encode(from.protocol.buildPush(full)))
        val ack = SyncCodec.decode(SyncCodec.encode(to.protocol.handlePush(push)))
        from.protocol.handleAck(ack)
    }

    private fun exp(id: String, amount: String, updatedAt: Long, deleted: Boolean = false, src: String = "WATCH") =
        ExpenseDto(id, amount, "food", 1_700_000_000_000, null, src, updatedAt, deleted)

    @Test fun `gastos sin conexion quedan pendientes y se sincronizan despues`() {
        val watch = Device(DeviceSource.WATCH)
        val phone = Device(DeviceSource.PHONE)
        // Sin conexión: el reloj registra 3 gastos; nadie los confirma.
        watch.store.localEdit(exp("w1", "10.00", 100))
        watch.store.localEdit(exp("w2", "20.00", 101))
        watch.store.localEdit(exp("w3", "30.50", 102))
        assertEquals(3, runBlocking { watch.store.pendingExpenses() }.size)
        assertTrue(phone.store.expenses.isEmpty())

        // Vuelve la conexión.
        sync(watch, phone)
        assertEquals(setOf("w1", "w2", "w3"), phone.store.expenses.keys)
        assertEquals("30.50", phone.store.expenses["w3"]!!.first.amount)
        assertTrue(runBlocking { watch.store.pendingExpenses() }.isEmpty())
        assertTrue(phone.store.expenses.values.all { it.second == SyncStatus.SYNCED })
    }

    @Test fun `cambios en ambos sentidos convergen`() {
        val watch = Device(DeviceSource.WATCH)
        val phone = Device(DeviceSource.PHONE)
        watch.store.localEdit(exp("w1", "10.00", 100))
        phone.store.localEdit(exp("p1", "99.90", 100, src = "PHONE"))
        sync(watch, phone)
        sync(phone, watch)
        assertEquals(watch.store.allExpensesBlocking(), phone.store.allExpensesBlocking())
        assertTrue(watch.store.expenses.values.all { it.second == SyncStatus.SYNCED })
        assertTrue(phone.store.expenses.values.all { it.second == SyncStatus.SYNCED })
    }

    @Test fun `conflicto - gana la edicion mas reciente en ambos dispositivos`() {
        val watch = Device(DeviceSource.WATCH)
        val phone = Device(DeviceSource.PHONE)
        watch.store.saveBlocking(exp("x", "50.00", 100), SyncStatus.SYNCED)
        phone.store.saveBlocking(exp("x", "50.00", 100), SyncStatus.SYNCED)
        // Ambos editan sin conexión; el celular lo hace después.
        watch.store.localEdit(exp("x", "55.00", 200))
        phone.store.localEdit(exp("x", "60.00", 300))
        sync(watch, phone)   // el celular conserva 60 y responde con su versión
        assertEquals("60.00", watch.store.expenses["x"]!!.first.amount)
        assertEquals(SyncStatus.SYNCED, watch.store.status("x"))
        sync(phone, watch)   // el celular confirma
        assertEquals(SyncStatus.SYNCED, phone.store.status("x"))
        assertEquals("60.00", phone.store.expenses["x"]!!.first.amount)
    }

    @Test fun `eliminar se sincroniza como lapida`() {
        val watch = Device(DeviceSource.WATCH)
        val phone = Device(DeviceSource.PHONE)
        watch.store.localEdit(exp("d", "15.00", 100))
        sync(watch, phone)
        phone.store.localEdit(exp("d", "15.00", 200, deleted = true))
        sync(phone, watch)
        assertTrue(watch.store.expenses["d"]!!.first.deleted)
    }

    @Test fun `empate de updatedAt es determinista y el borrado gana`() {
        val a = exp("t", "10.00", 500)
        val b = exp("t", "11.00", 500)
        assertEquals(ConflictResolver.decide(a, b) == MergeDecision.TAKE_REMOTE, ConflictResolver.decide(b, a) == MergeDecision.KEEP_LOCAL)
        assertEquals(MergeDecision.TAKE_REMOTE, ConflictResolver.decide(a, a.copy(deleted = true)))
        assertEquals(MergeDecision.SAME, ConflictResolver.decide(a, a.copy()))
    }

    @Test fun `sincronizacion completa trae todo del otro dispositivo`() {
        val watch = Device(DeviceSource.WATCH)
        val phone = Device(DeviceSource.PHONE)
        phone.store.saveBlocking(exp("old", "5.00", 10), SyncStatus.SYNCED) // ya marcado, no se reenviaría
        phone.store.categories["cafe"] = CategoryDto("cafe", "Café", "☕", 0xFFA78BFA, updatedAt = 10) to SyncStatus.SYNCED
        sync(watch, phone, full = true)
        assertTrue("old" in watch.store.expenses)
        assertTrue("cafe" in watch.store.categories)
    }

    private fun MemoryStore.allExpensesBlocking() = runBlocking { allExpenses().sortedBy { it.id } }
    private fun MemoryStore.saveBlocking(e: ExpenseDto, s: SyncStatus) = runBlocking { saveExpense(e, s) }
}
