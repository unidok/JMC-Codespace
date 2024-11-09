package me.unidok.jmccodespace.util

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object Compressor {
    fun compress(src: String): ByteArray {
        val input = src.toByteArray()
        val output = ByteArray(input.size * 4)
        val compressor = Deflater()
        compressor.setInput(input)
        compressor.finish()

        val compressedDataLength = compressor.deflate(output)
        return output.copyOfRange(0, compressedDataLength)
    }

    fun decompress(input: ByteArray): String {
        val inflater = Inflater()
        val outputStream = ByteArrayOutputStream()

        return outputStream.use {
            val buffer = ByteArray(1024)

            inflater.setInput(input)

            var count = -1
            while (count != 0) {
                count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }

            inflater.end()
            outputStream.toString(Charsets.UTF_8)
        }
    }
}
