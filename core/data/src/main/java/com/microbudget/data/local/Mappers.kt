package com.microbudget.data.local

import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.model.Budget
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.Category
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.Expense
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.Money
import com.microbudget.domain.sync.BudgetDto
import com.microbudget.domain.sync.CategoryDto
import com.microbudget.domain.sync.ExpenseDto
import java.math.BigDecimal
import java.time.LocalDate

private fun status(s: String) = runCatching { SyncStatus.valueOf(s) }.getOrDefault(SyncStatus.PENDING)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = Money.fromCents(amountCents),
    categoryId = categoryId,
    dateTime = dateTime,
    description = description,
    createdFrom = runCatching { DeviceSource.valueOf(createdFrom) }.getOrDefault(DeviceSource.PHONE),
    updatedAt = updatedAt,
    syncStatus = status(syncStatus),
    deleted = deleted,
)

fun Expense.toEntity() = ExpenseEntity(
    id, Money.toCents(amount), categoryId, dateTime, description, createdFrom.name, updatedAt, syncStatus.name, deleted,
)

fun ExpenseEntity.toDto() = ExpenseDto(
    id, Money.toPlain(Money.fromCents(amountCents)), categoryId, dateTime, description, createdFrom, updatedAt, deleted,
)

fun ExpenseDto.toEntity(status: SyncStatus) = ExpenseEntity(
    id, Money.toCents(BigDecimal(amount)), categoryId, dateTime, description, createdFrom, updatedAt, status.name, deleted,
)

fun CategoryEntity.toDomain() = Category(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted, status(syncStatus))

fun Category.toEntity() = CategoryEntity(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted, syncStatus.name)

fun CategoryEntity.toDto() = CategoryDto(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted)

fun CategoryDto.toEntity(status: SyncStatus) =
    CategoryEntity(id, name, icon, colorArgb, isDefault, sortOrder, updatedAt, deleted, status.name)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    amount = Money.fromCents(amountCents),
    period = runCatching { BudgetPeriod.valueOf(period) }.getOrDefault(BudgetPeriod.MONTHLY),
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = LocalDate.ofEpochDay(endEpochDay),
    alertAt80 = alertAt80,
    alertAt100 = alertAt100,
    customAlertAmount = customAlertCents?.let { Money.fromCents(it) },
    updatedAt = updatedAt,
    syncStatus = status(syncStatus),
)

fun Budget.toEntity() = BudgetEntity(
    id, Money.toCents(amount), period.name, startDate.toEpochDay(), endDate.toEpochDay(),
    alertAt80, alertAt100, customAlertAmount?.let { Money.toCents(it) }, updatedAt, syncStatus.name,
)

fun BudgetEntity.toDto() = BudgetDto(
    id, Money.toPlain(Money.fromCents(amountCents)), period, alertAt80, alertAt100,
    customAlertCents?.let { Money.toPlain(Money.fromCents(it)) }, updatedAt,
)

/** Al recibir un presupuesto del otro dispositivo se recalculan las fechas del periodo localmente. */
fun BudgetDto.toEntity(status: SyncStatus, today: LocalDate): BudgetEntity {
    val p = runCatching { BudgetPeriod.valueOf(period) }.getOrDefault(BudgetPeriod.MONTHLY)
    val range = PeriodCalculator.rangeFor(p, today)
    return BudgetEntity(
        id, Money.toCents(BigDecimal(amount)), p.name, range.start.toEpochDay(), range.end.toEpochDay(),
        alertAt80, alertAt100, customAlertAmount?.let { Money.toCents(BigDecimal(it)) }, updatedAt, status.name,
    )
}
