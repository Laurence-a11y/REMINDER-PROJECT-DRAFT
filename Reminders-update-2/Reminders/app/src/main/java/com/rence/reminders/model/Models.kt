package com.rence.reminders.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class RepeatType { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

enum class MonthlyMode { DAY_OF_MONTH, NTH_WEEKDAY, LAST_DAY }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * How a reminder repeats.
 * - WEEKLY uses [weekdays] (empty = the start date's weekday).
 * - MONTHLY uses [monthlyMode] with [dayOfMonth] or [nth] + [nthWeekday].
 * - YEARLY uses [month] + [dayOfMonth].
 * - [interval] = "every N" units (1 = every one).
 */
data class RepeatRule(
    val type: RepeatType = RepeatType.NONE,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    val dayOfMonth: Int = 1,
    val nth: Int = 1, // 1..4, or -1 for "last"
    val nthWeekday: DayOfWeek = DayOfWeek.MONDAY,
    val month: Int = 1,
)

data class Reminder(
    val id: Long,
    val title: String,
    val notes: String = "",
    val categoryId: Long? = null,
    val startDate: LocalDate,
    val time: LocalTime,
    val rule: RepeatRule = RepeatRule(),
    /** Extra heads-up notification this many days before each occurrence (0 = none). */
    val leadDays: Int = 0,
    /** Every occurrence at or before this moment counts as done. */
    val completedThrough: LocalDateTime? = null,
    val snoozeUntil: LocalDateTime? = null,
    val snoozeOccurrence: LocalDateTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Category(val id: Long, val name: String, val color: Long)

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultTime: LocalTime = LocalTime.of(9, 0),
    val onboarded: Boolean = false,
)

data class AppData(
    val categories: List<Category> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val settings: Settings = Settings(),
    val nextId: Long = 1,
)

object Palette {
    val colors: List<Long> = listOf(
        0xFFD71921, 0xFF1565C0, 0xFF2E7D32, 0xFFF9A825, 0xFF6A1B9A,
        0xFF00838F, 0xFFEF6C00, 0xFF5D4037, 0xFF546E7A, 0xFFAD1457,
    )
}
