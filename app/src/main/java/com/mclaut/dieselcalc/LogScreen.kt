package com.mclaut.dieselcalc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    entries: List<LogEntry>,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var selectedEntry by remember { mutableStateOf<LogEntry?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    BackHandler(enabled = selectedEntry != null) { selectedEntry = null }

    if (selectedEntry != null) {
        LogDetailScreen(entry = selectedEntry!!, dateFormat = dateFormat, onBack = { selectedEntry = null })
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Лог розрахунків") },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("Очистити", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Лог порожній\nРозрахунки з'являться тут після натискання «Розрахувати»",
                    color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = {
                            Text(if (entry.comment.isEmpty()) "Без коментаря" else entry.comment,
                                fontSize = 14.sp, color = if (entry.comment.isEmpty()) Color.Gray else LocalContentColor.current)
                        },
                        supportingContent = {
                            Text(dateFormat.format(Date(entry.date)), fontSize = 11.sp, color = Color.Gray)
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${entry.priceAtBorder} грн/л", fontSize = 12.sp, color = Color(0xFF2196F3))
                                Text("${entry.priceAtDelivery} грн/л", fontSize = 12.sp, color = Color(0xFFF47920))
                            }
                        },
                        modifier = Modifier.clickable { selectedEntry = entry }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистити весь лог?") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text("Очистити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") }
            }
        )
    }
}
