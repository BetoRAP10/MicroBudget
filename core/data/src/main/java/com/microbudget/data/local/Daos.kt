package com.microbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deleted = 0 ORDER BY dateTime DESC")
    fun observeActive(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE deleted = 0 ORDER BY dateTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE deleted = 0")
    suspend fun active(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses")
    suspend fun all(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun pending(): List<ExpenseEntity>

    @Query("SELECT COUNT(*) FROM expenses WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query("UPDATE expenses SET syncStatus = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    /** Al eliminar una categoría sus gastos pasan a "Otros" (y se sincroniza el cambio). */
    @Query(
        "UPDATE expenses SET categoryId = :to, updatedAt = :now, syncStatus = 'PENDING' " +
            "WHERE categoryId = :from AND deleted = 0",
    )
    suspend fun reassignCategory(from: String, to: String, now: Long): Int

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: String): CategoryEntity?

    @Query("SELECT * FROM categories")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun pending(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entities: List<CategoryEntity>)

    @Query("UPDATE categories SET syncStatus = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE id = :id")
    fun observe(id: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun get(id: String): BudgetEntity?

    @Query("SELECT COUNT(*) FROM budget WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entity: BudgetEntity)

    @Query("UPDATE budget SET syncStatus = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM budget")
    suspend fun deleteAll()
}

@Dao
interface AlertDao {
    @Query("SELECT alertKey FROM notified_alerts WHERE periodKey = :periodKey")
    suspend fun notifiedKeys(periodKey: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entities: List<NotifiedAlertEntity>)

    @Query("DELETE FROM notified_alerts WHERE periodKey = :periodKey AND alertKey IN (:keys)")
    suspend fun delete(periodKey: String, keys: List<String>)

    /** Restablece el estado al empezar otro periodo. */
    @Query("DELETE FROM notified_alerts WHERE periodKey != :currentPeriodKey")
    suspend fun deleteOtherPeriods(currentPeriodKey: String)

    @Query("DELETE FROM notified_alerts")
    suspend fun deleteAll()
}
