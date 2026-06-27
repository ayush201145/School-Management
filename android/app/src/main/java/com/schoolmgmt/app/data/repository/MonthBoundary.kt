package com.schoolmgmt.app.data.repository

import java.util.Calendar

/**
 * Month-boundary helpers shared by ExpenseRepository and ReportRepository.
 * monthStartMillis's month=13 overflow behavior (rolling into January of
 * next year, needed for a calendar month's EXCLUSIVE upper bound) was
 * empirically verified with a standalone Java test during development —
 * see the git history / session notes for ExpenseRepository.kt — not
 * just assumed from Calendar's documentation.
 */
object MonthBoundary {
    fun startMillis(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, 1, 0, 0, 0)
        return cal.timeInMillis
    }

    fun dayInMonthMillis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, day, 0, 0, 0)
        return cal.timeInMillis
    }
}
