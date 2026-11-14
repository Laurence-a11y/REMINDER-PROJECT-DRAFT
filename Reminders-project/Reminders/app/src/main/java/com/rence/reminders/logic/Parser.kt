package com.rence.reminders.logic

import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

data class ParseResult(
    val title: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val rule: RepeatRule,
    val leadDays: Int,
)

/**
 * Offline, rule-based English parser.
 * "pay rent every 5th of the month at 9am 3 days before" ->
 *   title "Pay rent", monthly on day 5, 9:00, heads-up 3 days before.
 */
object Parser {
    private const val MONTH =
        "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sept?(?:ember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
    private const val DAY =
        "(?:mon(?:day)?|tue(?:s(?:day)?)?|wed(?:nesday)?|thu(?:r(?:s(?:day)?)?)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)"
    private const val NUMS = "\\d+|an|a|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve"
    private const val ORD = "(first|1st|second|2nd|third|3rd|fourth|4th|last)"
    private const val SUF = "(?:st|nd|rd|th)"
    private const val DAYLIST = "($DAY(?:s)?\\b(?:\\s*(?:,\\s*and|,|and|&|/)\\s*$DAY(?:s)?\\b)*)"
    private const val YEARLY = "(?:every\\s+year|each\\s+year|yearly|annually)"

    private val WORDS = mapOf(
        "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
    )

    private fun rx(p: String) = Regex(p, RegexOption.IGNORE_CASE)

    private fun num(s: String): Int = s.toIntOrNull() ?: WORDS[s.lowercase()] ?: 1

    private fun ordinal(s: String): Int = when (s.lowercase()) {
        "first", "1st" -> 1
        "second", "2nd" -> 2
        "third", "3rd" -> 3
        "fourth", "4th" -> 4
        else -> -1
    }

