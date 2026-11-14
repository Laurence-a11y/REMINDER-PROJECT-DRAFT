@file:OptIn(ExperimentalMaterial3Api::class)

package com.rence.reminders.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.rence.reminders.data.Store
import com.rence.reminders.model.AppData
import com.rence.reminders.model.Category
import com.rence.reminders.model.Palette

@Composable
fun CategoriesScreen(data: AppData, store: Store, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Category(0, "", Palette.colors[data.categories.size % Palette.colors.size])
            }) { Icon(Icons.Filled.Add, "Add category") }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
            if (data.categories.isEmpty()) {
                item { Text("No categories yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(data.categories, key = { it.id }) { c ->
                val count = data.reminders.count { it.categoryId == c.id }
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(c.colorValue(), 16.dp)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (count == 1) "1 reminder" else "$count reminders",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, "Edit") }
                        IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    editing?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        var color by remember(c) { mutableStateOf(c.color) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (c.id == 0L) "New category" else "Edit category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Palette.colors.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            row.forEach { col ->
                                val sel = col == color
                                ColorDot(
                                    Color(col), 34.dp,
                                    Modifier
                                        .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                        .clickable { color = col },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    store.saveCategory(c.copy(name = name.trim(), color = color))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${c.name}\"?") },
            text = { Text("Reminders in this category are kept. They just won't have a category.") },
            confirmButton = { TextButton(onClick = { store.deleteCategory(c.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
