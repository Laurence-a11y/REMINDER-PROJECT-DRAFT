package com.rence.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.logic.Trigger
import com.rence.reminders.model.Reminder
import java.time.LocalDateTime
import java.time.ZoneId

/** One exact alarm per reminder: always the next thing that should ring. */
object Scheduler {
    const val ACTION_ALARM = "com.rence.reminders.ALARM"
    const val EXTRA_ID = "id"
    const val EXTRA_OCCURRENCE = "occurrence"
    const val EXTRA_KIND = "kind"
    const val EXTRA_AT = "at"

    private fun pending(ctx: Context, id: Long, t: Trigger?): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_ID, id)
            if (t != null) {
                putExtra(EXTRA_OCCURRENCE, t.occurrence.toString())
                putExtra(EXTRA_KIND, t.kind.name)
                putExtra(EXTRA_AT, t.at.toString())
            }
        }
        return PendingIntent.getBroadcast(
            ctx, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun canExact(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun schedule(ctx: Context, r: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val t = Recurrence.nextTrigger(r, now)
        if (t == null) {
            am.cancel(pending(ctx, r.id, null))
            return
        }
        val at = t.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pending(ctx, r.id, t)
        try {
            if (canExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(ctx: Context, id: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id, null))
    }
}
