package com.xconst.ethusdt

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OkxHttpClient(
    private val symbols: List<Symbol>,
    private val baseUrl: String, // 👉 你的服务器地址，例如 http://xxx.xxx.xxx.xxx:8000
    private val onPrice: (Symbol, Double) -> Unit,
    private val onState: (SocketStatus) -> Unit
) {
    private val tag = "OKX_HTTP"

    private var lastSuccessTs = 0L
    private val timeoutMs = 5000L // 超过3秒没成功就认为断了

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private val running = AtomicBoolean(false)

    // 👉 轮询间隔（毫秒）
    private var intervalMs = 1000L

    val API_KEY = "your_key"
    val SECRET = "your_secret"

    fun md5(input: String): String {
        val bytes = java.security.MessageDigest
            .getInstance("MD5")
            .digest(input.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    fun start() {
        if (running.get()) return

        running.set(true)
        onState(SocketStatus.CONNECTING)

        job = scope.launch {
            onState(SocketStatus.CONNECTED)

            while (running.get()) {
                try {
                    fetchPrices()
                    lastSuccessTs = System.currentTimeMillis()

                    // 👉 如果之前是断线，现在恢复
                    onState(SocketStatus.CONNECTED)

                } catch (e: Exception) {
                    Log.e(tag, "fetch error", e)
                }

                // 👉 存活检测（关键）
                val now = System.currentTimeMillis()
                if (now - lastSuccessTs > timeoutMs) {
                    onState(SocketStatus.RECONNECTING)
                }

                delay(intervalMs)
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        onState(SocketStatus.DISCONNECTED)
    }

    private fun fetchPrices() {
        // 👉 拼接批量接口
        val instIds = symbols.joinToString(",") { it.okxInstId }
        // 当前时间（秒）
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // 生成 sign
        val raw = API_KEY + timestamp + SECRET
        val sign = md5(raw)

        val url = "$baseUrl/api/prices?instIds=$instIds&key=$API_KEY&ts=$timestamp&sign=$sign"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body?.string() ?: return

            val json = JSONObject(body)
            val data: JSONArray = json.getJSONArray("data")

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)

                val instId = item.getString("instId")
                val price = item.getDouble("price")

                val symbol = Symbol.fromInstId(instId) ?: continue

                onPrice(symbol, price)
            }
        }
    }
}