package com.example.superiorcontroller.bluetooth

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class KnownDevicesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("known_devices", Context.MODE_PRIVATE)

    fun loadAll(): List<KnownDevice> {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                KnownDevice(
                    name = obj.getString("name"),
                    alias = obj.optString("alias", ""),
                    address = obj.getString("address"),
                    lastUsedAt = obj.optLong("lastUsedAt", 0L)
                )
            }.sortedByDescending { it.lastUsedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(device: KnownDevice) {
        val devices = loadAll().toMutableList()
        devices.removeAll { it.address == device.address }
        val existing = loadAll().find { it.address == device.address }
        val merged = device.copy(alias = existing?.alias ?: device.alias)
        devices.add(0, merged)
        persist(devices.take(MAX_DEVICES))
    }

    fun rename(address: String, newAlias: String) {
        val devices = loadAll().map {
            if (it.address == address) it.copy(alias = newAlias) else it
        }
        persist(devices)
    }

    fun remove(address: String) {
        persist(loadAll().filter { it.address != address })
    }

    fun getLastUsed(): KnownDevice? = loadAll().firstOrNull()

    private fun persist(devices: List<KnownDevice>) {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().apply {
                put("name", d.name)
                put("alias", d.alias)
                put("address", d.address)
                put("lastUsedAt", d.lastUsedAt)
            })
        }
        prefs.edit().putString(KEY_DEVICES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_DEVICES = "devices_json"
        private const val MAX_DEVICES = 20
    }
}
