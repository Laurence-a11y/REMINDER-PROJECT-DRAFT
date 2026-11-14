@file:OptIn(ExperimentalMaterial3Api::class)

package com.rence.reminders.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val sections = listOf(
    "Quick add: just type" to listOf(
        "Type one line in the box at the top of the home screen. A preview shows what the app understood. Tap Save, or Edit details to check it in the form first.",
    ),
    "Things you can type" to listOf(
        "pay rent every 5th at 9am",
        "gym every monday and thursday 7pm",
        "team meeting every other wednesday 10am",
        "submit report last friday of every month",
        "org meeting every 2nd tuesday 4pm",
        "pay bills on the last day of the month",
        "renew passport every year on march 3",
        "mom's birthday march 3 yearly a week before",
        "water plants every 3 days",
        "take vitamins every morning",
        "stand-up every weekday 9:15am",
        "call the bank tomorrow 2pm",
        "dentist dec 5 3:30pm 2 days before",
        "buy milk in 3 days",
        "submit essay friday 11:59pm",
    ),
    "Words it understands" to listOf(
        "Repeat: daily, weekly, monthly, yearly, every day / week / month / year, every 2 weeks, every other…, biweekly, every weekday, every weekend, every monday and friday, every 5th, every 2nd tuesday, last day of the month.",
        "Date: today, tonight, tomorrow, friday, next monday, dec 5, 5 december, 12/5 (month/day), in 3 days, next week, the 15th.",
        "Time: 9am, 9:30 pm, 21:00, at 7, noon, midnight, morning (9 AM), afternoon (3 PM), evening (6 PM), tonight (8 PM).",
        "Heads-up: 1 day before, 3 days before, a week before, the day before.",
        "No time typed? It uses your default time (Settings).",
    ),
    "The form" to listOf(
        "Tap + for a blank form, or tap any reminder to edit it. Pick the date, time, how it repeats, a heads-up, a category and notes. The colored box at the bottom shows exactly what will happen and when it rings next.",
    ),
    "Home screen" to listOf(
        "The calendar shows colored dots on days with reminders. Tap a day to see only that day, tap it again (or Show all) to go back.",
        "Tap the month name to jump back to today.",
        "The list is grouped as Overdue, Today, This week and Later.",
        "Tap the circle or swipe right to mark done. A one-time reminder is removed. A repeating one moves to its next date. You get an Undo either way.",
        "Swipe left to delete. It asks you to confirm first.",
        "Tap a category chip to filter by category.",
    ),
    "Notifications" to listOf(
        "A notification appears at the exact time, even with no internet. Its buttons let you mark it Done or snooze it 10 minutes or 1 hour.",
        "If you set a heads-up, you also get an early notification before the day.",
        "Reminders come back by themselves after the phone restarts.",
    ),
    "If notifications don't show up (Nothing Phone)" to listOf(
        "1. Settings in this app → make sure Notifications says Allowed.",
        "2. Settings in this app → Battery → tap Allow. Or: phone Settings → Apps → Reminders → Battery → Unrestricted.",
        "3. Don't swipe the app away while a test is running, and don't use Extreme battery saver.",
        "4. Use Send test notification in Settings to check.",
    ),
    "Backups" to listOf(
        "Settings → Export backup saves everything to a file. Keep it in Google Drive or on your laptop. Import backup on a new phone puts it all back.",
        "Everything stays on your phone. There's no account and no internet.",
    ),
)

@Composable
fun HelpScreen(onboarding: Boolean, onBack: () -> Unit, onFinish: () -> Unit) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onFinish() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (onboarding) "Welcome" else "How it works") },
                navigationIcon = {
                    if (!onboarding) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onboarding) {
                Text(
                    "Type what you need to remember and when. The app sets it up for you and notifies you, completely offline.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            sections.forEach { (title, lines) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(14.dp).fillMaxWidth()) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        val examples = title == "Things you can type"
                        lines.forEach {
                            Text(
                                if (examples) "“$it”" else it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = if (examples) FontFamily.Monospace else FontFamily.Default,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            if (onboarding) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Allow notifications & get started") }
                Text(
                    "You can open this guide anytime from the ⓘ button on the home screen.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
