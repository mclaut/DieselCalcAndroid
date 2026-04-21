package com.mclaut.dieselcalc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchUrl(urlString: String): String = withContext(Dispatchers.IO) {
    val conn = URL(urlString).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7)")
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    val result = conn.inputStream.bufferedReader().readText()
    conn.disconnect()
    result
}

suspend fun fetchInvestingPrice(): Double? = try {
    val html = fetchUrl("https://www.investing.com/commodities/london-gas-oil")
    val p1 = Regex("""data-test="instrument-price-last"[^>]*>([\d,\.]+)""")
    val p2 = Regex("""id="last_last"[^>]*>([\d,\.]+)""")
    (p1.find(html) ?: p2.find(html))?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
} catch (e: Exception) { null }

suspend fun fetchUSDMizhbank(): Double? = try {
    val html = fetchUrl("https://minfin.com.ua/ua/currency/mb/")
    val p1 = Regex(""""@type":\s*"UnitPriceSpecification",\s*"price":\s*"([\d.]+)"""")
    val p2 = Regex(""""UnitPriceSpecification","price":([\d.]+),"priceCurrency":"UAH"""")
    (p1.findAll(html).toList().getOrNull(1) ?: p2.findAll(html).toList().getOrNull(1))
        ?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }

suspend fun fetchEURNBU(): Double? = try {
    val html = fetchUrl("https://minfin.com.ua/ua/currency/nbu/eur/")
    val pattern = Regex(""""UnitPriceSpecification","price":([\d.]+),"priceCurrency":"UAH"""")
    pattern.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
} catch (e: Exception) { null }
