package com.microbudget.data.repository

import androidx.room.withTransaction
import com.microbudget.data.local.MicroBudgetDatabase
import com.microbudget.data.local.toDomain
import com.microbudget.data.local.toEntity
import com.microbudget.domain.budget.BudgetAlert
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DefaultCategories
import com.microbudget.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

class CategoryRepository(
    private val db: MicroBudgetDatabase,
    private val clock: Clock,
    private val afterLocalChange: suspend () -> List<BudgetAlert>,
) {
    private val dao = db.categoryDao()

    fun observeActive(): Flow<List<Category>> = dao.observeActive().map { list -> list.map { it.toDomain() } }

    /** Crea las 6 categorías iniciales si no existen (ids fijos, idénticas en ambos dispositivos). */
    suspend fun ensureSeeded() {
        dao.insertIfMissing(DefaultCategories.all.map { it.toEntity() })
    }

    suspend fun add(name: String, icon: String, colorArgb: Long): Result<Category> {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 24) return Result.failure(IllegalArgumentException("El nombre debe tener entre 1 y 24 caracteres"))
        if (dao.all().any { !it.deleted && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ya existe una categoría con ese nombre"))
        }
        val c = Category(
            id = UUID.randomUUID().toString(), name = clean, icon = icon, colorArgb = colorArgb,
            isDefault = false, sortOrder = 50, updatedAt = clock.millis(), syncStatus = SyncStatus.PENDING,
        )
        dao.upsert(c.toEntity())
        afterLocalChange()
        return Result.success(c)
    }

    suspend fun update(id: String, name: String, icon: String, colorArgb: Long): Result<Category> {
        val current = dao.get(id)?.takeIf { !it.deleted }?.toDomain()
            ?: return Result.failure(IllegalArgumentException("La categoría no existe"))
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 24) return Result.failure(IllegalArgumentException("El nombre debe tener entre 1 y 24 caracteres"))
        val updated = current.copy(
            name = clean, icon = icon, colorArgb = colorArgb,
            updatedAt = maxOf(clock.millis(), current.updatedAt + 1), syncStatus = SyncStatus.PENDING,
        )
        dao.upsert(updated.toEntity())
        afterLocalChange()
        return Result.success(updated)
    }

    /**
     * Elimina una categoría (lápida). Regla: sus gastos se reasignan a «Otros».
     * «Otros» no se puede eliminar porque es la categoría de respaldo.
     */
    suspend fun delete(id: String): Result<Int> {
        if (id == DefaultCategories.OTHER) return Result.failure(IllegalArgumentException("«Otros» no se puede eliminar"))
        val current = dao.get(id)?.takeIf { !it.deleted }?.toDomain()
            ?: return Result.failure(IllegalArgumentException("La categoría no existe"))
        val now = clock.millis()
        val moved = db.withTransaction {
            dao.upsert(current.copy(deleted = true, updatedAt = maxOf(now, current.updatedAt + 1), syncStatus = SyncStatus.PENDING).toEntity())
            db.expenseDao().reassignCategory(id, DefaultCategories.OTHER, now)
        }
        afterLocalChange()
        return Result.success(moved)
    }
}
