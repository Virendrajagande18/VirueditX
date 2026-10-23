package com.virueditx.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One recorded conversion (spec allows Room "or another appropriate local storage" — this is a small JSON store). */
data class ConversionRecord(
    val id: String,
    val tool: String,
    val inputName: String,
    val outputName: String,
    val outputPath: String,
    val success: Boolean,
    val message: String,
    val timestamp: Long
)

object HistoryStore {
    private const val FILE_NAME = "history.json"
    private const val MAX_RECORDS = 200

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun load(context: Context): List<ConversionRecord> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ConversionRecord(
                    id = o.optString("id"),
                    tool = o.optString("tool"),
                    inputName = o.optString("inputName"),
                    outputName = o.optString("outputName"),
                    outputPath = o.optString("outputPath"),
                    success = o.optBoolean("success"),
                    message = o.optString("message"),
                    timestamp = o.optLong("timestamp")
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(context: Context, record: ConversionRecord) {
        val all = (listOf(record) + load(context)).take(MAX_RECORDS)
        val arr = JSONArray()
        all.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("tool", r.tool)
                    .put("inputName", r.inputName)
                    .put("outputName", r.outputName)
                    .put("outputPath", r.outputPath)
                    .put("success", r.success)
                    .put("message", r.message)
                    .put("timestamp", r.timestamp)
            )
        }
        try {
            file(context).writeText(arr.toString())
        } catch (e: Exception) {
            // History is best-effort; a failed history write must never fail a conversion.
        }
    }

    @Synchronized
    fun clear(context: Context) {
        try {
            file(context).delete()
        } catch (e: Exception) {
            // ignore
        }
    }
}
