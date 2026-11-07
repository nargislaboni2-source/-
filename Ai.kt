package com.ss.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenAI-compatible chat client (works with Grok / xAI by default). Key comes from encrypted storage. */
object AiClient {
    private const val SYSTEM =
        "You are SS AI, the assistant inside SS Bedrock Client, an Android companion app for Minecraft Bedrock Edition. " +
            "Help with Bedrock add-ons, JSON templates, resource packs, troubleshooting and explaining app features. " +
            "Reply in the same language the user writes in (English or Bengali). Be honest about what an Android app can and cannot do inside Minecraft."

    private fun open(url: String, method: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 60000
        c.setRequestProperty("Authorization", "Bearer ${Prefs.apiKey().trim()}")
        c.setRequestProperty("Content-Type", "application/json")
        return c
    }

    private fun body(c: HttpURLConnection): String {
        val s = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        return s?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun base(): String {
        val b = Prefs.aiEndpoint().trim().trimEnd('/')
        require(b.isNotBlank()) { "API endpoint is empty" }
        require(Prefs.apiKey().isNotBlank()) { "API key is empty" }
        return b
    }

    suspend fun test(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val c = open("${base()}/models", "GET")
            val code = c.responseCode
            val txt = body(c)
            c.disconnect()
            if (code !in 200..299) error("HTTP $code ${txt.take(160)}")
            "OK"
        }
    }

    suspend fun chat(history: List<Pair<String, String>>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val b = base()
            val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM))
            history.forEach { msgs.put(JSONObject().put("role", it.first).put("content", it.second)) }
            val payload = JSONObject().put("model", Prefs.aiModel()).put("messages", msgs).toString()
            val c = open("$b/chat/completions", "POST")
            c.doOutput = true
            c.outputStream.use { it.write(payload.toByteArray()) }
            val code = c.responseCode
            val txt = body(c)
            c.disconnect()
            if (code !in 200..299) error("HTTP $code ${txt.take(200)}")
            JSONObject(txt).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }
}
