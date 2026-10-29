package com.lextezy.tambiolo.data

import android.content.Context
import com.lextezy.tambiolo.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Downloads results.json and keeps a copy on the phone so results show offline. */
class ResultsRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "results.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun cached(): ResultsFile? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        runCatching { json.decodeFromString<ResultsFile>(cacheFile.readText()) }.getOrNull()
    }

    suspend fun refresh(): ResultsFile = withContext(Dispatchers.IO) {
        val conn = URL(Config.RESULTS_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Cache-Control", "no-cache")
            if (conn.responseCode != 200) error("Server returned ${conn.responseCode}")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString<ResultsFile>(text)
            cacheFile.writeText(text)
            parsed
        } finally {
            conn.disconnect()
        }
    }
}
