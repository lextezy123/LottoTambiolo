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

/** Downloads the results files and keeps copies on the phone so results show offline. */
class ResultsRepository(context: Context) {
    private val lottoFile = File(context.filesDir, "results.json")
    private val digitsFile = File(context.filesDir, "digits.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun cached(): ResultsFile? = withContext(Dispatchers.IO) {
        if (!lottoFile.exists()) return@withContext null
        runCatching { json.decodeFromString<ResultsFile>(lottoFile.readText()) }.getOrNull()
    }

    suspend fun refresh(): ResultsFile = withContext(Dispatchers.IO) {
        json.decodeFromString<ResultsFile>(download(Config.RESULTS_URL, lottoFile))
    }

    suspend fun cachedDigits(): DigitsFile? = withContext(Dispatchers.IO) {
        if (!digitsFile.exists()) return@withContext null
        runCatching { json.decodeFromString<DigitsFile>(digitsFile.readText()) }.getOrNull()
    }

    suspend fun refreshDigits(): DigitsFile = withContext(Dispatchers.IO) {
        json.decodeFromString<DigitsFile>(download(Config.DIGITS_URL, digitsFile))
    }

    /**
     * Returns the file's text, downloading it only if it changed since last time.
     * The server's ETag is saved next to the cached copy; when it still matches,
     * the server answers 304 with no body and the cached copy is used.
     */
    private fun download(url: String, cache: File): String {
        val etagFile = File(cache.path + ".etag")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            if (cache.exists() && etagFile.exists()) {
                conn.setRequestProperty("If-None-Match", etagFile.readText())
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return cache.readText()
            if (code != HttpURLConnection.HTTP_OK) error("Server returned $code")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            // Write to a temp file first so a dropped connection never leaves a half-written cache.
            val tmp = File(cache.path + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(cache)) cache.writeText(text)
            val etag = conn.getHeaderField("ETag")
            if (etag != null) etagFile.writeText(etag) else etagFile.delete()
            return text
        } finally {
            conn.disconnect()
        }
    }
}
