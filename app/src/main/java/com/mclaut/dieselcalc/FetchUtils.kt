package com.mclaut.dieselcalc

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

// ICE London Gas Oil з власного сервера
suspend fun fetchInvestingPrice(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/quotes/gasoil", apiHeaders)
    Regex(""""price"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

// USD/UAH медіана (міжбанк) з власного сервера
suspend fun fetchUSDMizhbank(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/rates/fiat/USD/median?max_age=300", apiHeaders)
    Regex(""""median"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

// EUR/UAH медіана з власного сервера
suspend fun fetchEURNBU(): Double? = try {
    val json = fetchUrl("${ApiConfig.BASE_URL}/rates/fiat/EUR/median?max_age=300", apiHeaders)
    Regex(""""median"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }
