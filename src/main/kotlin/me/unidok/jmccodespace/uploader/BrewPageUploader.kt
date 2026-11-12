package me.unidok.jmccodespace.uploader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.unidok.jmccodespace.JMCCodespace
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object BrewPageUploader : ModuleUploader {
    override fun upload(data: String): String {
        val fileBytes = data.toByteArray(Charsets.UTF_8)
        val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"

        // Согласно документации BrewPage, имя поля должно быть "file"
        // Параметр ns управляет пространством имён ссылки, можно изменить на пользовательский
        val bodyBytes = ByteArrayOutputStream().use { output ->
            val writer = output.writer(Charsets.UTF_8)

            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"${JMCCodespace.getUniqueId()}.json\"\r\n")
            writer.write("Content-Type: application/json\r\n\r\n")
            writer.flush()

            output.write(fileBytes)

            writer.write("\r\n--$boundary--\r\n")
            writer.flush()

            output.toByteArray()
        }

        // ns=uploads — это пример пространства имён, можно заменить на своё
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://brewpage.app/api/files?ns=uploads"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = JMCCodespace.httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val statusCode = response.statusCode()
        val responseBody = response.body()

        JMCCodespace.logger.info("BrewPage upload status: $statusCode, body: $responseBody")

        if (statusCode in 200..299) {
            // BrewPage возвращает JSON, ссылка находится в поле "link"
            val json = Json.parseToJsonElement(responseBody).jsonObject
            val link = json["link"]?.jsonPrimitive?.content
                ?: throw IOException("BrewPage response missing 'link': $responseBody")
            return link
        }

        throw IOException("BrewPage HTTP $statusCode: $responseBody")
    }
}