package com.mclaut.dieselcalc

import java.util.UUID

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val comment: String = "",
    val icePrice: Double = 0.0,
    val premium: Double = 0.0,
    val roadUah: Double = 0.0,
    val deliveryUah: Double = 0.0,
    val usdUah: Double = 0.0,
    val eurUah: Double = 0.0,
    val exciseEur: Double = 0.0,
    val density: Double = 0.0,
    val litresPerTruck: Double = 0.0,
    val priceAtBorder: String = "",
    val priceAtDelivery: String = "",
    val fullResult: String = ""
)
