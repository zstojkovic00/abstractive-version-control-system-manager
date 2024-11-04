package com.zeljko.abstractive.zsv.manager.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater

fun ByteArray.zlibDecompress(): ByteArray {
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()

    return outputStream.use {
        val buffer = ByteArray(1024)

        inflater.setInput(this)

        var count = -1
        while (count != 0) {
            count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }

        inflater.end()
        outputStream.toByteArray()
    }
}

fun DataInputStream.zlibDecompress(size: Int): Pair<ByteArray, DataInputStream> {
    val remainingBytesBeforeDecompression = this.available()
    val inflater = Inflater()

    val compressed = this.readAllBytes()
    inflater.setInput(compressed)

    val decompressed = ByteArray(size)
    inflater.inflate(decompressed)

    val unusedBytesCount = remainingBytesBeforeDecompression - inflater.totalIn
    val newInput = if (unusedBytesCount > 0) {
        DataInputStream(ByteArrayInputStream(
            compressed.sliceArray(inflater.totalIn until compressed.size)
        ))
    } else {
        this
    }

    inflater.end()
    return Pair(decompressed, newInput)
}

fun String.zlibCompress(): ByteArray {
    val input = this.toByteArray(charset("UTF-8"))

    // Compress the bytes
    // 1 to 4 bytes/char for UTF-8
    val output = ByteArray(input.size * 4)
    val compressor = Deflater().apply {
        setInput(input)
        finish()
    }
    val compressedDataLength: Int = compressor.deflate(output)
    return output.copyOfRange(0, compressedDataLength)
}


fun ByteArray.zlibCompress(): ByteArray {
    val output = ByteArray(this.size * 2)
    val compressor = Deflater().apply {
        setInput(this@zlibCompress)
        finish()
    }
    val compressedDataLength: Int = compressor.deflate(output)
    return output.copyOfRange(0, compressedDataLength)
}


fun String.toSha1(): String {
    return MessageDigest
        .getInstance("SHA-1")
        .digest(this.toByteArray())
        .joinToString(separator = "", transform = { "%02x".format(it) })
}

fun ByteArray.toSha1(): String {
    return MessageDigest
        .getInstance("SHA-1")
        .digest(this)
        .joinToString(separator = "", transform = { "%02x".format(it) })
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun String.toShaByte(): ByteArray {
    return ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}