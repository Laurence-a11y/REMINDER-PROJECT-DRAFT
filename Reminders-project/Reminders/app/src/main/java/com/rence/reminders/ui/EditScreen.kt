@file:OptIn(ExperimentalMaterial3Api::class)

package com.rence.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rence.reminders.data.Store
import com.rence.reminders.logic.Format
import com.rence.reminders.logic.Parser
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.model.AppData
import com.rence.reminders.model.MonthlyMode
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatRule
import com.rence.reminders.model.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun EditScreen(data: AppData, store: Store, id: Long?, initialText: String?, onBack: () -> Unit) {
    val now = remember { LocalDateTime.now() }
    val today = now.toLocalDate()
    val defaultTime = data.settings.defaultTime
    val existing = remember(id) { id?.let { store.find(it) } }
    val initial: Reminder = remember(id) {
        existing
            ?: initialText?.let { buildReminder(Parser.parse(it, today), defaultTime, now) }
            ?: Reminder(id = 0, title = "", startDate = today, time = defaultTime)
    }

    var typed by remember { mutableStateOf(initialText ?: "") }
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var date by remember { mutableStateOf(initial.startDate) }
    var time by remember { mutableStateOf(initial.time) }
    var type by remember { mutableStateOf(initial.rule.type) }
    var interval by remember { mutableIntStateOf(initial.rule.interval) }
    var weekdays by remember { mutableStateOf(initial.rule.weekdays.ifEmpty { setOf(initial.startDate.dayOfWeek) }) }
    var mode by remember { mutableStateOf(initial.rule.monthlyMode) }
    var dom by remember { mutableIntStateOf(if (initial.rule.type == RepeatType.MONTHLY) initial.rule.dayOfMonth else initial.startDate.dayOfMonth) }
    var nth by remember { mutableIntStateOf(if (initial.rule.type == RepeatType.MONTHLY) initial.rule.nth else nthOf(initial.startDate)) }
    var nthDay by remember { mutableStateOf(if (initial.rule.type == RepeatType.MONTHLY) initial.rule.nthWeekday else initial.startDate.dayOfWeek) }
    var lead by remember { mutableIntStateOf(initial.leadDays) }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun followDate(d: LocalDate) {
        dom = d.dayOfMonth
        nth = nthOf(d)
        nthDay = d.dayOfWeek
        if (type != RepeatType.WEEKLY) weekdays = setOf(d.dayOfWeek)
    }

    fun load(r: Reminder) {
        if (r.title.isNotBlank()) title = r.title
        date = r.startDate
        time = r.time
        type = r.rule.type
        interval = r.rule.interval
        weekdays = r.rule.weekdays.ifEmpty { setOf(r.startDate.dayOfWeek) }
        mode = r.rule.monthlyMode
        if (r.rule.type == RepeatType.MONTHLY) {
            dom = r.rule.dayOfMonth; nth = r.rule.nth; nthDay = r.rule.nthWeekday
        } else {
            dom = r.startDate.dayOfMonth; nth = nthOf(r.startDate); nthDay = r.startDate.dayOfWeek
        }
        lead = r.leadDays
    }

    fun currentRule(): RepeatRule = when (type) {
        RepeatType.NONE -> RepeatRule()
        RepeatType.DAILY -> RepeatRule(RepeatType.DAILY, interval)
        RepeatType.WEEKLY -> RepeatRule(RepeatType.WEEKLY, interval, weekdays = weekdays.ifEmpty { setOf(date.dayOfWeek) })
        RepeatType.MONTHLY -> RepeatRule(RepeatType.MONTHLY, interval, monthlyMode = mode, dayOfMonth = dom, nth = nth, nthWeekday = nthDay)
        RepeatType.YEARLY -> RepeatRule(RepeatType.YEARLY, interval, month = date.monthValue, dayOfMonth = date.dayOfMonth)
    }

    fun draft(): Reminder {
        val rule = currentRule()
        val base = existing ?: Reminder(id = 0, title = "", startDate = date, time = time)
        val scheduleChanged = existing == null || existing.rule != rule || existing.startDate != date || existing.time != time
        return base.copy(
            title = title.trim(),
            notes = notes.trim(),
            categoryId = categoryId,
            startDate = date,
            time = time,
            rule = rule,
            leadDays = lead,
            completedThrough = when {
                existing == null -> if (rule.type != RepeatType.NONE) LocalDateTime.now() else null
                else -> existing.completedThrough
            },
            snoozeUntil = if (scheduleChanged) null else base.snoozeUntil,
            snoozeOccurrence = if (scheduleChanged) null else base.snoozeOccurrence,
        )
    }

    fun save() {
        if (title.isBlank()) return
        store.saveReminder(draft())
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New reminder" else "Edit reminder") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    TextButton(onClick = { save() }, enabled = title.isNotBlank()) { Text("Save") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Type it
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp)) {
                    SectionLabel("Type it and I'll fill the form")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. dentist dec 5 3pm 2 days before") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) load(buildReminder(Parser.parse(typed, today), defaultTime, LocalDateTime.now())) }),
                    )
                    FilledTonalButton(
                        onClick = { load(buildReminder(Parser.parse(typed, today), defaultTime, LocalDateTime.now())) },
                        enabled = typed.isNotBlank(),
                        modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
                    ) { Text("Fill form") }
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, null)
                    Spacer(Modifier.width(6.dp))
                    Text(Format.date(date, today))
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Notifications, null)
                    Spacer(Modifier.width(6.dp))
                    Text(Format.time(time))
                }
            }
            if (type != RepeatType.NONE) {
                Text("Starts on the date above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Repeat
            SectionLabel("Repeat")
            ChipRow {
                listOf(
                    RepeatType.NONE to "Once", RepeatType.DAILY to "Daily", RepeatType.WEEKLY to "Weekly",
                    RepeatType.MONTHLY to "Monthly", RepeatType.YEARLY to "Yearly",
                ).forEach { (t, label) ->
                    FilterChip(selected = type == t, onClick = {
                        if (t != type) {
                            if (t == RepeatType.WEEKLY && weekdays.isEmpty()) weekdays = setOf(date.dayOfWeek)
                            type = t
                        }
                    }, label = { Text(label) })
                }
            }

            if (type != RepeatType.NONE) {
                val unit = when (type) {
                    RepeatType.DAILY -> "day"; RepeatType.WEEKLY -> "week"; RepeatType.MONTHLY -> "month"; else -> "year"
                }
                Stepper("Every", interval, 1, 99, if (interval == 1) unit else "${unit}s") { interval = it }
            }

            if (type == RepeatType.WEEKLY) {
                SectionLabel("On")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEach { d ->
                        DayToggle(Format.dayShort(d).take(2), d in weekdays, Modifier.weight(1f)) {
                            weekdays = if (d in weekdays) (weekdays - d).ifEmpty { setOf(d) } else weekdays + d
                        }
                    }
                }
            }

            if (type == RepeatType.MONTHLY) {
                ChipRow {
                    FilterChip(selected = mode == MonthlyMode.DAY_OF_MONTH, onClick = { mode = MonthlyMode.DAY_OF_MONTH }, label = { Text("On day $dom") })
                    FilterChip(
                        selected = mode == MonthlyMode.NTH_WEEKDAY, onClick = { mode = MonthlyMode.NTH_WEEKDAY },
                        label = { Text("On the ${Format.ordinal(nth)} ${Format.dayShort(nthDay)}") },
                    )
                    FilterChip(selected = mode == MonthlyMode.LAST_DAY, onClick = { mode = MonthlyMode.LAST_DAY }, label = { Text("Last day") })
                }
                when (mode) {
                    MonthlyMode.DAY_OF_MONTH -> {
                        Stepper("Day", dom, 1, 31, "of the month") { dom = it }
                        if (dom > 28) Text("Shorter months use their last day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MonthlyMode.NTH_WEEKDAY -> {
                        ChipRow {
                            listOf(1, 2, 3, 4, -1).forEach { n ->
                                FilterChip(selected = nth == n, onClick = { nth = n }, label = { Text(if (n < 0) "Last" else Format.ordinal(n)) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            DayOfWeek.values().forEach { d ->
                                DayToggle(Format.dayShort(d).take(2), d == nthDay, Modifier.weight(1f)) { nthDay = d }
                            }
                        }
                    }
                    MonthlyMode.LAST_DAY -> {}
                }
            }

            if (type == RepeatType.YEARLY) {
                Text(
                    "Every year on ${Format.monthShort(date.monthValue)} ${date.dayOfMonth}. Change it with the date button.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("Heads-up notification")
            ChipRow {
                listOf(0 to "None", 1 to "1 day before", 2 to "2 days", 3 to "3 days", 7 to "1 week").forEach { (n, label) ->
                    FilterChip(selected = lead == n, onClick = { lead = n }, label = { Text(label) })
                }
                if (lead !in listOf(0, 1, 2, 3, 7)) FilterChip(selected = true, onClick = {}, label = { Text("$lead days") })
            }

            SectionLabel("Category")
            ChipRow {
                FilterChip(selected = categoryId == null, onClick = { categoryId = null }, label = { Text("None") })
                data.categories.forEach { c ->
                    FilterChip(
                        selected = categoryId == c.id, onClick = { categoryId = c.id },
                        label = { Text(c.name) }, leadingIcon = { ColorDot(c.colorValue()) },
                    )
                }
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            // Preview
            val d = draft()
            val next = remember(d) { Recurrence.nextOccurrence(d.copy(completedThrough = null), LocalDateTime.now()) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                    Text(title.ifBlank { "(no title yet)" }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Format.summary(d.rule, d.startDate, d.time, d.leadDays, today), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        next?.let { "Next: ${Format.relative(it, today)}" } ?: "Date and time are in the past",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Button(onClick = { save() }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Save reminder")
            }
            if (existing != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) { Text("Delete reminder") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) PickDateDialog(date, { showDate = false }) { date = it; followDate(it) }
    if (showTime) PickTimeDialog(time, { showTime = false }) { time = it }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reminder?") },
            text = { Text("\"${existing.title}\" will be removed.") },
            confirmButton = { TextButton(onClick = { store.deleteReminder(existing.id); confirmDelete = false; onBack() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun nthOf(d: LocalDate): Int {
    val n = (d.dayOfMonth - 1) / 7 + 1
    return if (n > 4) -1 else n
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, suffix: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(56.dp))
        OutlinedButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Text("−") }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        OutlinedButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+") }
        Text(suffix, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) cs.primary else Color.Transparent)
            .border(1.dp, if (selected) cs.primary else cs.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) cs.onPrimary else cs.onSurface, style = MaterialTheme.typography.labelMedium)
    }
}

