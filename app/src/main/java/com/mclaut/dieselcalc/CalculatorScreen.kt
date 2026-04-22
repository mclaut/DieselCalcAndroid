package com.mclaut.dieselcalc

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    onNewEntry: (LogEntry) -> Unit,
    calculateTrigger: Int = 0,
    updateTrigger: Int = 0,
    onUpdatingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var icePrice by remember { mutableStateOf("1373.5") }
    var premium by remember { mutableStateOf("135") }
    var roadUah by remember { mutableStateOf("70000") }
    var deliveryUah by remember { mutableStateOf("60000") }
    var usdUah by remember { mutableStateOf("43.80") }
    var eurUah by remember { mutableStateOf("50.6104") }
    var exciseEur by remember { mutableStateOf("253.8") }
    var density by remember { mutableStateOf("0.84") }
    var litresPerTruck by remember { mutableStateOf("30000") }
    var comment by remember { mutableStateOf("") }

    var result by remember { mutableStateOf("") }
    var priceAtBorder by remember { mutableStateOf("") }
    var priceAtDelivery by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    var isFetchingPrice by remember { mutableStateOf(false) }
    var isFetchingUSD by remember { mutableStateOf(false) }
    var isFetchingEUR by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var usdError by remember { mutableStateOf<String?>(null) }
    var eurError by remember { mutableStateOf<String?>(null) }

    val isUpdating = isFetchingPrice || isFetchingUSD || isFetchingEUR
    LaunchedEffect(isUpdating) { onUpdatingChanged(isUpdating) }

    // Початкове завантаження
    LaunchedEffect(Unit) {
        isFetchingPrice = true; priceError = null
        fetchInvestingPrice()?.let { icePrice = it.toString() } ?: run { priceError = "Помилка ICE" }
        isFetchingPrice = false
        isFetchingUSD = true; usdError = null
        fetchUSDMizhbank()?.let { usdUah = it.toString() } ?: run { usdError = "Курс не знайдено" }
        isFetchingUSD = false
        isFetchingEUR = true; eurError = null
        fetchEURNBU()?.let { eurUah = it.toString() } ?: run { eurError = "Курс не знайдено" }
        isFetchingEUR = false
    }

    // Тригер розрахунку з bottom bar
    LaunchedEffect(calculateTrigger) {
        if (calculateTrigger > 0) {
            calculate(
                context, icePrice, premium, roadUah, deliveryUah, usdUah, eurUah,
                exciseEur, density, litresPerTruck, comment,
                onResult = { r, border, delivery ->
                    result = r
                    priceAtBorder = border
                    priceAtDelivery = delivery
                    hasResult = true
                    showResult = true
                },
                onNewEntry = onNewEntry
            )
        }
    }

    // Тригер оновлення курсів з bottom bar
    LaunchedEffect(updateTrigger) {
        if (updateTrigger > 0) {
            isFetchingPrice = true; priceError = null
            fetchInvestingPrice()?.let { icePrice = it.toString() } ?: run { priceError = "Помилка ICE" }
            isFetchingPrice = false
            isFetchingUSD = true; usdError = null
            fetchUSDMizhbank()?.let { usdUah = it.toString() } ?: run { usdError = "Курс не знайдено" }
            isFetchingUSD = false
            isFetchingEUR = true; eurError = null
            fetchEURNBU()?.let { eurUah = it.toString() } ?: run { eurError = "Курс не знайдено" }
            isFetchingEUR = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // ── Поля вводу (повний екран, скрол) ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InputField("London Gas Oil", icePrice, Modifier.weight(1f)) { icePrice = it }
                IconButton(onClick = {
                    scope.launch {
                        isFetchingPrice = true; priceError = null
                        fetchInvestingPrice()?.let { icePrice = it.toString() } ?: run { priceError = "Помилка ICE" }
                        isFetchingPrice = false
                    }
                }, enabled = !isFetchingPrice) {
                    if (isFetchingPrice) CircularProgressIndicator(Modifier.size(22.dp))
                    else Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            priceError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

            InputField("Премія $/т", premium) { premium = it }
            InputField("Транспорт кордон грн/авто", roadUah) { roadUah = it }
            InputField("Транспорт до вигрузки", deliveryUah) { deliveryUah = it }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                InputField("USD Міжбанк", usdUah, Modifier.weight(1f)) { usdUah = it }
                IconButton(onClick = {
                    scope.launch {
                        isFetchingUSD = true; usdError = null
                        fetchUSDMizhbank()?.let { usdUah = it.toString() } ?: run { usdError = "Курс не знайдено" }
                        isFetchingUSD = false
                    }
                }, enabled = !isFetchingUSD) {
                    if (isFetchingUSD) CircularProgressIndicator(Modifier.size(22.dp))
                    else Icon(Icons.Default.Refresh, null, tint = Color(0xFF4CAF50))
                }
            }
            usdError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                InputField("EUR NBU", eurUah, Modifier.weight(1f)) { eurUah = it }
                IconButton(onClick = {
                    scope.launch {
                        isFetchingEUR = true; eurError = null
                        fetchEURNBU()?.let { eurUah = it.toString() } ?: run { eurError = "Курс не знайдено" }
                        isFetchingEUR = false
                    }
                }, enabled = !isFetchingEUR) {
                    if (isFetchingEUR) CircularProgressIndicator(Modifier.size(22.dp))
                    else Icon(Icons.Default.Refresh, null, tint = Color(0xFF9C27B0))
                }
            }
            eurError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

            InputField("Акциз EUR/1000 літрів", exciseEur) { exciseEur = it }
            InputField("Щільність кг/л", density) { density = it }
            InputField("Літрів у машині", litresPerTruck) { litresPerTruck = it }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Коментар", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            )
        }

        // ── Оверлей результату ────────────────────────────────────────────
        if (hasResult && showResult) {
            // Затемнення фону — дотик закриває
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showResult = false }
            )

            // Картка результату — дотик НЕ закриває
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1F0D))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Заголовок: "Результат" + копіювати + закрити
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Результат",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyToClipboard(context, result) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Копіювати",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = { showResult = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрити",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    // Повний текст розрахунку
                    Text(
                        result,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50)
                    )

                    HorizontalDivider(color = Color(0xFF1E4D1E))

                    // Ціна на кордоні
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Кордон: $priceAtBorder грн/л",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyToClipboard(context, priceAtBorder) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                null,
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E4D1E))

                    // Ціна у вигрузці
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Вигрузка: $priceAtDelivery грн/л",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp,
                            color = Color(0xFFF47920),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyToClipboard(context, priceAtDelivery) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                null,
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFF47920)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculate(
    context: Context,
    icePrice: String, premium: String, roadUah: String, deliveryUah: String,
    usdUah: String, eurUah: String, exciseEur: String, density: String,
    litresPerTruck: String, comment: String,
    onResult: (result: String, border: String, delivery: String) -> Unit,
    onNewEntry: (LogEntry) -> Unit
) {
    val icePriceD = icePrice.replace(",", ".").toDoubleOrNull() ?: return
    val premiumD = premium.replace(",", ".").toDoubleOrNull() ?: return
    val roadUahD = roadUah.replace(",", ".").toDoubleOrNull() ?: return
    val deliveryUahD = deliveryUah.replace(",", ".").toDoubleOrNull() ?: return
    val usdUahD = usdUah.replace(",", ".").toDoubleOrNull() ?: return
    val eurUahD = eurUah.replace(",", ".").toDoubleOrNull() ?: return
    val exciseEurD = exciseEur.replace(",", ".").toDoubleOrNull() ?: return
    val densityD = density.replace(",", ".").toDoubleOrNull() ?: return
    val litresD = max(litresPerTruck.replace(",", ".").toDoubleOrNull() ?: 1.0, 1.0)

    val pricePer1000L = (icePriceD + premiumD) * usdUahD * densityD
    val exciseUAH = exciseEurD * eurUahD
    val transportPer1000L = (roadUahD / litresD) * 1000
    val totalBeforeVAT = pricePer1000L + exciseUAH + transportPer1000L
    val vatPer1000L = totalBeforeVAT * 0.20
    val finalPer1000L = totalBeforeVAT + vatPer1000L
    val borderPerLitre = finalPer1000L / 1000.0
    val deliveryPerLitre = deliveryUahD / litresD

    val priceAtBorder = formatNum(borderPerLitre, 2)
    val priceAtDelivery = formatNum(borderPerLitre + deliveryPerLitre, 2)

    val commentLine = if (comment.isNotEmpty()) "📝 $comment\n" else ""
    val result = buildString {
        append(commentLine)
        append("База: ${formatNum(pricePer1000L)}\n")
        append("Транспорт кордон: ${formatNum(transportPer1000L)}\n")
        append("Акциз: ${formatNum(exciseUAH)}\n")
        append("ПДВ (20%): ${formatNum(vatPer1000L)}\n")
        append("─────────────────────\n")
        append("Ціна на кордоні: $priceAtBorder грн/л\n")
        append("Транспорт вигрузка: ${formatNum(deliveryPerLitre, 2)} грн/л\n")
        append("Ціна у вигрузці: $priceAtDelivery грн/л")
    }

    onResult(result, priceAtBorder, priceAtDelivery)

    // Зберігаємо параметри для віджету і оновлюємо його
    WidgetPrefs.saveParams(context, premiumD, roadUahD, deliveryUahD, exciseEurD, densityD, litresD)
    WidgetUpdateWorker.enqueue(context, immediate = true)

    onNewEntry(LogEntry(
        comment = comment,
        icePrice = icePriceD, premium = premiumD,
        roadUah = roadUahD, deliveryUah = deliveryUahD,
        usdUah = usdUahD, eurUah = eurUahD,
        exciseEur = exciseEurD, density = densityD,
        litresPerTruck = litresD,
        priceAtBorder = priceAtBorder,
        priceAtDelivery = priceAtDelivery,
        fullResult = result
    ))
}

@Composable
fun InputField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp)
    )
}
