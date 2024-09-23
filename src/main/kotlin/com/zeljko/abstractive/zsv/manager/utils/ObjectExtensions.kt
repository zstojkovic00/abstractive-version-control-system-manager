package com.zeljko.abstractive.zsv.manager.utils

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater


/**
 * Decompress a byte array using ZLIB.
 *
 * @return an byte array.
 */
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


/**
 * Compress a string using ZLIB.
 *
 * @return an UTF-8 encoded byte array.
 */
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


/**
 * Compress a ByteArray using ZLIB.
 *
 * @return a compressed ByteArray.
 */
fun ByteArray.zlibCompress(): ByteArray {
    val output = ByteArray(this.size * 2)
    val compressor = Deflater().apply {
        setInput(this@zlibCompress)
        finish()
    }
    val compressedDataLength: Int = compressor.deflate(output)
    return output.copyOfRange(0, compressedDataLength)
}


/**
 * Encrypt String to SHA1 format
 */
fun String.toSha1(): String {
    return MessageDigest
        .getInstance("SHA-1")
        .digest(this.toByteArray())
        .joinToString(separator = "", transform = { "%02x".format(it) })
}


/**
 * Calculate SHA1 hash of a ByteArray
 *
 * @return SHA1 hash as a String
 */
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