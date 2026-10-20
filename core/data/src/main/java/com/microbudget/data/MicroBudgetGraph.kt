package com.microbudget.data

import android.content.Context
import com.microbudget.data.alerts.AlertNotifier
import com.microbudget.data.alerts.AlertService
import com.microbudget.data.demo.DemoDataSeeder
import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.repository.BudgetRepository
import com.microbudget.data.repository.CategoryRepository
import com.microbudget.data.repository.ExpenseRepository
import com.microbudget.data.sync.RoomSyncStore
import com.microbudget.data.sync.SyncManager
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.sync.SyncProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Contenedor de dependencias (inyección manual). Se crea una vez en la clase Application
 * de cada app indicando si es el reloj o el celular.
 */
class MicroBudgetGraph private constructor(context: Context, val deviceSource: DeviceSource) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock: Clock = Clock.systemDefaultZone()

    val database = MicroBudgetDatabase.build(appContext)
    val notifier = AlertNotifier(appContext)
    val alertService = AlertService(database, clock, notifier)

    private val protocol = SyncProtocol(RoomSyncStore(database, clock), deviceSource, clock::millis)

    val syncManager = SyncManager(
        context = appContext,
        protocol = protocol,
        scope = appScope,
        pendingCount = combine(
            database.expenseDao().observePendingCount(),
            database.categoryDao().observePendingCount(),
            database.budgetDao().observePendingCount(),
        ) { a, b, c -> a + b + c },
        onRemoteChanges = { alertService.evaluateAndNotify() },
    )

    private suspend fun afterLocalChange(): List<BudgetAlert> {
        val alerts = alertService.evaluateAndNotify()
        syncManager.requestSync()
        return alerts
    }

    val budgetRepository = BudgetRepository(database, clock, ::afterLocalChange)
    val expenseRepository = ExpenseRepository(database, clock, deviceSource, ::afterLocalChange)
    val categoryRepository = CategoryRepository(database, clock, ::afterLocalChange)
    val demoData = DemoDataSeeder(database, clock, deviceSource) { afterLocalChange() }

    init {
        appScope.launch {
            categoryRepository.ensureSeeded()
            budgetRepository.ensureSeededAndCurrent()
            alertService.evaluateAndNotify()
            syncManager.syncNow()
        }
    }

    /** Llamar al volver a primer plano: actualiza el periodo y reintenta enviar lo pendiente. */
    fun onAppForeground() {
        appScope.launch {
            budgetRepository.ensureSeededAndCurrent()
            syncManager.syncNow()
        }
    }

    companion object {
        @Volatile private var instance: MicroBudgetGraph? = null

        fun init(context: Context, source: DeviceSource): MicroBudgetGraph =
            instance ?: synchronized(this) { instance ?: MicroBudgetGraph(context, source).also { instance = it } }

        fun get(): MicroBudgetGraph = checkNotNull(instance) { "MicroBudgetGraph.init() no se ha llamado en Application.onCreate()" }
    }
}