    fun dayOf(s: String): DayOfWeek = when (s.lowercase().take(3)) {
        "mon" -> DayOfWeek.MONDAY
        "tue" -> DayOfWeek.TUESDAY
        "wed" -> DayOfWeek.WEDNESDAY
        "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY
        "sat" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun monthOf(s: String): Int = when (s.lowercase().take(3)) {
        "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
        "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; else -> 12
    }

    private fun days(list: String): Set<DayOfWeek> =
        rx(DAY).findAll(list).map { dayOf(it.value) }.toSet()

    private fun validMonthDay(month: Int, day: Int): Boolean =
        month in 1..12 && day >= 1 && day <= Month.of(month).maxLength()

    /** Next date (today or later) with that month/day, or in [year] if given. */
    fun resolveMonthDay(month: Int, day: Int, year: Int?, today: LocalDate): LocalDate? {
        if (!validMonthDay(month, day)) return null
        return try {
            if (year != null) {
                val y = if (year < 100) 2000 + year else year
                val ym = java.time.YearMonth.of(y, month)
                LocalDate.of(y, month, minOf(day, ym.lengthOfMonth()))
            } else {
                var y = today.year
                var d = LocalDate.of(y, month, minOf(day, java.time.YearMonth.of(y, month).lengthOfMonth()))
                if (d.isBefore(today)) {
                    y += 1
                    d = LocalDate.of(y, month, minOf(day, java.time.YearMonth.of(y, month).lengthOfMonth()))
                }
                d
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun partOfDay(s: String): LocalTime = when (s.lowercase()) {
        "morning" -> LocalTime.of(9, 0)
        "afternoon" -> LocalTime.of(15, 0)
        "evening" -> LocalTime.of(18, 0)
        else -> LocalTime.of(20, 0) // night / tonight
    }

    fun parse(input: String, today: LocalDate = LocalDate.now()): ParseResult {
        var s = " " + input.trim() + " "

        fun take(re: Regex, accept: (MatchResult) -> Boolean = { true }): MatchResult? {
            for (m in re.findAll(s)) {
                if (accept(m)) {
                    s = s.substring(0, m.range.first) + " " + s.substring(m.range.last + 1)
                    return m
                }
            }
            return null
        }

        var type = RepeatType.NONE
        var interval = 1
        var weekdays = emptySet<DayOfWeek>()
        var mode = MonthlyMode.DAY_OF_MONTH
        var dom = 0
        var nth = 1
        var nthDay = DayOfWeek.MONDAY
        var month = 0
        var date: LocalDate? = null
        var time: LocalTime? = null
        var lead = 0

        // ---- Heads-up ("3 days before", "a week early", "the day before") ----
        take(rx("\\b(?:and\\s+)?(?:remind\\s+me\\s+|notify\\s+me\\s+|alert\\s+me\\s+)?($NUMS)\\s+(day|week)s?\\s+(?:before(?:hand)?|early|earlier|ahead|in\\s+advance|prior)\\b"))?.let {
            lead = num(it.groupValues[1]) * (if (it.groupValues[2].lowercase() == "week") 7 else 1)
        }
        if (lead == 0) {
            take(rx("\\b(?:and\\s+)?(?:remind\\s+me\\s+)?(?:the\\s+)?day\\s+before\\b"))?.let { lead = 1 }
        }

        // ---- Repeat ----
        take(rx("\\b(?:every|each)\\s+(morning|afternoon|evening|night)\\b"))?.let {
            type = RepeatType.DAILY
            time = partOfDay(it.groupValues[1])
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:on\\s+)?(?:every\\s+|each\\s+|the\\s+)?last\\s+day\\s+of\\s+(?:the\\s+|every\\s+|each\\s+)?month\\b|\\b(?:every|each)\\s+end\\s+of\\s+(?:the\\s+)?month\\b|\\bmonth[\\s-]?end\\b"))?.let {
                type = RepeatType.MONTHLY
                mode = MonthlyMode.LAST_DAY
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:on\\s+)?(every\\s+|each\\s+|the\\s+)?$ORD\\s+($DAY)s?\\b(\\s+of\\s+(?:the\\s+|every\\s+|each\\s+)?month\\b|\\s+monthly\\b|\\s+(?:of\\s+)?(?:every|each)\\s+month\\b)?")) { m ->
                val g1 = m.groupValues[1].trim().lowercase()
                g1 == "every" || g1 == "each" || m.groupValues[4].isNotBlank()
            }?.let {
                type = RepeatType.MONTHLY
                mode = MonthlyMode.NTH_WEEKDAY
                nth = ordinal(it.groupValues[2])
                nthDay = dayOf(it.groupValues[3])
            }
        }
        if (type == RepeatType.NONE) {
            val pats = listOf(
                "\\b(?:on\\s+)?(?:every|each)\\s+(\\d{1,2})$SUF(?:\\s+(?:day\\s+)?of\\s+(?:the|every|each)\\s+month)?\\b",
                "\\b(?:every|each)\\s+month\\s+on\\s+the\\s+(\\d{1,2})$SUF?\\b",
                "\\b(?:on\\s+)?the\\s+(\\d{1,2})$SUF?\\s+(?:day\\s+)?of\\s+(?:every|each|the)\\s+month\\b",
                "\\bmonthly\\s+on\\s+the\\s+(\\d{1,2})$SUF?\\b",
            )
            for (p in pats) {
                val m = take(rx(p)) { it.groupValues[1].toInt() in 1..31 } ?: continue
                type = RepeatType.MONTHLY
                mode = MonthlyMode.DAY_OF_MONTH
                dom = m.groupValues[1].toInt()
                break
            }
        }
        if (type == RepeatType.NONE) {
            val pats = listOf(
                "\\b$YEARLY\\s+(?:on\\s+)?$MONTH\\.?\\s+(\\d{1,2})$SUF?\\b" to true,
                "\\b$YEARLY\\s+(?:on\\s+)?(?:the\\s+)?(\\d{1,2})$SUF?\\s+(?:of\\s+)?$MONTH\\b" to false,
                "\\b(?:every|each)\\s+$MONTH\\.?\\s+(\\d{1,2})$SUF?\\b" to true,
                "\\b(?:every|each)\\s+(?:the\\s+)?(\\d{1,2})$SUF?\\s+(?:of\\s+)?$MONTH\\b" to false,
                "\\b(?:on\\s+)?$MONTH\\.?\\s+(\\d{1,2})$SUF?\\s+$YEARLY\\b" to true,
                "\\b(?:on\\s+)?(?:the\\s+)?(\\d{1,2})$SUF?\\s+(?:of\\s+)?$MONTH\\s+$YEARLY\\b" to false,
            )
            fun md(m: MatchResult, monthFirst: Boolean): Pair<Int, Int> =
                if (monthFirst) monthOf(m.groupValues[1]) to m.groupValues[2].toInt()
                else monthOf(m.groupValues[2]) to m.groupValues[1].toInt()
            for ((p, monthFirst) in pats) {
                val m = take(rx(p)) { val (mo, d) = md(it, monthFirst); validMonthDay(mo, d) } ?: continue
                val (mo, d) = md(m, monthFirst)
                type = RepeatType.YEARLY
                month = mo
                dom = d
                break
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:every|each|on)\\s+weekdays?\\b|\\bweekdays\\b"))?.let {
                type = RepeatType.WEEKLY
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:every|each|on)\\s+weekends?\\b|\\bweekends\\b"))?.let {
                type = RepeatType.WEEKLY
                weekdays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:every|each)\\s+(other\\s+)?$DAYLIST"))?.let {
                type = RepeatType.WEEKLY
                if (it.groupValues[1].isNotBlank()) interval = 2
                weekdays = days(it.groupValues[2])
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\bweekly\\s+on\\s+$DAYLIST"))?.let {
                type = RepeatType.WEEKLY
                weekdays = days(it.groupValues[1])
            }
        }
        if (type == RepeatType.NONE) {
            val plural = "(?:mon|tues|wednes|thurs|fri|satur|sun)days\\b"
            take(rx("\\b(?:on\\s+)?($plural(?:\\s*(?:,\\s*and|,|and|&)\\s*$plural)*)"))?.let {
                type = RepeatType.WEEKLY
                weekdays = days(it.groupValues[1])
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:every|each)\\s+(other|$NUMS)\\s+(day|week|month|year)s?\\b"))?.let {
                interval = if (it.groupValues[1].lowercase() == "other") 2 else num(it.groupValues[1]).coerceAtLeast(1)
                type = unitType(it.groupValues[2])
            }
        }
        if (type == RepeatType.NONE) {
            take(rx("\\b(?:every|each)\\s+(day|week|month|year)\\b|\\b(daily|everyday|weekly|monthly|yearly|annually|biweekly|bi-weekly|fortnightly)\\b"))?.let {
                val w = (it.groupValues[1] + it.groupValues[2]).lowercase()
                when (w) {
                    "biweekly", "bi-weekly", "fortnightly" -> { type = RepeatType.WEEKLY; interval = 2 }
                    else -> type = unitType(w)
                }
            }
        }

        // ---- Refine repeat with extra details ----
        if (type == RepeatType.WEEKLY && weekdays.isEmpty()) {
            take(rx("\\b(?:(on)\\s+)?$DAYLIST")) { m -> m.groupValues[1].isNotEmpty() || m.groupValues[2].length >= 6 }?.let {
                weekdays = days(it.groupValues[2])
            }
        }
        if (type == RepeatType.MONTHLY && mode == MonthlyMode.DAY_OF_MONTH && dom == 0) {
            val m = take(rx("\\b(?:on\\s+)?(?:the\\s+)?$ORD\\s+($DAY)s?\\b"))
            if (m != null) {
                mode = MonthlyMode.NTH_WEEKDAY
                nth = ordinal(m.groupValues[1])
                nthDay = dayOf(m.groupValues[2])
            } else {
                take(rx("\\b(?:on\\s+)?(?:the\\s+)?(\\d{1,2})$SUF\\b|\\bon\\s+the\\s+(\\d{1,2})\\b")) {
                    (it.groupValues[1] + it.groupValues[2]).toInt() in 1..31
                }?.let { dom = (it.groupValues[1] + it.groupValues[2]).toInt() }
            }
        }

        // ---- Time ----
        take(rx("(?<![\\w:/])(?:at\\s+|@\\s*|by\\s+)?(\\d{1,2})(?:[:.](\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)(?=[\\s,.!?;)]|$)")) {
            val h = it.groupValues[1].toInt()
            val mi = it.groupValues[2].ifEmpty { "0" }.toInt()
            h in 1..12 && mi in 0..59
        }?.let {
            var h = it.groupValues[1].toInt() % 12
            if (it.groupValues[3].lowercase().startsWith("p")) h += 12
            time = LocalTime.of(h, it.groupValues[2].ifEmpty { "0" }.toInt())
        } ?: take(rx("(?<![\\w:/])(?:at\\s+|@\\s*|by\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)\\b"))?.let {
            time = LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        } ?: take(rx("\\b(?:at\\s+|by\\s+)?(noon|midday|midnight)\\b"))?.let {
            time = if (it.groupValues[1].lowercase() == "midnight") LocalTime.MIDNIGHT else LocalTime.NOON
        } ?: take(rx("\\b(?:at|@)\\s*(\\d{1,2})\\b(?!\\s*(?:[:/]|days?\\b|weeks?\\b|months?\\b|years?\\b))")) {
            it.groupValues[1].toInt() in 0..23
        }?.let {
            val h = it.groupValues[1].toInt()
            time = LocalTime.of(if (h in 1..6) h + 12 else h, 0)
        }
        if (time == null) {
            take(rx("\\b(?:in\\s+the\\s+|this\\s+)?(morning|afternoon|evening)\\b"))?.let { time = partOfDay(it.groupValues[1]) }
        }

        // ---- Date ----
        take(rx("\\b(?:on\\s+)?(?:the\\s+)?day\\s+after\\s+(?:tomorrow|tmrw|tmr)\\b"))?.let { date = today.plusDays(2) }
        if (date == null) take(rx("\\b(tomorrow|tomorow|tmrw|tmr)\\b"))?.let { date = today.plusDays(1) }
        if (date == null) take(rx("\\b(today|tonight|tdy)\\b"))?.let {
            date = today
            if (it.groupValues[1].lowercase() == "tonight" && time == null) time = LocalTime.of(20, 0)
        }
        if (date == null) take(rx("\\bin\\s+($NUMS)\\s+(day|week|month|year)s?\\b"))?.let {
            val n = num(it.groupValues[1]).toLong()
            date = when (it.groupValues[2].lowercase()) {
                "day" -> today.plusDays(n)
                "week" -> today.plusWeeks(n)
                "month" -> today.plusMonths(n)
                else -> today.plusYears(n)
            }
        }
        if (date == null) take(rx("\\bnext\\s+(week|month|year)\\b"))?.let {
            date = when (it.groupValues[1].lowercase()) {
                "week" -> today.plusWeeks(1)
                "month" -> today.plusMonths(1)
                else -> today.plusYears(1)
            }
        }
        if (date == null) take(rx("\\b(?:on\\s+)?$MONTH\\.?\\s+(\\d{1,2})$SUF?\\b(?:,?\\s+(\\d{4})\\b)?")) {
            resolveMonthDay(monthOf(it.groupValues[1]), it.groupValues[2].toInt(), it.groupValues[3].toIntOrNull(), today) != null
        }?.let {
            date = resolveMonthDay(monthOf(it.groupValues[1]), it.groupValues[2].toInt(), it.groupValues[3].toIntOrNull(), today)
        }
        if (date == null) take(rx("\\b(?:on\\s+)?(?:the\\s+)?(\\d{1,2})$SUF?\\s+(?:of\\s+)?$MONTH\\b(?:,?\\s+(\\d{4})\\b)?")) {
            resolveMonthDay(monthOf(it.groupValues[2]), it.groupValues[1].toInt(), it.groupValues[3].toIntOrNull(), today) != null
        }?.let {
            date = resolveMonthDay(monthOf(it.groupValues[2]), it.groupValues[1].toInt(), it.groupValues[3].toIntOrNull(), today)
        }
        if (date == null) take(rx("\\b(?:on\\s+)?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b")) {
            resolveMonthDay(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toIntOrNull(), today) != null
        }?.let {
            date = resolveMonthDay(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toIntOrNull(), today)
        }
        if (date == null) take(rx("\\b(?:(on|next|this|coming)\\s+)?($DAY)\\b")) { m ->
            m.groupValues[1].isNotEmpty() || m.groupValues[2].length >= 6
        }?.let {
            val dow = dayOf(it.groupValues[2])
            date = if (it.groupValues[1].lowercase() == "next") {
                today.with(TemporalAdjusters.next(dow))
            } else {
                today.with(TemporalAdjusters.nextOrSame(dow))
            }
        }
        if (date == null) take(rx("\\b(?:on\\s+)?the\\s+(\\d{1,2})$SUF\\b|\\bon\\s+(?:the\\s+)?(\\d{1,2})$SUF\\b")) {
            (it.groupValues[1] + it.groupValues[2]).toInt() in 1..31
        }?.let {
            val d = (it.groupValues[1] + it.groupValues[2]).toInt()
            var ym = java.time.YearMonth.from(today)
            if (d < today.dayOfMonth) ym = ym.plusMonths(1)
            date = ym.atDay(minOf(d, ym.lengthOfMonth()))
            if (type == RepeatType.MONTHLY && mode == MonthlyMode.DAY_OF_MONTH && dom == 0) dom = d
        }

        // ---- Finalize repeat details ----
        when (type) {
            RepeatType.MONTHLY -> if (mode == MonthlyMode.DAY_OF_MONTH && dom == 0) dom = (date ?: today).dayOfMonth
            RepeatType.YEARLY -> if (month == 0) {
                val base = date ?: today
                month = base.monthValue
                dom = base.dayOfMonth
            } else if (date == null) {
                date = resolveMonthDay(month, dom, null, today)
            }
            RepeatType.WEEKLY -> if (weekdays.isEmpty()) weekdays = setOf((date ?: today).dayOfWeek)
            else -> {}
        }

        val rule = RepeatRule(
            type = type,
            interval = interval,
            weekdays = if (type == RepeatType.WEEKLY) weekdays else emptySet(),
            monthlyMode = mode,
            dayOfMonth = if (dom == 0) (date ?: today).dayOfMonth else dom,
            nth = nth,
            nthWeekday = nthDay,
            month = if (month == 0) (date ?: today).monthValue else month,
        )
        return ParseResult(cleanTitle(s), date, time, rule, lead)
    }

    private fun unitType(w: String): RepeatType = when (w.lowercase()) {
        "day", "daily", "everyday" -> RepeatType.DAILY
        "week", "weekly" -> RepeatType.WEEKLY
        "month", "monthly" -> RepeatType.MONTHLY
        else -> RepeatType.YEARLY
    }

    private val LEADING = rx("^(?:remind\\s+me\\s+(?:to|about|of)?|reminder\\s*(?:to|:|-)?|don'?t\\s+forget\\s+(?:to)?|please|to|that|[,;:\\-–])\\s*")
    private val TRAILING = rx("\\s*(?:\\b(?:on|at|by|every|each|in|the|and|for|of|from|starting|this|next)|[,;:\\-–@&])$")

    fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim()
        while (true) {
            val n = t.replace(LEADING, "").replace(TRAILING, "").replace(Regex("\\s+,"), ",").trim()
            if (n == t) break
            t = n
        }
        return t.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
