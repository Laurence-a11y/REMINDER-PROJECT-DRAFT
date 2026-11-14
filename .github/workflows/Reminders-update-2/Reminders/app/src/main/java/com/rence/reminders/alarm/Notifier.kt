package com.rence.reminders.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rence.reminders.MainActivity
import com.rence.reminders.R
import com.rence.reminders.logic.Format
import com.rence.reminders.logic.TriggerKind
import com.rence.reminders.model.Reminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object Notifier {
    const val CHANNEL = "reminders"
    const val ACTION_DONE = "com.rence.reminders.DONE"
    const val ACTION_SNOOZE = "com.rence.reminders.SNOOZE"
    const val EXTRA_MINUTES = "minutes"

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Your reminders and heads-ups"
            enableVibration(true)
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun actionIntent(ctx: Context, r: Reminder, occ: LocalDateTime, act: String, minutes: Long, code: Int): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).apply {
            setAction(act)
            putExtra(Scheduler.EXTRA_ID, r.id)
            putExtra(Scheduler.EXTRA_OCCURRENCE, occ.toString())
            putExtra(EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            ctx, (r.id * 10 + code).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, r: Reminder, occ: LocalDateTime, kind: TriggerKind, categoryName: String?) {
        if (!hasPermission(ctx)) return
        val text = when (kind) {
            TriggerKind.HEADS_UP -> {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), occ.toLocalDate())
                val whenText = if (days <= 1L) "tomorrow" else "in $days days"
                "Coming up $whenText · ${Format.relative(occ)}"
            }
            else -> "Due ${Format.relative(occ)}"
        }
        val big = if (r.notes.isBlank()) text else "$text\n${r.notes}"
        val open = PendingIntent.getActivity(
            ctx, r.id.toInt(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setSubText(categoryName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Done", actionIntent(ctx, r, occ, ACTION_DONE, 0, 1))
            .addAction(0, "Snooze 10 min", actionIntent(ctx, r, occ, ACTION_SNOOZE, 10, 2))
            .addAction(0, "Snooze 1 hr", actionIntent(ctx, r, occ, ACTION_SNOOZE, 60, 3))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(r.id.toInt(), n)
        } catch (e: SecurityException) {
            // Permission revoked; nothing to do.
        }
    }

    @SuppressLint("MissingPermission")
    fun showTest(ctx: Context) {
        if (!hasPermission(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test reminder")
            .setContentText("Notifications are working.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(999_999, n)
        } catch (e: SecurityException) {
        }
    }

    fun cancel(ctx: Context, id: Long) {
        NotificationManagerCompat.from(ctx).cancel(id.toInt())
    }
}
