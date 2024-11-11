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

fun DataInputStream.MSB(): Pair<Int, Int> {
    val byte = read() and 0xFF
    // Extract object type from the first byte:
    // Shift right by 4 to get first 3 bits and mask with 7 (0111) to get type
    val type = (byte shr 4) and 7
    println("Object type code $type")

    // Extract initial size from remaining 4 bits of first byte
    var size = byte and 15
    var shift = 4

    // Read variable-length size encoding:
    // While MSB (Most Significant Bit) is 1, continue reading size bytes
    var currentByte = byte
    while ((currentByte and 0x80) != 0) {
        currentByte = read() and 0xFF
        size += (currentByte and 0x7F) shl shift
        shift += 7
    }
    println("Object size: $size")
    return Pair(size, type)
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