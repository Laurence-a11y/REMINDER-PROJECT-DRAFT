package com.rence.reminders.data

import android.content.Context
import com.rence.reminders.alarm.Notifier
import com.rence.reminders.alarm.Scheduler
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.model.AppData
import com.rence.reminders.model.Category
import com.rence.reminders.model.Palette
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDateTime

/** All app data lives in one JSON file in the app's private storage. No internet, no account. */
class Store private constructor(private val ctx: Context) {
    private val file = File(ctx.filesDir, "data.json")
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun defaults() = AppData(
        categories = listOf(
            Category(1, "Personal", Palette.colors[1]),
            Category(2, "School", Palette.colors[4]),
            Category(3, "Bills", Palette.colors[0]),
            Category(4, "Work", Palette.colors[2]),
        ),
        nextId = 5,
    )

    private fun load(): AppData = try {
        if (file.exists()) JsonCodec.decode(file.readText()) else defaults()
    } catch (e: Exception) {
        defaults()
    }

    private fun save(d: AppData) {
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(JsonCodec.encode(d))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    @Synchronized
    private fun update(block: (AppData) -> AppData): AppData {
        val n = block(_data.value)
        _data.value = n
        save(n)
        return n
    }

    fun find(id: Long): Reminder? = _data.value.reminders.find { it.id == id }

    private fun replace(id: Long, block: (Reminder) -> Reminder): Reminder? {
        var out: Reminder? = null
        update { d ->
            d.copy(reminders = d.reminders.map { if (it.id == id) block(it).also { n -> out = n } else it })
        }
        return out
    }

    /** Insert (id == 0) or update. Returns the saved reminder. */
    fun saveReminder(r: Reminder): Reminder {
        var saved = r
        update { d ->
            if (r.id == 0L || d.reminders.none { it.id == r.id }) {
                saved = if (r.id == 0L) r.copy(id = d.nextId) else r
                d.copy(reminders = d.reminders + saved, nextId = maxOf(d.nextId, saved.id) + 1)
            } else {
                d.copy(reminders = d.reminders.map { if (it.id == r.id) r else it })
            }
        }
        Scheduler.schedule(ctx, saved)
        return saved
    }

    fun deleteReminder(id: Long) {
        update { d -> d.copy(reminders = d.reminders.filterNot { it.id == id }) }
        Scheduler.cancel(ctx, id)
        Notifier.cancel(ctx, id)
    }

    /** Mark one occurrence done. Returns the previous completedThrough (for Undo). */
    fun markDone(id: Long, occurrence: LocalDateTime): LocalDateTime? {
        val before = find(id)?.completedThrough
        val r = replace(id) { r ->
            val snoozeDone = r.snoozeOccurrence?.let { !it.isAfter(occurrence) } ?: false
            r.copy(
                completedThrough = Recurrence.later(r.completedThrough, occurrence),
                snoozeUntil = if (snoozeDone) null else r.snoozeUntil,
                snoozeOccurrence = if (snoozeDone) null else r.snoozeOccurrence,
            )
        }
        r?.let { Scheduler.schedule(ctx, it) }
        Notifier.cancel(ctx, id)
        return before
    }

    fun setCompletedThrough(id: Long, value: LocalDateTime?) {
        replace(id) { it.copy(completedThrough = value) }?.let { Scheduler.schedule(ctx, it) }
    }

    fun snooze(id: Long, occurrence: LocalDateTime, minutes: Long) {
        replace(id) { it.copy(snoozeUntil = LocalDateTime.now().plusMinutes(minutes), snoozeOccurrence = occurrence) }
            ?.let { Scheduler.schedule(ctx, it) }
        Notifier.cancel(ctx, id)
    }

    fun clearSnooze(id: Long): Reminder? = replace(id) { it.copy(snoozeUntil = null, snoozeOccurrence = null) }

    fun saveCategory(c: Category): Category {
        var saved = c
        update { d ->
            if (c.id == 0L) {
                saved = c.copy(id = d.nextId)
                d.copy(categories = d.categories + saved, nextId = d.nextId + 1)
            } else {
                d.copy(categories = d.categories.map { if (it.id == c.id) c else it })
            }
        }
        return saved
    }

    fun deleteCategory(id: Long) {
        update { d ->
            d.copy(
                categories = d.categories.filterNot { it.id == id },
                reminders = d.reminders.map { if (it.categoryId == id) it.copy(categoryId = null) else it },
            )
        }
    }

    fun updateSettings(block: (Settings) -> Settings) {
        update { d -> d.copy(settings = block(d.settings)) }
    }

    fun rescheduleAll() {
        _data.value.reminders.forEach { Scheduler.schedule(ctx, it) }
    }

    fun exportJson(): String = JsonCodec.encode(_data.value)

    /** Replaces everything with the backup. Throws if the file is not a valid backup. */
    fun importJson(text: String) {
        val incoming = JsonCodec.decode(text)
        _data.value.reminders.forEach { Scheduler.cancel(ctx, it.id) }
        update { incoming.copy(settings = incoming.settings.copy(onboarded = true)) }
        rescheduleAll()
    }

    companion object {
        @Volatile private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
