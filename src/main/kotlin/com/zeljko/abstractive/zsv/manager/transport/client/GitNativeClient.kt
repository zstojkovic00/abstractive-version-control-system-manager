package com.zeljko.abstractive.zsv.manager.transport.client

import com.zeljko.abstractive.zsv.manager.transport.model.GitReference
import com.zeljko.abstractive.zsv.manager.transport.model.GitUrl
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

    fun connect(gitUrl: GitUrl) {
        Socket(gitUrl.host, gitUrl.port).use { socket ->
            val input = DataInputStream(socket.inputStream)
            val output = DataOutputStream(socket.outputStream)

            sendRefDiscoveryRequest(output, gitUrl)
            val references = readPacketReferenceResponse(input)
            println(references)

            val headSha = references.first().sha
            sendWantRequest(output, headSha)
            readPackFile(input)
        }
    }

    private fun sendRefDiscoveryRequest(output: DataOutputStream, gitUrl: GitUrl) {
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

    private fun sendWantRequest(output: DataOutputStream, headSha: String) {
        val packet = buildString {
            append(createPacket {
                append(GIT_WANT)
                    .append(" ")
                    .append(headSha)
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
        return "$hexLength$command"
    }


    private fun readPackFile(input: DataInputStream) {
        // TODO:  Read objects
    }

    private fun readPacketReferenceResponse(input: DataInputStream): List<GitReference> {
        val responseBuilder = StringBuilder()

        while (true) {
            val lengthHex = input.readNBytes(4).toString(StandardCharsets.UTF_8)
            if (lengthHex == FLUSH_PACKET) break

            val length = lengthHex.toInt(16) - 4
            if (length > 0) {
                val responseData = input.readNBytes(length)
                val response = responseData.toString(StandardCharsets.UTF_8)
                responseBuilder.append(response)
            }
        }

        if (responseBuilder.isEmpty()) {
            return listOf()
        }

        return parseReferences(responseBuilder.toString())
    }

    // f490f44a26417c31c44174e12fdbc5a53a761082 HEAD\u0000multi_ack thin-pack side-band side-band-64k ofs-delta shallow deepen-since deepen-not deepen-relative no-progress include-tag multi_ack_detailed symref=HEAD:refs/heads/master object-format=sha1 agent=git/2.34.1
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/heads/master
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/remotes/origin/master
    private fun parseReferences(response: String): List<GitReference> {
        return response.lines()
            .filter { it.isNotEmpty() }
            .map { line ->
                val (sha, rest) = line.split(" ", limit = 2)
                if (rest.contains(NUL_BYTE)) {
                    val (refName, capsString) = rest.split(NUL_BYTE, limit = 2)
                    val capabilities = capsString.split(" ")
                    GitReference(sha, refName, capabilities)
                } else {
                    GitReference(sha, rest)
                }
            }
    }
}