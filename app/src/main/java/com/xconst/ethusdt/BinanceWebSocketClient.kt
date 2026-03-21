package com.xconst.ethusdt

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BinanceWebSocketClient(   // 即使这个class不针对币安, 但我依然使用这个标识符
    private val onPrice: (Double) -> Unit,
    private val onSocketState: (SocketStatus) -> Unit
) {

    private val TAG = "OKX_WS"

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var reconnectJob: Job? = null
    private val manuallyClosed = AtomicBoolean(false)
    private var reconnectDelayMs = 1000L

    fun connect() {

        Log.d(TAG, "Connecting to OKX websocket...")

        manuallyClosed.set(false)
        onSocketState(SocketStatus.CONNECTING)

        val request = Request.Builder()
            .url("wss://ws.okx.com:8443/ws/v5/public")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {

                Log.d(TAG, "WebSocket connected")

                reconnectDelayMs = 1000L

                onSocketState(SocketStatus.CONNECTED)

                subscribe()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {

                Log.d(TAG, "RAW: $text")

                runCatching {

                    val obj = JSONObject(text)

                    if (!obj.has("data")) return

                    val data = obj.getJSONArray("data")
                    val price = data
                        .getJSONObject(0)
                        .getString("markPx")
                        .toDouble()

                    Log.d(TAG, "ETH price = $price")

                    onPrice(price)

                }.onFailure {

                    Log.e(TAG, "parse error", it)

                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {

                Log.d(TAG, "WebSocket closed")

                onSocketState(SocketStatus.DISCONNECTED)

                if (!manuallyClosed.get()) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {

                Log.e(TAG, "WebSocket failure", t)

                onSocketState(SocketStatus.RECONNECTING)

                if (!manuallyClosed.get()) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun subscribe() {

        val sub = """
        {
          "op": "subscribe",
          "args": [
            {
              "channel": "mark-price",
              "instId": "ETH-USDT-SWAP"
            }
          ]
        }
        """.trimIndent()

        Log.d(TAG, "Subscribe: $sub")

        webSocket?.send(sub)
    }

    private fun scheduleReconnect() {

        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {

            Log.d(TAG, "Reconnect in $reconnectDelayMs ms")

            delay(reconnectDelayMs)

            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)

            connect()
        }
    }

    fun close() {

        manuallyClosed.set(true)

        reconnectJob?.cancel()

        webSocket?.close(1000, "manual close")

        webSocket = null

        onSocketState(SocketStatus.DISCONNECTED)
    }
}