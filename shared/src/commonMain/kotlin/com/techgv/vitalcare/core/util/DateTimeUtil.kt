package com.techgv.vitalcare.core.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * All clock access goes through these helpers so features never touch
 * platform time APIs directly and tests can inject a fixed [Clock] (D-016).
 */
fun Clock.nowLocal(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    now().toLocalDateTime(timeZone)

fun Clock.todayLocal(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    nowLocal(timeZone).date

fun Clock.nowEpochMillis(): Long = now().toEpochMilliseconds()

/** "Thursday, 3 July" — Dashboard app-bar date (03 §3.5). */
val DashboardDateFormat: kotlinx.datetime.format.DateTimeFormat<LocalDate> = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
    chars(", ")
    day(padding = Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
}

/** "3 July 2026" — history date headers, record details, read-only date field. */
val FullDateFormat: kotlinx.datetime.format.DateTimeFormat<LocalDate> = LocalDate.Format {
    day(padding = Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    year()
}

/** "28 Jun" — compact chart axis labels. */
val ShortDateFormat: kotlinx.datetime.format.DateTimeFormat<LocalDate> = LocalDate.Format {
    day(padding = Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
}

/** "08:30" — record rows and the time field. */
val TimeFormat: kotlinx.datetime.format.DateTimeFormat<LocalTime> = LocalTime.Format {
    hour()
    char(':')
    minute()
}

private val CsvStampFormat = LocalDateTime.Format {
    year(); monthNumber(); day()
    char('-')
    hour(); minute()
}

/** "vitalcare-20260703-0830.csv" — CSV export file name (05 §3). */
fun csvExportFileName(now: LocalDateTime): String = "vitalcare-${CsvStampFormat.format(now)}.csv"

/** "vitalcare-fluids-20260703-0830.csv" — fluids CSV export file name (D-032). */
fun fluidCsvExportFileName(now: LocalDateTime): String =
    "vitalcare-fluids-${CsvStampFormat.format(now)}.csv"

/**
 * Semantic date label for history grouping (03 §3.7). The UI resolves
 * Today/Yesterday to localized strings; domain code stays resource-free.
 */
sealed interface DateLabel {
    data object Today : DateLabel
    data object Yesterday : DateLabel
    data class Other(val date: LocalDate) : DateLabel
}

fun LocalDate.toDateLabel(today: LocalDate): DateLabel = when (this) {
    today -> DateLabel.Today
    today.minus(1, DateTimeUnit.DAY) -> DateLabel.Yesterday
    else -> DateLabel.Other(this)
}
