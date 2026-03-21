package com.xconst.ethusdt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OkxWebSocketClient(
    private val symbols: List<Symbol>,
    private val onPrice: (Symbol, Double) -> Unit,
    private val onSocketState: (SocketStatus) -> Unit
) {
    private val tag = "OKX_WS"

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private val manuallyClosed = AtomicBoolean(false)
    private var reconnectDelayMs = 1000L

    fun connect() {
        Log.d(tag, "Connecting to OKX websocket...")

        manuallyClosed.set(false)
        onSocketState(SocketStatus.CONNECTING)

        val request = Request.Builder()
            .url("wss://ws.okx.com:8443/ws/v5/public")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connected")
                reconnectDelayMs = 1000L
                onSocketState(SocketStatus.CONNECTED)
                subscribe()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "RAW: $text")

                runCatching {
                    val obj = JSONObject(text)

                    if (!obj.has("data")) return
                    val dataArray = obj.getJSONArray("data")
                    if (dataArray.length() == 0) return

                    val item = dataArray.getJSONObject(0)
                    val instId = item.optString("instId")
                    val markPx = item.optString("markPx")

                    if (instId.isBlank() || markPx.isBlank()) return

                    val symbol = Symbol.fromInstId(instId) ?: return
                    val price = markPx.toDouble()

                    Log.d(tag, "symbol=$symbol, price=$price")
                    onPrice(symbol, price)
                }.onFailure {
                    Log.e(tag, "parse error", it)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: $code / $reason")
                onSocketState(SocketStatus.DISCONNECTED)
                if (!manuallyClosed.get()) scheduleReconnect()
            }


            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {

                Log.e(tag, "WebSocket failure: ${t.message}", t)

                response?.let {
                    Log.e(tag, "HTTP code = ${it.code}")
                    Log.e(tag, "HTTP message = ${it.message}")
                    Log.e(tag, "HTTP headers = ${it.headers}")
                }

                onSocketState(SocketStatus.RECONNECTING)

                if (!manuallyClosed.get()) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun subscribe() {
        val args = JSONArray()
        symbols.forEach { symbol ->
            args.put(
                JSONObject().apply {
                    put("channel", "mark-price")
                    put("instId", symbol.okxInstId)
                }
            )
        }

        val sub = JSONObject().apply {
            put("op", "subscribe")
            put("args", args)
        }.toString()

        Log.d(tag, "Subscribe: $sub")
        webSocket?.send(sub)
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            Log.d(tag, "Reconnect in $reconnectDelayMs ms")
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(40_000L)
            connect()
        }
    }

    public fun resetWebSocket(){
        reconnectDelayMs = 1000L
    }

    fun close() {
        manuallyClosed.set(true)
        reconnectJob?.cancel()
        webSocket?.close(1000, "manual close")
        webSocket = null
        onSocketState(SocketStatus.DISCONNECTED)
    }
}