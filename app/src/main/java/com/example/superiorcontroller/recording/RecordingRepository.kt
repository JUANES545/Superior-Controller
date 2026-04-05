package com.example.superiorcontroller.recording

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RecordingRepository(context: Context) {

    private val dir: File = File(context.filesDir, "recordings").also { it.mkdirs() }

    fun save(data: RecordingData) {
        val json = JSONObject().apply {
            put("id", data.id)
            put("name", data.name)
            put("createdAt", data.createdAt)
            put("durationMs", data.durationMs)
            data.initialSnapshot?.let { snap ->
                put("snapshot", JSONObject().apply {
                    put("buttons", snap.buttons)
                    put("dpad", snap.dpad)
                    put("leftX", snap.leftX.toDouble())
                    put("leftY", snap.leftY.toDouble())
                    put("rightX", snap.rightX.toDouble())
                    put("rightY", snap.rightY.toDouble())
                    put("leftTrigger", snap.leftTrigger.toDouble())
                    put("rightTrigger", snap.rightTrigger.toDouble())
                })
            }
            val eventsArr = JSONArray()
            data.events.forEach { e ->
                eventsArr.put(JSONObject().apply {
                    put("t", e.t)
                    put("type", e.type)
                    put("btn", e.btn)
                    put("x", e.x.toDouble())
                    put("y", e.y.toDouble())
                })
            }
            put("events", eventsArr)
        }
        File(dir, "${data.id}.json").writeText(json.toString())
    }

    fun loadAll(): List<RecordingMeta> {
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val obj = JSONObject(file.readText())
                    RecordingMeta(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        createdAt = obj.getLong("createdAt"),
                        durationMs = obj.getLong("durationMs"),
                        eventCount = obj.getJSONArray("events").length()
                    )
                } catch (_: Exception) { null }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun load(id: String): RecordingData? {
        val file = File(dir, "$id.json")
        if (!file.exists()) return null
        return try {
            val obj = JSONObject(file.readText())
            val eventsArr = obj.getJSONArray("events")
            val events = (0 until eventsArr.length()).map { i ->
                val e = eventsArr.getJSONObject(i)
                RecordedEvent(
                    t = e.getLong("t"),
                    type = e.getInt("type"),
                    btn = e.optInt("btn", 0),
                    x = e.optDouble("x", 0.0).toFloat(),
                    y = e.optDouble("y", 0.0).toFloat()
                )
            }
            val snap = obj.optJSONObject("snapshot")?.let { s ->
                GamepadSnapshot(
                    buttons = s.optInt("buttons"), dpad = s.optInt("dpad"),
                    leftX = s.optDouble("leftX").toFloat(),
                    leftY = s.optDouble("leftY").toFloat(),
                    rightX = s.optDouble("rightX").toFloat(),
                    rightY = s.optDouble("rightY").toFloat(),
                    leftTrigger = s.optDouble("leftTrigger").toFloat(),
                    rightTrigger = s.optDouble("rightTrigger").toFloat()
                )
            }
            RecordingData(
                id = obj.getString("id"),
                name = obj.getString("name"),
                createdAt = obj.getLong("createdAt"),
                durationMs = obj.getLong("durationMs"),
                events = events,
                initialSnapshot = snap
            )
        } catch (_: Exception) { null }
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    fun rename(id: String, newName: String) {
        val file = File(dir, "$id.json")
        if (!file.exists()) return
        try {
            val obj = JSONObject(file.readText())
            obj.put("name", newName)
            file.writeText(obj.toString())
        } catch (_: Exception) { }
    }
}
