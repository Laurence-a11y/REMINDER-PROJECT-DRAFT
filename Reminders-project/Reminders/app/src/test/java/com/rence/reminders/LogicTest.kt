package com.rence.reminders

import com.rence.reminders.logic.Parser
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.logic.TriggerKind
import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LogicTest {
    // Saturday, Nov 14 2026
    private val today = LocalDate.of(2026, 11, 14)
    private fun p(s: String) = Parser.parse(s, today)

    @Test fun monthlyDay() {
        val r = p("Pay rent every 5th of the month at 9am")
        assertEquals("Pay rent", r.title)
        assertEquals(RepeatType.MONTHLY, r.rule.type)
        assertEquals(MonthlyMode.DAY_OF_MONTH, r.rule.monthlyMode)
        assertEquals(5, r.rule.dayOfMonth)
        assertEquals(LocalTime.of(9, 0), r.time)
    }

    @Test fun weeklyDays() {
        val r = p("every Monday and Thursday at 7pm gym")
        assertEquals("Gym", r.title)
        assertEquals(RepeatType.WEEKLY, r.rule.type)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), r.rule.weekdays)
        assertEquals(LocalTime.of(19, 0), r.time)
    }

    @Test fun yearlyWithDate() {
        val r = p("renew passport every year on March 3")
        assertEquals("Renew passport", r.title)
        assertEquals(RepeatType.YEARLY, r.rule.type)
        assertEquals(3, r.rule.month)
        assertEquals(3, r.rule.dayOfMonth)
        assertEquals(LocalDate.of(2027, 3, 3), r.date)
    }

    @Test fun lastWeekdayOfMonth() {
        val r = p("last Friday of every month submit report")
        assertEquals("Submit report", r.title)
        assertEquals(MonthlyMode.NTH_WEEKDAY, r.rule.monthlyMode)
        assertEquals(-1, r.rule.nth)
        assertEquals(DayOfWeek.FRIDAY, r.rule.nthWeekday)
    }

    @Test fun everyTwoWeeks() {
        val r = p("every 2 weeks water plants")
        assertEquals("Water plants", r.title)
        assertEquals(RepeatType.WEEKLY, r.rule.type)
        assertEquals(2, r.rule.interval)
        assertEquals(setOf(DayOfWeek.SATURDAY), r.rule.weekdays)
    }

    @Test fun tomorrowOneTime() {
        val r = p("tomorrow 8am call Mom")
        assertEquals("Call Mom", r.title)
        assertEquals(RepeatType.NONE, r.rule.type)
        assertEquals(today.plusDays(1), r.date)
        assertEquals(LocalTime.of(8, 0), r.time)
    }

    @Test fun headsUpAndDate() {
        val r = p("dentist appointment on Dec 5 at 3:30pm 2 days before")
        assertEquals("Dentist appointment", r.title)
        assertEquals(LocalDate.of(2026, 12, 5), r.date)
        assertEquals(LocalTime.of(15, 30), r.time)
        assertEquals(2, r.leadDays)
    }

    @Test fun everyMorning() {
        val r = p("take vitamins every morning")
        assertEquals("Take vitamins", r.title)
        assertEquals(RepeatType.DAILY, r.rule.type)
        assertEquals(LocalTime.of(9, 0), r.time)
    }

    @Test fun lastDay() {
        val r = p("pay bills on the last day of the month")
        assertEquals("Pay bills", r.title)
        assertEquals(MonthlyMode.LAST_DAY, r.rule.monthlyMode)
    }

    @Test fun everyOther() {
        val r = p("team meeting every other wednesday at 10am")
        assertEquals("Team meeting", r.title)
        assertEquals(2, r.rule.interval)
        assertEquals(setOf(DayOfWeek.WEDNESDAY), r.rule.weekdays)
    }

    @Test fun yearlySuffix() {
        val r = p("mom's birthday march 3 yearly")
        assertEquals("Mom's birthday", r.title)
        assertEquals(RepeatType.YEARLY, r.rule.type)
        assertEquals(3, r.rule.month)
    }

    @Test fun bareAt() {
        val r = p("check email daily at 8")
        assertEquals("Check email", r.title)
        assertEquals(RepeatType.DAILY, r.rule.type)
        assertEquals(LocalTime.of(8, 0), r.time)
    }

    @Test fun weekdayDate() {
        val r = p("submit essay friday 11:59pm")
        assertEquals("Submit essay", r.title)
        assertEquals(LocalDate.of(2026, 11, 20), r.date)
        assertEquals(LocalTime.of(23, 59), r.time)
    }

    @Test fun remindMePrefix() {
        val r = p("remind me to buy milk in 3 days")
        assertEquals("Buy milk", r.title)
        assertEquals(today.plusDays(3), r.date)
    }

    @Test fun secondTuesday() {
        val r = p("org meeting every 2nd tuesday 4pm")
        assertEquals("Org meeting", r.title)
        assertEquals(MonthlyMode.NTH_WEEKDAY, r.rule.monthlyMode)
        assertEquals(2, r.rule.nth)
        assertEquals(DayOfWeek.TUESDAY, r.rule.nthWeekday)
        assertEquals(LocalTime.of(16, 0), r.time)
    }

    @Test fun monthlyKeywordOnThe() {
        val r = p("electric bill monthly on the 20th")
        assertEquals("Electric bill", r.title)
        assertEquals(20, r.rule.dayOfMonth)
    }

    @Test fun weekdaysKeyword() {
        val r = p("stand-up every weekday at 9:15 am")
        assertEquals("Stand-up", r.title)
        assertEquals(5, r.rule.weekdays.size)
        assertEquals(LocalTime.of(9, 15), r.time)
    }

    @Test fun numericDateWeekBefore() {
        val r = p("insurance renewal 1/15 a week before")
        assertEquals("Insurance renewal", r.title)
        assertEquals(LocalDate.of(2027, 1, 15), r.date)
        assertEquals(7, r.leadDays)
    }

    @Test fun plainTitle() {
        val r = p("Buy groceries")
        assertEquals("Buy groceries", r.title)
        assertEquals(RepeatType.NONE, r.rule.type)
        assertNull(r.date)
        assertNull(r.time)
    }

    // ---- Recurrence ----

    private fun rem(rule: RepeatRule, start: LocalDate = today, time: LocalTime = LocalTime.of(9, 0), lead: Int = 0, done: LocalDateTime? = null) =
        Reminder(id = 1, title = "t", startDate = start, time = time, rule = rule, leadDays = lead, completedThrough = done)

    @Test fun monthlyClampsTo30() {
        val r = rem(RepeatRule(RepeatType.MONTHLY, dayOfMonth = 31))
        val n = Recurrence.nextOccurrence(r, LocalDateTime.of(2026, 11, 14, 10, 0))
        assertEquals(LocalDateTime.of(2026, 11, 30, 9, 0), n)
    }

    @Test fun lastFriday() {
        val r = rem(RepeatRule(RepeatType.MONTHLY, monthlyMode = MonthlyMode.NTH_WEEKDAY, nth = -1, nthWeekday = DayOfWeek.FRIDAY))
        assertEquals(LocalDateTime.of(2026, 11, 27, 9, 0), Recurrence.nextOccurrence(r, null))
    }

    @Test fun biweekly() {
        val r = rem(RepeatRule(RepeatType.WEEKLY, interval = 2, weekdays = setOf(DayOfWeek.SATURDAY)))
        val first = Recurrence.nextOccurrence(r, null)!!
        val second = Recurrence.nextOccurrence(r, first)!!
        assertEquals(today.atTime(9, 0), first)
        assertEquals(today.plusWeeks(2).atTime(9, 0), second)
    }

    @Test fun headsUpTrigger() {
        val r = rem(RepeatRule(), start = LocalDate.of(2026, 12, 5), lead = 2)
        val t = Recurrence.nextTrigger(r, LocalDateTime.of(2026, 11, 14, 10, 0))!!
        assertEquals(TriggerKind.HEADS_UP, t.kind)
        assertEquals(LocalDateTime.of(2026, 12, 3, 9, 0), t.at)
        val t2 = Recurrence.nextTrigger(r, t.at)!!
        assertEquals(TriggerKind.DUE, t2.kind)
        assertNull(Recurrence.nextTrigger(r, t2.at))
    }

    @Test fun overdueRecurringStillSchedulesNext() {
        val r = rem(RepeatRule(RepeatType.DAILY))
        val now = LocalDateTime.of(2026, 11, 16, 12, 0)
        val t = Recurrence.nextTrigger(r, now)!!
        assertEquals(LocalDateTime.of(2026, 11, 17, 9, 0), t.at)
        assertEquals(today.atTime(9, 0), Recurrence.nextOpen(r))
    }

    @Test fun leapYearBirthday() {
        val r = rem(RepeatRule(RepeatType.YEARLY, month = 2, dayOfMonth = 29), start = LocalDate.of(2027, 2, 1))
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0), Recurrence.nextOccurrence(r, null))
        assertTrue(Recurrence.matches(r, LocalDate.of(2028, 2, 29)))
    }
}
