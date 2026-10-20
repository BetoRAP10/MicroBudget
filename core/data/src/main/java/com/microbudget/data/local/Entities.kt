package com.microbudget.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Importes en centavos (Long): enteros exactos, sin errores de redondeo. */
@Entity(
    tableName = "expenses",
    indices = [Index("dateTime"), Index("categoryId"), Index("syncStatus")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val categoryId: String,
    val dateTime: Long,
    val description: String?,
    val createdFrom: String,
    val updatedAt: Long,
    val syncStatus: String,
    val deleted: Boolean,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorArgb: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncStatus: String,
)

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val period: String,
    /** LocalDate.toEpochDay() */
    val startEpochDay: Long,
    val endEpochDay: Long,
    val alertAt80: Boolean,
    val alertAt100: Boolean,
    val customAlertCents: Long?,
    val updatedAt: Long,
    val syncStatus: String,
)

/** Umbrales ya notificados en un periodo, para no repetir alertas. */
@Entity(tableName = "notified_alerts", primaryKeys = ["periodKey", "alertKey"])
data class NotifiedAlertEntity(
    val periodKey: String,
    val alertKey: String,
    val notifiedAt: Long,
)
