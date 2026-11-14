package com.rence.reminders.logic

import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Format {
    private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    private val DATE_YEAR = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)

    fun time(t: LocalTime): String = t.format(TIME)

    fun date(d: LocalDate, today: LocalDate = LocalDate.now()): String =
        if (d.year == today.year) d.format(DATE) else d.format(DATE_YEAR)

    /** "Today 9:00 AM", "Tomorrow 9:00 AM", "Mon, Nov 16 · 9:00 AM" */
    fun relative(dt: LocalDateTime, today: LocalDate = LocalDate.now()): String {
        val d = dt.toLocalDate()
        val day = when (d) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date(d, today)
        }
        return "$day · ${time(dt.toLocalTime())}"
    }

    fun dayShort(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    fun monthShort(m: Int): String = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    fun ordinal(n: Int): String = when {
        n < 0 -> "last"
        n % 100 in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

    fun repeat(rule: RepeatRule, start: LocalDate, today: LocalDate = LocalDate.now()): String {
        val iv = rule.interval.coerceAtLeast(1)
        return when (rule.type) {
            RepeatType.NONE -> "Once on ${date(start, today)}"
            RepeatType.DAILY -> if (iv == 1) "Every day" else "Every $iv days"
            RepeatType.WEEKLY -> {
                val days = rule.weekdays.ifEmpty { setOf(start.dayOfWeek) }.sorted()
                val list = when {
                    days.size == 7 -> "every day"
                    days == listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> "weekdays"
                    days == listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "weekends"
                    else -> days.joinToString(", ") { dayShort(it) }
                }
                if (iv == 1) "Every $list" else "Every $iv weeks on $list"
            }
            RepeatType.MONTHLY -> {
                val base = if (iv == 1) "Monthly" else "Every $iv months"
                when (rule.monthlyMode) {
                    MonthlyMode.DAY_OF_MONTH -> "$base on the ${ordinal(rule.dayOfMonth)}"
                    MonthlyMode.NTH_WEEKDAY -> "$base on the ${ordinal(rule.nth)} ${dayShort(rule.nthWeekday)}"
                    MonthlyMode.LAST_DAY -> "$base on the last day"
                }
            }
            RepeatType.YEARLY -> {
                val base = if (iv == 1) "Yearly" else "Every $iv years"
                "$base on ${monthShort(rule.month)} ${rule.dayOfMonth}"
            }
        }
    }

    fun lead(days: Int): String? = when {
        days <= 0 -> null
        days == 1 -> "heads-up 1 day before"
        days % 7 == 0 -> if (days == 7) "heads-up 1 week before" else "heads-up ${days / 7} weeks before"
        else -> "heads-up $days days before"
    }

    fun summary(rule: RepeatRule, start: LocalDate, time: LocalTime, leadDays: Int, today: LocalDate = LocalDate.now()): String =
        listOfNotNull(repeat(rule, start, today), time(time), lead(leadDays)).joinToString(" · ")
}
