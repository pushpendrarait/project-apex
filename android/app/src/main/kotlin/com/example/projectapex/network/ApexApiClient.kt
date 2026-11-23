package com.example.projectapex.network

import com.example.projectapex.model.SportType
import com.example.projectapex.model.TimeSlot
import com.example.projectapex.model.Venue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApexApiClient(
    var baseUrl: String = "http://10.0.2.2:8080"
) {

    suspend fun pingBackend(customUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        val target = (customUrl ?: baseUrl).trimEnd('/') + "/api/v1/sports"
        try {
            val url = URL(target)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getAvailableSports(): List<String> = withContext(Dispatchers.IO) {
        val endpoint = "${baseUrl.trimEnd('/')}/api/v1/sports"
        try {
            val res = httpGet(endpoint)
            val jsonArr = JSONArray(res)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArr.length()) {
                list.add(jsonArr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            throw RuntimeException("HTTP error $code")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        conn.disconnect()
        return sb.toString()
    }

    private fun httpPost(urlStr: String, body: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(body)
        writer.flush()
        writer.close()

        val code = conn.responseCode
        val isSuccess = code in 200..299
        val inputStream = if (isSuccess) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(inputStream ?: conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        conn.disconnect()

        if (!isSuccess) {
            throw RuntimeException("HTTP $code: $sb")
        }
        return sb.toString()
    }
}
