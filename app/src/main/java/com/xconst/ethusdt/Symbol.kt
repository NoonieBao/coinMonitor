package com.xconst.ethusdt

import kotlinx.serialization.Serializable

@Serializable
enum class Symbol(
    val okxInstId: String,
    val displayName: String
) {
    ETH("ETH-USDT-SWAP", "ETH"),
    BTC("BTC-USDT-SWAP", "BTC");

    companion object {
        fun fromInstId(instId: String): Symbol? {
            return entries.firstOrNull { it.okxInstId == instId }
        }
    }
}