package com.rence.reminders.data

import com.rence.reminders.model.AppData
import com.rence.reminders.model.Category
import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import com.rence.reminders.model.Settings
import com.rence.reminders.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object JsonCodec {
    private const val VERSION = 1

    private fun JSONObject.str(k: String): String? = if (!has(k) || isNull(k)) null else getString(k)
    private fun JSONObject.lng(k: String): Long? = if (!has(k) || isNull(k)) null else getLong(k)
    private fun Any?.orNull(): Any = this ?: JSONObject.NULL

    fun encode(d: AppData): String = JSONObject().apply {
        put("version", VERSION)
        put("nextId", d.nextId)
        put("settings", JSONObject().apply {
            put("theme", d.settings.theme.name)
            put("defaultTime", d.settings.defaultTime.toString())
            put("onboarded", d.settings.onboarded)
        })
        put("categories", JSONArray().apply {
            d.categories.forEach { c ->
                put(JSONObject().apply { put("id", c.id); put("name", c.name); put("color", c.color) })
            }
        })
        put("reminders", JSONArray().apply { d.reminders.forEach { put(encodeReminder(it)) } })
    }.toString(2)

    private fun encodeReminder(r: Reminder) = JSONObject().apply {
        put("id", r.id)
        put("title", r.title)
        put("notes", r.notes)
        put("categoryId", r.categoryId.orNull())
        put("startDate", r.startDate.toString())
        put("time", r.time.toString())
        put("leadDays", r.leadDays)
        put("completedThrough", r.completedThrough?.toString().orNull())
        put("snoozeUntil", r.snoozeUntil?.toString().orNull())
        put("snoozeOccurrence", r.snoozeOccurrence?.toString().orNull())
        put("createdAt", r.createdAt)
        put("rule", JSONObject().apply {
            put("type", r.rule.type.name)
            put("interval", r.rule.interval)
            put("weekdays", JSONArray().apply { r.rule.weekdays.sorted().forEach { put(it.name) } })
            put("monthlyMode", r.rule.monthlyMode.name)
            put("dayOfMonth", r.rule.dayOfMonth)
            put("nth", r.rule.nth)
            put("nthWeekday", r.rule.nthWeekday.name)
            put("month", r.rule.month)
        })
    }

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        val s = o.optJSONObject("settings") ?: JSONObject()
        val settings = Settings(
            theme = runCatching { ThemeMode.valueOf(s.optString("theme", "SYSTEM")) }.getOrDefault(ThemeMode.SYSTEM),
            defaultTime = runCatching { LocalTime.parse(s.optString("defaultTime", "09:00")) }.getOrDefault(LocalTime.of(9, 0)),
            onboarded = s.optBoolean("onboarded", false),
        )
        val cats = mutableListOf<Category>()
        o.optJSONArray("categories")?.let { a ->
            for (i in 0 until a.length()) {
                val c = a.getJSONObject(i)
                cats += Category(c.getLong("id"), c.getString("name"), c.getLong("color"))
            }
        }
        val rems = mutableListOf<Reminder>()
        o.optJSONArray("reminders")?.let { a ->
            for (i in 0 until a.length()) {
                runCatching { decodeReminder(a.getJSONObject(i)) }.getOrNull()?.let { rems += it }
            }
        }
        val maxId = (cats.map { it.id } + rems.map { it.id }).maxOrNull() ?: 0
        return AppData(cats, rems, settings, maxOf(o.optLong("nextId", 1), maxId + 1))
    }

    private fun decodeReminder(o: JSONObject): Reminder {
        val ru = o.getJSONObject("rule")
        val wd = mutableSetOf<DayOfWeek>()
        ru.optJSONArray("weekdays")?.let { a -> for (i in 0 until a.length()) wd += DayOfWeek.valueOf(a.getString(i)) }
        val rule = RepeatRule(
            type = RepeatType.valueOf(ru.getString("type")),
            interval = ru.optInt("interval", 1),
            weekdays = wd,
            monthlyMode = MonthlyMode.valueOf(ru.optString("monthlyMode", "DAY_OF_MONTH")),
            dayOfMonth = ru.optInt("dayOfMonth", 1),
            nth = ru.optInt("nth", 1),
            nthWeekday = DayOfWeek.valueOf(ru.optString("nthWeekday", "MONDAY")),
            month = ru.optInt("month", 1),
        )
        return Reminder(
            id = o.getLong("id"),
            title = o.getString("title"),
            notes = o.optString("notes", ""),
            categoryId = o.lng("categoryId"),
            startDate = LocalDate.parse(o.getString("startDate")),
            time = LocalTime.parse(o.getString("time")),
            rule = rule,
            leadDays = o.optInt("leadDays", 0),
            completedThrough = o.str("completedThrough")?.let(LocalDateTime::parse),
            snoozeUntil = o.str("snoozeUntil")?.let(LocalDateTime::parse),
            snoozeOccurrence = o.str("snoozeOccurrence")?.let(LocalDateTime::parse),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        )
    }
}
