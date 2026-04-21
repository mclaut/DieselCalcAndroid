package com.mclaut.dieselcalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    entry: LogEntry,
    dateFormat: SimpleDateFormat,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(dateFormat.format(Date(entry.date)), fontSize = 12.sp, color = Color.Gray)
            if (entry.comment.isNotEmpty()) Text("📝 ${entry.comment}", fontSize = 14.sp)

            HorizontalDivider()
            Text("Вхідні дані", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

            DetailRow("London Gas Oil", entry.icePrice.toString())
            DetailRow("Премія $/т", entry.premium.toString())
            DetailRow("Транспорт кордон", entry.roadUah.toString())
            DetailRow("Транспорт вигрузка", entry.deliveryUah.toString())
            DetailRow("USD Міжбанк", entry.usdUah.toString())
            DetailRow("EUR NBU", entry.eurUah.toString())
            DetailRow("Акциз EUR/1000л", entry.exciseEur.toString())
            DetailRow("Щільність кг/л", entry.density.toString())
            DetailRow("Літрів у машині", entry.litresPerTruck.toString())

            HorizontalDivider()
            Text("Результат", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1F0D))) {
                Text(
                    text = entry.fullResult,
                    modifier = Modifier.padding(10.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { copyToClipboard(context, entry.priceAtBorder) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Кордон ${entry.priceAtBorder}", fontSize = 11.sp, color = Color(0xFF2196F3))
                }
                OutlinedButton(
                    onClick = { copyToClipboard(context, entry.priceAtDelivery) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Вигрузка ${entry.priceAtDelivery}", fontSize = 11.sp, color = Color(0xFFF47920))
                }
            }

            OutlinedButton(
                onClick = { copyToClipboard(context, entry.fullResult) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Копіювати весь результат", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
