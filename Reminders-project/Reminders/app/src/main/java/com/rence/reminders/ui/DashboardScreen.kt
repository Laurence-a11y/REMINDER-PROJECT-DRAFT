@file:OptIn(ExperimentalMaterial3Api::class)

package com.rence.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rence.reminders.Screen
import com.rence.reminders.data.Store
import com.rence.reminders.logic.Format
import com.rence.reminders.logic.Parser
import com.rence.reminders.logic.Recurrence
import com.rence.reminders.model.AppData
import com.rence.reminders.model.Category
import com.rence.reminders.model.Reminder
import com.rence.reminders.model.RepeatType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Item(val r: Reminder, val occ: LocalDateTime, val done: Boolean)

@Composable
fun DashboardScreen(data: AppData, store: Store, nav: (Screen) -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = LocalDateTime.now() }

    val today = now.toLocalDate()
    var month by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var filter by remember { mutableStateOf<Long?>(null) }
    var quick by remember { mutableStateOf("") }
    var showCompleted by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cats = remember(data.categories) { data.categories.associateBy { it.id } }
    val reminders = remember(data.reminders, filter) {
        data.reminders.filter { filter == null || it.categoryId == filter }
    }

    // Grouped list
    val open = remember(reminders, now) {
        reminders.mapNotNull { r -> Recurrence.nextOpen(r)?.let { Item(r, it, false) } }.sortedBy { it.occ }
    }
    val overdue = open.filter { it.occ.isBefore(now) }
    val todayItems = open.filter { !it.occ.isBefore(now) && it.occ.toLocalDate() == today }
    val week = open.filter { val d = it.occ.toLocalDate(); d.isAfter(today) && !d.isAfter(today.plusDays(7)) }
    val later = open.filter { it.occ.toLocalDate().isAfter(today.plusDays(7)) }
    val completed = remember(reminders) { reminders.filter { Recurrence.isFinished(it) }.sortedByDescending { it.startDate } }

    val dayItems = remember(reminders, selected) {
        val d = selected ?: return@remember emptyList<Item>()
        reminders.mapNotNull { r -> Recurrence.occurrenceOn(r, d)?.let { Item(r, it, Recurrence.isDone(r, it)) } }
            .sortedBy { it.occ }
    }

    val dots = remember(reminders, month, cats) {
        val map = HashMap<LocalDate, List<Color>>()
        for (day in 1..month.lengthOfMonth()) {
            val d = month.atDay(day)
            val colors = reminders.filter { Recurrence.matches(it, d) }
                .map { r -> r.categoryId?.let { cats[it]?.colorValue() } ?: Color.Gray }
            if (colors.isNotEmpty()) map[d] = colors.distinct().take(3)
        }
        map
    }

    fun done(item: Item) {
        if (item.done) {
            store.setCompletedThrough(item.r.id, item.occ.minusSeconds(1).takeIf { item.r.rule.type != RepeatType.NONE })
            return
        }
        val before = store.markDone(item.r.id, item.occ)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val res = snackbar.showSnackbar("Done: ${item.r.title}", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) store.setCompletedThrough(item.r.id, before)
        }
    }

    fun delete(r: Reminder) {
        store.deleteReminder(r.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val res = snackbar.showSnackbar("Deleted: ${r.title}", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) store.saveReminder(r)
        }
    }

    fun quickSave() {
        val pr = Parser.parse(quick, LocalDate.now())
        if (pr.title.isBlank()) return
        val r = store.saveReminder(buildReminder(pr, data.settings.defaultTime, LocalDateTime.now(), filter))
        quick = ""
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val res = snackbar.showSnackbar(
                "Saved: ${r.title} — ${Format.summary(r.rule, r.startDate, r.time, r.leadDays)}",
                actionLabel = "Undo", duration = SnackbarDuration.Short,
            )
            if (res == SnackbarResult.ActionPerformed) store.deleteReminder(r.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { nav(Screen.Categories) }) { Icon(Icons.AutoMirrored.Filled.List, "Categories") }
                    IconButton(onClick = { nav(Screen.Help) }) { Icon(Icons.Filled.Info, "How it works") }
                    IconButton(onClick = { nav(Screen.Settings) }) { Icon(Icons.Filled.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav(Screen.Edit(null, null)) }) { Icon(Icons.Filled.Add, "Add with form") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                QuickAdd(
                    text = quick,
                    onText = { quick = it },
                    today = today,
                    onSave = { quickSave() },
                    onEdit = { nav(Screen.Edit(null, quick)); quick = "" },
                )
            }
            item {
                MonthCalendar(
                    month = month,
                    today = today,
                    selected = selected,
                    dots = dots,
                    onPrev = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                    onTitle = { month = YearMonth.from(today); selected = null },
                    onSelect = { selected = if (selected == it) null else it },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    data.categories.forEach { c ->
                        FilterChip(
                            selected = filter == c.id,
                            onClick = { filter = if (filter == c.id) null else c.id },
                            label = { Text(c.name) },
                            leadingIcon = { ColorDot(c.colorValue()) },
                        )
                    }
                }
            }

            val sel = selected
            if (sel != null) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Format.date(sel, today), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selected = null }) { Text("Show all") }
                    }
                }
                if (dayItems.isEmpty()) {
                    item { Empty("Nothing on this day.") }
                } else {
                    items(dayItems, key = { "d" + it.r.id }) { ReminderRow(it, cats, now, today, { done(it) }, { delete(it.r) }) { nav(Screen.Edit(it.r.id, null)) } }
                }
            } else {
                if (open.isEmpty() && completed.isEmpty()) {
                    item { Empty("No reminders yet.\nType one above, like \"pay rent every 5th at 9am\".") }
                }
                fun section(title: String, list: List<Item>, alert: Boolean = false) {
                    if (list.isEmpty()) return
                    item(key = "h$title") { Header(title, list.size, alert) }
                    items(list, key = { title + it.r.id }) {
                        ReminderRow(it, cats, now, today, { done(it) }, { delete(it.r) }) { nav(Screen.Edit(it.r.id, null)) }
                    }
                }
                section("Overdue", overdue, alert = true)
                section("Today", todayItems)
                section("This week", week)
                section("Later", later)
                if (completed.isNotEmpty()) {
                    item(key = "hCompleted") {
                        TextButton(onClick = { showCompleted = !showCompleted }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(if (showCompleted) "Hide completed (${completed.size})" else "Show completed (${completed.size})")
                        }
                    }
                    if (showCompleted) {
                        items(completed, key = { "c" + it.id }) { r ->
                            val occ = r.startDate.atTime(r.time)
                            ReminderRow(Item(r, occ, true), cats, now, today, { done(Item(r, occ, true)) }, { delete(r) }) {
                                nav(Screen.Edit(r.id, null))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAdd(text: String, onText: (String) -> Unit, today: LocalDate, onSave: () -> Unit, onEdit: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type a reminder… e.g. pay rent every 5th 9am") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
        )
        if (text.isNotBlank()) {
            val pr = remember(text, today) { Parser.parse(text, today) }
            val preview = remember(pr) { buildReminder(pr, java.time.LocalTime.of(9, 0), LocalDateTime.now()) }
            Card(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        pr.title.ifBlank { "(add a title)" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pr.title.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    val timeText = pr.time?.let { Format.time(it) } ?: "default time"
                    val parts = listOfNotNull(
                        Format.repeat(preview.rule, preview.startDate, today), timeText, Format.lead(pr.leadDays),
                    )
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onEdit) { Text("Edit details") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onSave, enabled = pr.title.isNotBlank()) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    dots: Map<LocalDate, List<Color>>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTitle: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val fmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                Text(
                    month.format(fmt),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable(onClick = onTitle).padding(8.dp),
                )
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
            }
            Row {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(
                        it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday first
            val len = month.lengthOfMonth()
            val rows = (offset + len + 6) / 7
            for (w in 0 until rows) {
                Row {
                    for (c in 0 until 7) {
                        val day = w * 7 + c - offset + 1
                        if (day in 1..len) {
                            val d = month.atDay(day)
                            DayCell(d, d == today, d == selected, dots[d].orEmpty(), Modifier.weight(1f)) { onSelect(d) }
                        } else {
                            Spacer(Modifier.weight(1f).height(46.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isToday: Boolean, isSelected: Boolean, dots: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .height(46.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) cs.primary else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, cs.primary, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            d.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) cs.onPrimary else cs.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(7.dp).padding(top = 2.dp)) {
            dots.forEach { ColorDot(if (isSelected) cs.onPrimary else it, 5.dp) }
        }
    }
}

@Composable
private fun Header(title: String, count: Int, alert: Boolean) {
    Text(
        "$title  $count",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (alert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Empty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

@Composable
private fun ReminderRow(
    item: Item,
    cats: Map<Long, Category>,
    now: LocalDateTime,
    today: LocalDate,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val lastAction = remember { longArrayOf(0L) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            val t = System.currentTimeMillis()
            if (t - lastAction[0] > 800) {
                when (v) {
                    SwipeToDismissBoxValue.StartToEnd -> { lastAction[0] = t; onDone() }
                    SwipeToDismissBoxValue.EndToStart -> { lastAction[0] = t; onDelete() }
                    SwipeToDismissBoxValue.Settled -> {}
                }
            }
            false // always spring back; the list updates itself
        },
    )
    val cs = MaterialTheme.colorScheme
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundContent = {
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toLeft = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    .background(if (toRight) Color(0xFF2E7D32) else if (toLeft) cs.primary else Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (toRight) Icon(Icons.Filled.Check, "Done", tint = Color.White)
                if (toLeft) Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
            }
        },
    ) {
        val r = item.r
        val cat = r.categoryId?.let { cats[it] }
        val color = cat?.colorValue() ?: cs.outline
        val overdue = !item.done && item.occ.isBefore(now)
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
        ) {
            Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(5.dp).fillMaxHeight().background(color))
                Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CheckDot(item.done, color, onDone)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            r.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = if (item.done) cs.onSurfaceVariant else cs.onSurface,
                        )
                        Text(
                            (if (overdue) "Overdue · " else "") + Format.relative(item.occ, today),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) cs.primary else cs.onSurfaceVariant,
                        )
                        if (r.rule.type != RepeatType.NONE) {
                            Text(
                                Format.repeat(r.rule, r.startDate, today),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    if (cat != null) {
                        Text(cat.name, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

