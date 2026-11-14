package com.rence.reminders.logic

import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.min

enum class TriggerKind { HEADS_UP, DUE, SNOOZE }

data class Trigger(val at: LocalDateTime, val occurrence: LocalDateTime, val kind: TriggerKind)

object Recurrence {

    fun later(a: LocalDateTime?, b: LocalDateTime?): LocalDateTime? = when {
        a == null -> b
        b == null -> a
        a.isAfter(b) -> a
        else -> b
    }

    /** Does [r] have an occurrence on [date]? */
    fun matches(r: Reminder, date: LocalDate): Boolean = matches(r.rule, r.startDate, date)

    fun matches(rule: RepeatRule, start: LocalDate, date: LocalDate): Boolean {
        if (date.isBefore(start)) return false
        val iv = rule.interval.coerceAtLeast(1).toLong()
        return when (rule.type) {
            RepeatType.NONE -> date == start
            RepeatType.DAILY -> ChronoUnit.DAYS.between(start, date) % iv == 0L
            RepeatType.WEEKLY -> {
                val days = rule.weekdays.ifEmpty { setOf(start.dayOfWeek) }
                date.dayOfWeek in days &&
                    ChronoUnit.WEEKS.between(start.with(DayOfWeek.MONDAY), date.with(DayOfWeek.MONDAY)) % iv == 0L
            }
            RepeatType.MONTHLY -> {
                val months = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(date))
                months % iv == 0L && monthDayMatches(rule, date)
            }
            RepeatType.YEARLY -> {
                val years = (date.year - start.year).toLong()
                years % iv == 0L && date.monthValue == rule.month &&
                    date.dayOfMonth == min(rule.dayOfMonth, date.lengthOfMonth())
            }
        }
    }

    private fun monthDayMatches(rule: RepeatRule, date: LocalDate): Boolean {
        val len = date.lengthOfMonth()
        return when (rule.monthlyMode) {
            MonthlyMode.DAY_OF_MONTH -> date.dayOfMonth == min(rule.dayOfMonth, len)
            MonthlyMode.LAST_DAY -> date.dayOfMonth == len
            MonthlyMode.NTH_WEEKDAY -> date.dayOfWeek == rule.nthWeekday && (
                if (rule.nth < 0) date.dayOfMonth + 7 > len else (date.dayOfMonth - 1) / 7 + 1 == rule.nth
                )
        }
    }

    /** First occurrence strictly after [after]; if [after] is null, the first occurrence at all. */
    fun nextOccurrence(r: Reminder, after: LocalDateTime?): LocalDateTime? {
        if (r.rule.type == RepeatType.NONE) {
            val dt = r.startDate.atTime(r.time)
            return if (after == null || dt.isAfter(after)) dt else null
        }
        var d = if (after == null || after.toLocalDate().isBefore(r.startDate)) r.startDate else after.toLocalDate()
        val limit = d.plusYears(r.rule.interval.coerceAtLeast(1).toLong() + 2)
        while (!d.isAfter(limit)) {
            if (matches(r, d)) {
                val dt = d.atTime(r.time)
                if (after == null || dt.isAfter(after)) return dt
            }
            d = d.plusDays(1)
        }
        return null
    }

    /** The next occurrence that is not marked done (may be in the past = overdue). Null = finished. */
    fun nextOpen(r: Reminder): LocalDateTime? = nextOccurrence(r, r.completedThrough)

    fun isDone(r: Reminder, occurrence: LocalDateTime): Boolean =
        r.completedThrough != null && !occurrence.isAfter(r.completedThrough)

    /** One-time reminder that has been completed. */
    fun isFinished(r: Reminder): Boolean = nextOpen(r) == null

    /** When should the next notification for [r] fire, after [now]? */
    fun nextTrigger(r: Reminder, now: LocalDateTime): Trigger? {
        val candidates = mutableListOf<Trigger>()
        val snooze = r.snoozeUntil
        val snoozeOcc = r.snoozeOccurrence
        if (snooze != null && snoozeOcc != null && snooze.isAfter(now) && !isDone(r, snoozeOcc)) {
            candidates += Trigger(snooze, snoozeOcc, TriggerKind.SNOOZE)
        }
        val lead = r.leadDays.coerceAtLeast(0).toLong()
        var after = later(r.completedThrough, now.minusDays(lead))
        for (i in 0 until 3) {
            val occ = nextOccurrence(r, after) ?: break
            if (lead > 0) {
                val h = occ.minusDays(lead)
                if (h.isAfter(now)) candidates += Trigger(h, occ, TriggerKind.HEADS_UP)
            }
            if (occ.isAfter(now)) candidates += Trigger(occ, occ, TriggerKind.DUE)
            after = occ
        }
        return candidates.minByOrNull { it.at }
    }

    /** Occurrence date-times of [r] that fall on [date]. */
    fun occurrenceOn(r: Reminder, date: LocalDate): LocalDateTime? =
        if (matches(r, date)) date.atTime(r.time) else null
}
