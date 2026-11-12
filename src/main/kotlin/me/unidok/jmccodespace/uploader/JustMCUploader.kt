package me.unidok.jmccodespace.uploader

import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.unidok.jmccodespace.JMCCodespace
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object JustMCUploader : ModuleUploader {
    override fun upload(data: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI("https://m.justmc.ru/api/upload"))
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .timeout(Duration.ofSeconds(30))
            .build()
        val response = JMCCodespace.httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        val body = Json.parseToJsonElement(response.body()).jsonObject
        val statusCode = response.statusCode()
        if (statusCode in 200..299) return "https://m.justmc.ru/api/" + body["id"]!!.jsonPrimitive.content
        throw IOException("HTTP $statusCode: $body")
    }
}