package com.nova.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AiClient {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-3-5-sonnet-20241022"

    fun chat(apiKey: String, userText: String): String {
        return try {
            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("anthropic-version", "2023-06-01")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 300)
                put("system", "You are Nova, a helpful voice assistant. Reply in short, spoken-friendly sentences, mixing Roman Urdu and English the way the user speaks.")
                put("messages", JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userText)
                    }
                ))
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream.bufferedReader().use { it.readText() }

            if (responseCode !in 200..299) {
                return "Nova ko jawab nahi mila (error $responseCode)"
            }

            val json = JSONObject(responseText)
            val content = json.optJSONArray("content")
            val textBlock = content?.let { arr ->
                (0 until arr.length())
                    .map { arr.getJSONObject(it) }
                    .firstOrNull { it.optString("type") == "text" }
            }
            textBlock?.optString("text")?.takeIf { it.isNotBlank() } ?: "Koi jawab nahi mila"
        } catch (e: Exception) {
            "Internet ya API mein masla hai: ${e.message}"
        }
    }
}
