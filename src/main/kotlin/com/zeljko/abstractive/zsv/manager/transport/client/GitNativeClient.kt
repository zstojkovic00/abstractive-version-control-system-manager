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
    }

    fun connect(gitUrl: GitUrl) {
        Socket(gitUrl.host, gitUrl.port).use { socket ->
            val input = DataInputStream(socket.inputStream)
            val output = DataOutputStream(socket.outputStream)

            sendRefDiscoveryRequest(output, gitUrl)
            readServerResponses(input)
            terminateConnection(output)
        }
    }

    private fun sendRefDiscoveryRequest(output: DataOutputStream, gitUrl: GitUrl) {
        val packet = createRefDiscoveryPacket(gitUrl)
        output.writeBytes(packet)
        output.flush()
    }


    // 0032git-upload-pack /test-repo\0host=127.0.0.1\0
    private fun createRefDiscoveryPacket(gitUrl: GitUrl): String {
        val command = buildString {
            append(GIT_UPLOAD_PACK)
            append(" ")
            append(gitUrl.path)
            append(NUL_BYTE)
            append("host=")
            append(gitUrl.host)
            append(NUL_BYTE)
        }

        val length = command.length + 4 // +4 for length header
        val hexLength = String.format("%04x", length)

        return "$hexLength$command"
    }

    private fun readServerResponses(input: DataInputStream) {
        while (true) {
            val lengthHex = input.readNBytes(4).toString(StandardCharsets.UTF_8)
            if (lengthHex == FLUSH_PACKET) break

            val length = lengthHex.toInt(16) - 4
            if (length > 0) {
                val responseData = input.readNBytes(length)
                val response = responseData.toString(StandardCharsets.UTF_8)
                val gitReferences = parseResponse(response)
                println(gitReferences)
            }
        }
    }

    // f490f44a26417c31c44174e12fdbc5a53a761082 HEADmulti_ack thin-pack side-band side-band-64k ofs-delta shallow deepen-since deepen-not deepen-relative no-progress include-tag multi_ack_detailed symref=HEAD:refs/heads/master object-format=sha1 agent=git/2.34.1
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/heads/master
    // f490f44a26417c31c44174e12fdbc5a53a761082 refs/remotes/origin/master
    private fun parseResponse(response: String): List<GitReference> {
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

    private fun terminateConnection(output: DataOutputStream) {
        output.writeBytes(FLUSH_PACKET)
        output.flush()
    }
}