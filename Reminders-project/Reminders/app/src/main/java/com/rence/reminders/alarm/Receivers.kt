package com.rence.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rence.reminders.data.Store
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.logic.TriggerKind
import java.time.LocalDateTime

/** Fires when a reminder's alarm goes off: shows the notification and schedules the next one. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = Store.get(ctx)
        val id = intent.getLongExtra(Scheduler.EXTRA_ID, -1)
        val occ = intent.getStringExtra(Scheduler.EXTRA_OCCURRENCE)?.let(LocalDateTime::parse) ?: return
        val kind = intent.getStringExtra(Scheduler.EXTRA_KIND)
            ?.let { runCatching { TriggerKind.valueOf(it) }.getOrNull() } ?: TriggerKind.DUE
        val at = intent.getStringExtra(Scheduler.EXTRA_AT)?.let(LocalDateTime::parse) ?: occ
        val now = Recurrence.later(LocalDateTime.now(), at)!!

        var r = store.find(id) ?: return
        if (kind == TriggerKind.SNOOZE) r = store.clearSnooze(id) ?: r

        if (!Recurrence.isDone(r, occ)) {
            val cat = r.categoryId?.let { cid -> store.data.value.categories.find { it.id == cid }?.name }
            Notifier.show(ctx, r, occ, kind, cat)
        }
        Scheduler.schedule(ctx, r, now)
    }
}

/** Handles the Done / Snooze buttons on a notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = Store.get(ctx)
        val id = intent.getLongExtra(Scheduler.EXTRA_ID, -1)
        val occ = intent.getStringExtra(Scheduler.EXTRA_OCCURRENCE)?.let(LocalDateTime::parse) ?: return
        when (intent.action) {
            Notifier.ACTION_DONE -> store.markDone(id, occ)
            Notifier.ACTION_SNOOZE -> store.snooze(id, occ, intent.getLongExtra(Notifier.EXTRA_MINUTES, 10))
        }
        Notifier.cancel(ctx, id)
    }
}

/** Alarms are wiped on restart / time change, so put them all back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.get(ctx).rescheduleAll()
    }
}
