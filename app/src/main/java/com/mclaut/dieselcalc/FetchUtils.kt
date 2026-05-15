package com.mclaut.dieselcalc

import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchUrl(urlString: String, headers: Map<String, String> = emptyMap()): String =
    withContext(Dispatchers.IO) {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "DieselCalc/1.0 Android")
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.instanceFollowRedirects = true
        val result = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        conn.disconnect()
        result
    }

private val apiHeaders = mapOf("X-API-Key" to ApiConfig.API_KEY)

// ICE London Gas Oil з власного сервера (для Diesel-розрахунку і ICE Gasoil рядка віджета)
suspend fun fetchInvestingPrice(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/quotes/gasoil", apiHeaders)
    Regex(""""price"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

// USD/UAH медіана (міжбанк) з власного сервера (для розрахунку Diesel)
suspend fun fetchUSDMizhbank(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/rates/fiat/USD/median?max_age=300", apiHeaders)
    Regex(""""median"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

// EUR/UAH медіана з власного сервера (для розрахунку Diesel)
suspend fun fetchEURNBU(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/rates/fiat/EUR/median?max_age=300", apiHeaders)
    Regex(""""median"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

// === Currency widgets — мульти-джерельні дані ===

/**
 * Один запис курсу від конкретного джерела.
 * Якщо bid/ask є — джерело підтримує spread (Privat Card/Cash, тепер і Міжбанк).
 * Якщо тільки rate — single (НБУ, fallback для Міжбанку якщо bid/ask відсутні).
 */
data class RateEntry(val source: String, val bid: Double?, val ask: Double?, val rate: Double)

/** Повертає всі курси UAH-пари ("USD" або "EUR") з усіх джерел. */
suspend fun fetchAllRates(currency: String): List<RateEntry> = try {
    val text = fetchUrl("${ApiConfig.BASE_URL}/rates/fiat/$currency", apiHeaders)
    val obj  = JSONObject(text)
    val arr  = obj.optJSONArray("rates") ?: JSONArray()
    val list = mutableListOf<RateEntry>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list += RateEntry(
            source = o.getString("source"),
            bid    = if (o.has("bid")) o.getDouble("bid") else null,
            ask    = if (o.has("ask")) o.getDouble("ask") else null,
            rate   = o.getDouble("rate")
        )
    }
    list
} catch (e: Exception) { emptyList() }

/** EUR/USD крос-курс (з /quotes/eur-usd). */
suspend fun fetchEurUsdCross(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/quotes/eur-usd", apiHeaders)
    Regex(""""price"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }
