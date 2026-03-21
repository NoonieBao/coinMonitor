package com.xconst.ethusdt

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val store = MutableStateFlow<List<AlertCondition>>(emptyList())
    }

    val conditions: StateFlow<List<AlertCondition>> = store.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        scope.launch {
            load()
        }
    }

    fun addCondition(symbol: Symbol, direction: Direction, value: Double) {

        val item = AlertCondition(
//            id = Random.nextLong(),
            id = System.currentTimeMillis(),
            symbol = symbol,
            direction = direction,
            targetPrice = value,
            enabled = true
        )

        store.value = store.value + item

        save()
    }

    fun deleteCondition(id: Long) {

        store.value = store.value.filterNot { it.id == id }

        save()
    }

    fun toggleCondition(id: Long) {

        store.value = store.value.map {
            if (it.id == id) it.copy(enabled = !it.enabled) else it
        }

        save()
    }

    fun getAll(): List<AlertCondition> = store.value

    fun updateTriggered(id: Long, triggered: Boolean) {

        store.value = store.value.map {
            if (it.id == id) it.copy(triggered = triggered) else it
        }

        save()
    }

    // ===== persistence =====

    private fun save0() {
        scope.launch {

            val text = json.encodeToString(store.value)

            appContext
                .getSharedPreferences("alert_conditions", Context.MODE_PRIVATE)
                .edit()
                .putString("data", text)
                .apply()
        }
    }

    private fun save() {
        scope.launch {

            runCatching {

                val text = json.encodeToString(store.value)

                appContext
                    .getSharedPreferences("alert_conditions", Context.MODE_PRIVATE)
                    .edit()
                    .putString("data", text)
                    .apply()

            }.onFailure {
                android.util.Log.e("AppRepository", "save error", it)
            }
        }
    }

    private suspend fun load() {

        val text = appContext
            .getSharedPreferences("alert_conditions", Context.MODE_PRIVATE)
            .getString("data", null)

        if (text != null) {
            runCatching {
                val list = json.decodeFromString<List<AlertCondition>>(text)
                store.value = list
            }
        }
    }
}