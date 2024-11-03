package com.zeljko.abstractive.zsv.manager.transport.client

import com.zeljko.abstractive.zsv.manager.transport.model.GitReference
import com.zeljko.abstractive.zsv.manager.transport.model.GitUrl
import com.zeljko.abstractive.zsv.manager.utils.zlibDecompress
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets

class GitNativeClient {
    companion object {
        private const val FLUSH_PACKET = "0000"
        private const val NUL_BYTE = '\u0000'
        private const val GIT_UPLOAD_PACK = "git-upload-pack"
        private const val GIT_WANT = "want"
        private const val GIT_DONE = "done"
        private const val NEWLINE = "\n"
    }

    fun clone(gitUrl: GitUrl) {
        Socket(gitUrl.host, gitUrl.port).use { socket ->
            val input = DataInputStream(socket.inputStream)
            val output = DataOutputStream(socket.outputStream)

            sendRefDiscoveryRequest(output, gitUrl)
            val references = readRefDiscoveryResponse(input)

            sendWantRequest(output, references)
            val packByteArray = readWantResponse(input)
            parsePack(packByteArray);

        }
    }

    private fun parsePack(packByteArray: ByteArray) {
        val input = DataInputStream(ByteArrayInputStream(packByteArray))
        val magicByte = input.readNBytes(4).toString(StandardCharsets.UTF_8);

        if (magicByte != "PACK") {
            throw IllegalStateException("Something is not right, missing PACK signature :)")
        }

        val version = input.readInt()
        val nObjects = input.readInt()

        println("Pack file version: $version")
        println("Number of git objects: $nObjects")

        for (i in 1..nObjects) {
            // reading one byte (8 bits)
            val objectHeader = input.read()

            // take first 3 bits by shifting right 4
            val type = (objectHeader shr 4) and 7
            /*
                OBJ_COMMIT = 1
                OBJ_TREE = 2
                OBJ_BLOB = 3
                OBJ_TAG = 4
                OBJ_OFS_DELTA = 6
                OBJ_REF_DELTA = 7
             */
            println("Object type code $type")

            // take last 4 bits by masking with 15
            // partial size if the MSB is set and more bytes need to be read
            var size = objectHeader and 15
            var shift = 4
            var currentByte = objectHeader

            while (currentByte and 0x80 != 0) {
                currentByte = input.read()
                // take last 7 bits (0111 1111 = 0x7F)
                val add = (currentByte) and 0x7F
                size += add shl shift
                shift += 7
            }
            println("Object size $size")

            // git is giving us size of decompressed file not compressed
            when (type) {
                1, 2, 3 -> {
                    val decompressedData = input.readNBytes(size).zlibDecompress()
                    println(decompressedData.toString(StandardCharsets.UTF_8))
                }
            }

        }

    }


    private fun sendRefDiscoveryRequest(output: DataOutputStream, gitUrl: GitUrl) {
        // 002egit-upload-pack /test-repohost=127.0.0.1
        val packet = createPacket {
            append(GIT_UPLOAD_PACK)
                .append(" ")
                .append(gitUrl.path)
                .append(NUL_BYTE)
                .append("host=")
                .append(gitUrl.host)
                .append(NUL_BYTE)
        }
        output.writeBytes(packet)
        output.flush()
    }

    private fun sendWantRequest(output: DataOutputStream, references: List<GitReference>) {
        val requiredCapabilities = references.first().capabilities?.filter { capability ->
            when (capability) {
                "multi_ack", "side-band-64k", "thin-pack", "agent=git/2.34.1", "ofs-delta" -> true
                else -> false
            }
        }?.joinToString(" ")

        // 005ewant 6d275e323ac3397b78002f28fb2bb8e291b0b795 multi_ack thin-pack side-band-64k ofs-delta
        // FLUSH_PACKET
        // 0009done
        val packet = buildString {
            append(createPacket {
                append(GIT_WANT)
                    .append(" ")
                    .append(references.first().sha)
                    .append(" ")
                    .append(requiredCapabilities)
                    .append(NEWLINE)
            })
            append(FLUSH_PACKET)
            append(createPacket {
                append(GIT_DONE)
                append(NEWLINE)
            })
        }
        output.writeBytes(packet)
        output.flush()
    }

    private fun createPacket(buildCommand: StringBuilder.() -> Unit): String {
        val command = buildString(buildCommand)
        val length = command.length + 4
        val hexLength = String.format("%04x", length)
        println("$hexLength$command")
        return "$hexLength$command"
    }

    private fun readWantResponse(input: DataInputStream): ByteArray {
        val packByteArrayOutput = ByteArrayOutputStream()

        while (true) {
            val lengthHex = input.readNBytes(4).toString(StandardCharsets.UTF_8)
            if (lengthHex == FLUSH_PACKET) break

            val length = lengthHex.toInt(16) - 4
            if (length > 0) {
                val data = input.readNBytes(length)
                val channel = data[0]

                /*
                                PACK_DATA_CHANNEL: Byte = 0x01
                                PROGRESS_CHANNEL: Byte = 0x02
                                ERROR_CHANNEL: Byte = 0x03
                                                                    */
                when (channel) {
                    0x01.toByte() -> {
                        // skip first byte
                        packByteArrayOutput.write(data, 1, data.size - 1)
                    }

                    0x02.toByte() -> {
                        println("Progress: ${String(data, 1, data.size - 1)}")
                    }

                    0x03.toByte() -> {
                        println("Error: ${String(data, 1, data.size - 1)}")
                    }
                }
            }
        }

        return packByteArrayOutput.toByteArray()
    }

    // f490f44a26417c31c44174e12fdbc5a53a761082 HEAD\u0000multi_ack thin-pack side-band side-band-64k ofs-delta shallow deepen-since deepen-not deepen-relative no-progress include-tag multi_ack_detailed symref=HEAD:refs/heads/master object-format=sha1 agent=git/2.34.1
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/heads/master
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/remotes/origin/master
    private fun readRefDiscoveryResponse(input: DataInputStream): List<GitReference> {
        val references = mutableListOf<GitReference>()

        while (true) {
            val lengthHex = input.readNBytes(4).toString(StandardCharsets.UTF_8)
            if (lengthHex == FLUSH_PACKET) {
                break
            }

            val length = lengthHex.toInt(16) - 4
            if (length > 0) {
                val data = input.readNBytes(length)
                val line = String(data, StandardCharsets.UTF_8)

                if (line.contains(NUL_BYTE)) {
                    val (sha, rest) = line.split(" ", limit = 2)
                    val (refName, capsString) = rest.split(NUL_BYTE, limit = 2)
                    val capabilities = capsString.split(" ")
                    references.add(GitReference(sha, refName, capabilities))
                } else {
                    val (sha, refName) = line.split(" ", limit = 2)
                    references.add(GitReference(sha, refName))
                }
            }
        }
        return references
    }
}