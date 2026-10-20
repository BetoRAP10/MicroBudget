package com.microbudget.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.microbudget.data.alerts.AlertNotifier
import com.microbudget.data.alerts.AlertService
import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.repository.BudgetRepository
import com.microbudget.data.repository.CategoryRepository
import com.microbudget.data.repository.ExpenseRepository
import com.microbudget.data.repository.SaveResult
import com.microbudget.domain.budget.AlertType
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Clock

/**
 * Pruebas instrumentadas (se ejecutan en el emulador) sobre la base de datos real.
 * Casos 1, 2, 4, 5, 6 y 7 del plan de pruebas.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "test-microbudget.db"
    private val clock = Clock.systemDefaultZone()
    private lateinit var db: MicroBudgetDatabase
    private lateinit var alerts: AlertService

    private fun open() {
        db = MicroBudgetDatabase.build(context, dbName)
        alerts = AlertService(db, clock, AlertNotifier(context))
    }

    private fun expenses() = ExpenseRepository(db, clock, DeviceSource.WATCH) { alerts.evaluateAndNotify() }
    private fun budgets() = BudgetRepository(db, clock) { alerts.evaluateAndNotify() }
    private fun categories() = CategoryRepository(db, clock) { alerts.evaluateAndNotify() }

    @Before fun setUp() = runBlocking {
        context.deleteDatabase(dbName)
        open()
        categories().ensureSeeded()
        budgets().ensureSeededAndCurrent()
    }

    @After fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    @Test fun guardaGastoConCategoriaYEstadoPendiente() = runBlocking {
        val r = expenses().add(BigDecimal("45.50"), DefaultCategories.TRANSPORT, description = "Metro")
        assertTrue(r is SaveResult.Success)
        val saved = expenses().get((r as SaveResult.Success).expense.id)
        assertNotNull(saved)
        assertEquals(BigDecimal("45.50"), saved!!.amount)
        assertEquals(DefaultCategories.TRANSPORT, saved.categoryId)
        assertEquals(DeviceSource.WATCH, saved.createdFrom)
        assertEquals(SyncStatus.PENDING, saved.syncStatus)
    }

    @Test fun rechazaImportesInvalidosYCategoriasInexistentes() = runBlocking {
        assertTrue(expenses().add(BigDecimal("-3"), DefaultCategories.FOOD) is SaveResult.Error)
        assertTrue(expenses().add(BigDecimal.ZERO, DefaultCategories.FOOD) is SaveResult.Error)
        assertTrue(expenses().add(BigDecimal("1.234"), DefaultCategories.FOOD) is SaveResult.Error)
        assertTrue(expenses().add(BigDecimal("10"), "no-existe") is SaveResult.Error)
        assertEquals(0, db.expenseDao().all().size)
    }

    @Test fun losGastosPermanecenAlCerrarYReabrir() = runBlocking {
        val id = (expenses().add(BigDecimal("99.99"), DefaultCategories.FOOD) as SaveResult.Success).expense.id
        db.close()          // simula cerrar la app
        open()              // simula volver a abrirla
        assertEquals(BigDecimal("99.99"), expenses().get(id)!!.amount)
    }

    @Test fun editarYEliminarActualizaTotales() = runBlocking {
        budgets().setBudget(BigDecimal("1000"), com.microbudget.domain.model.BudgetPeriod.MONTHLY)
        val repo = expenses()
        val a = (repo.add(BigDecimal("300"), DefaultCategories.FOOD) as SaveResult.Success).expense
        repo.add(BigDecimal("200"), DefaultCategories.SHOPPING)
        val summary = { runBlocking { budgets().observeSummary(repo.observeActive()).first() } }
        assertEquals(BigDecimal("500.00"), summary().spent)

        repo.update(a.id, BigDecimal("100"), DefaultCategories.FOOD, a.dateTime, null)
        assertEquals(BigDecimal("300.00"), summary().spent)

        repo.delete(a.id)
        assertEquals(BigDecimal("200.00"), summary().spent)
        assertNull(repo.get(a.id))
        assertTrue(db.expenseDao().get(a.id)!!.deleted) // lápida para sincronizar el borrado
    }

    @Test fun alertasSeDisparanUnaSolaVez() = runBlocking {
        budgets().setBudget(BigDecimal("100"), com.microbudget.domain.model.BudgetPeriod.MONTHLY)
        val repo = expenses()
        assertTrue((repo.add(BigDecimal("50"), DefaultCategories.FOOD) as SaveResult.Success).alerts.isEmpty())
        assertEquals(listOf(AlertType.PERCENT_80), (repo.add(BigDecimal("30"), DefaultCategories.FOOD) as SaveResult.Success).alerts.map { it.type })
        assertTrue((repo.add(BigDecimal("5"), DefaultCategories.FOOD) as SaveResult.Success).alerts.isEmpty())
        assertEquals(listOf(AlertType.PERCENT_100), (repo.add(BigDecimal("20"), DefaultCategories.FOOD) as SaveResult.Success).alerts.map { it.type })
        assertTrue((repo.add(BigDecimal("1"), DefaultCategories.FOOD) as SaveResult.Success).alerts.isEmpty())
    }

    @Test fun eliminarCategoriaMueveGastosAOtros() = runBlocking {
        val cat = categories().add("Café", "☕", 0xFFA78BFA).getOrThrow()
        val e = (expenses().add(BigDecimal("40"), cat.id) as SaveResult.Success).expense
        assertEquals(1, categories().delete(cat.id).getOrThrow())
        assertEquals(DefaultCategories.OTHER, expenses().get(e.id)!!.categoryId)
        assertTrue(categories().delete(DefaultCategories.OTHER).isFailure)
    }
}
