@file:OptIn(ExperimentalMaterial3Api::class)

package com.rence.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rence.reminders.logic.ParseResult
import com.rence.reminders.model.Category
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

fun Category.colorValue(): Color = Color(color)

/** Yearly rules always follow the start date's month/day. */
fun normalizeRule(rule: RepeatRule, start: LocalDate): RepeatRule =
    if (rule.type == RepeatType.YEARLY) rule.copy(month = start.monthValue, dayOfMonth = start.dayOfMonth) else rule

/** Turn typed text into a new (unsaved, id = 0) reminder. */
fun buildReminder(pr: ParseResult, defaultTime: LocalTime, now: LocalDateTime, categoryId: Long? = null): Reminder {
    val time = pr.time ?: defaultTime
    var start = pr.date ?: now.toLocalDate()
    if (pr.rule.type == RepeatType.NONE && pr.date == null && !start.atTime(time).isAfter(now)) {
        start = start.plusDays(1) // "call mom 8am" typed at 10am -> tomorrow
    }
    val rule = normalizeRule(pr.rule, start)
    return Reminder(
        id = 0,
        title = pr.title,
        categoryId = categoryId,
        startDate = start,
        time = time,
        rule = rule,
        leadDays = pr.leadDays,
        completedThrough = if (rule.type != RepeatType.NONE) now else null,
    )
}

@Composable
fun ColorDot(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun CheckDot(done: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .size(26.dp)
            .clip(CircleShape)
            .then(if (done) Modifier.background(color) else Modifier.border(2.dp, color, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PickDateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun PickTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = color, modifier = modifier)
}
