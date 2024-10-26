package com.zeljko.abstractive.zsv.manager.transport.client

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
                println("Server response: $response")
            }
        }
    }

    private fun terminateConnection(output: DataOutputStream) {
        output.writeBytes(FLUSH_PACKET)
        output.flush()
    }
}