package com.example.fincontrol.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    private val headerFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

    fun format(date: LocalDate): String = date.format(displayFormatter)

    fun formatHeader(date: LocalDate): String = date.format(headerFormatter)

    fun safeDayForMonth(dayOfMonth: Int, yearMonth: YearMonth): LocalDate {
        val day = minOf(dayOfMonth, yearMonth.lengthOfMonth())
        return yearMonth.atDay(day)
    }
}
