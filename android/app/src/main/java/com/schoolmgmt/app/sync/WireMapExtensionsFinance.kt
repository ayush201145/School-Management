package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.ExpenseEntity
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import com.schoolmgmt.app.data.local.entity.StaffEntity

fun StaffEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "teacherId" to teacherId,
    "phone" to phone,
    "monthlySalary" to monthlySalary,
    "joiningDate" to IsoDates.toIsoString(joiningDate),
    "isActive" to isActive,
)

fun SalaryPaymentEntity.toWireMap(): Map<String, Any?> = mapOf(
    "staffId" to staffId,
    "amount" to amount,
    "forMonth" to forMonth,
    "forYear" to forYear,
    "paidAt" to IsoDates.toIsoString(paidAt),
    "mode" to mode.name,
    "referenceNo" to referenceNo,
    "notes" to notes,
    "recordedById" to recordedById,
)

fun ExpenseCategoryEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description,
)

fun RecurringExpenseTemplateEntity.toWireMap(): Map<String, Any?> = mapOf(
    "expenseCategoryId" to expenseCategoryId,
    "label" to label,
    "amount" to amount,
    "frequency" to frequency.name,
    "dayOfMonth" to dayOfMonth,
    "isActive" to isActive,
)

fun ExpenseEntity.toWireMap(): Map<String, Any?> = mapOf(
    "expenseCategoryId" to expenseCategoryId,
    "recurringTemplateId" to recurringTemplateId,
    "description" to description,
    "amount" to amount,
    "spentAt" to IsoDates.toIsoString(spentAt),
    "mode" to mode.name,
    "referenceNo" to referenceNo,
    "recordedById" to recordedById,
)
