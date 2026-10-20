package com.microbudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos SQLite local (persistente: sobrevive a cerrar la app y reiniciar el dispositivo). */
@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, BudgetEntity::class, NotifiedAlertEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MicroBudgetDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val FILE_NAME = "microbudget.db"

        fun build(context: Context, name: String = FILE_NAME): MicroBudgetDatabase =
            Room.databaseBuilder(context.applicationContext, MicroBudgetDatabase::class.java, name).build()
    }
}
