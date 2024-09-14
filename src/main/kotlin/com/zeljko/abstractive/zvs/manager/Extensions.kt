package com.zeljko.abstractive.zvs.manager

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

fun ByteArray.zlibDecompress(): String {
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()

    val decompressedString = outputStream.use {
        val buffer = ByteArray(1024)

        inflater.setInput(this)

        var count = -1
        while (count != 0) {
            count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }

        inflater.end()
        outputStream.toString("UTF-8")
    }

    // replace nul (unicode representation of ASCII code 0) with \0
    return decompressedString.replace("\u0000", "\\0")
}