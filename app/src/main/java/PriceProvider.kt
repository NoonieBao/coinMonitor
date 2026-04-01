package com.xconst.ethusdt // 请根据你的实际包名修改

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.xconst.ethusdt.bus.AppBus

// 引用你的全局总线和枚举




class PriceProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == "getLatestPrice") {
            val state = AppBus.appState.value
            
            // 构建显示的字符串
            val priceContent = buildString {
                // 假设 Symbol 是一个枚举或对象，这里匹配你的逻辑
                state.prices[Symbol.ETH]?.let { append("ETH: $it  ") }
                append("\n")
                state.prices[Symbol.BTC]?.let { append("BTC: $it") }
            }

            return Bundle().apply {
                // 如果没有价格，返回 "Loading..." 避免 AOD 显示空白
                putString("content", if (priceContent.isEmpty()) "Xconst Loading..." else priceContent)
                // 也可以单独传数值，方便 AOD 端做颜色判断（比如涨红跌绿）
                putDouble("eth_raw", state.prices[Symbol.ETH] ?: 0.0)
            }
        }
        return null
    }

    // 以下是 ContentProvider 必须实现但在此场景下不用的方法
    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}