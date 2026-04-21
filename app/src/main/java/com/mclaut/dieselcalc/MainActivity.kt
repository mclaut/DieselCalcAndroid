package com.mclaut.dieselcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mclaut.dieselcalc.ui.theme.DieselCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val logStore = LogStore(this)
        setContent {
            DieselCalcTheme {
                MainScreen(logStore)
            }
        }
    }
}

@Composable
fun MainScreen(logStore: LogStore) {
    var logEntries by remember { mutableStateOf(logStore.getEntries()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var calculateTrigger by remember { mutableIntStateOf(0) }
    var updateTrigger by remember { mutableIntStateOf(0) }
    var isUpdatingState by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Ліво: перемикач вигляду ──────────────────────────────
                    IconButton(
                        onClick = { selectedTab = if (selectedTab == 0) 1 else 0 },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.History else Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (selectedTab == 0) "Архів" else "Калькулятор",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // ── Центр: оновити курси ──────────────────────────────────
                    IconButton(
                        onClick = { updateTrigger++ },
                        enabled = !isUpdatingState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (isUpdatingState)
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF47920)
                            )
                        else
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Оновити",
                                tint = Color(0xFFF47920),
                                modifier = Modifier.size(26.dp)
                            )
                    }

                    // ── Право: розрахувати ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { selectedTab = 0; calculateTrigger++ },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Icon(
                                Icons.Filled.Calculate,
                                contentDescription = "Розрахувати",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> CalculatorScreen(
                modifier = Modifier.padding(innerPadding),
                onNewEntry = { entry -> logEntries = logStore.addEntry(entry) },
                calculateTrigger = calculateTrigger,
                updateTrigger = updateTrigger,
                onUpdatingChanged = { isUpdatingState = it }
            )
            1 -> LogScreen(
                modifier = Modifier.padding(innerPadding),
                entries = logEntries,
                onDelete = { id -> logEntries = logStore.deleteEntry(id) },
                onClearAll = { logEntries = logStore.clearAll() }
            )
        }
    }
}
