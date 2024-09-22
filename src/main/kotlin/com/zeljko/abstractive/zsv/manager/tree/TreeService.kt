package com.zeljko.abstractive.zsv.manager.tree

import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.InvalidObjectHeaderException
import com.zeljko.abstractive.zsv.manager.utils.zlibDecompress
import org.springframework.shell.command.annotation.Command
import org.springframework.stereotype.Service
import java.nio.file.Files

@Service
@Command(command = ["zsv"], description = "Zsv commands")
class TreeService {
    fun decompressTreeObject(nameOnly: Boolean, treeSha: String): List<Tree> {

        val path = getObjectShaPath(treeSha)
        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        val header = decompressedContent.take(4).toByteArray().toString(Charsets.UTF_8)

        if (header != "tree") {
            throw InvalidObjectHeaderException("Not a tree object")
        }

        return parseTreeContent(decompressedContent)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseTreeContent(content : ByteArray): List<Tree> {
        var index = content.indexOf(0) + 1
        val result = mutableListOf<Tree>()

        while (index < content.size) {
            val modeAndName = StringBuilder()

            while (index < content.size && content[index] != 0.toByte()) {
                modeAndName.append(content[index].toInt().toChar())
                index++
            }

            index++

            if (index + 20 <= content.size) {
                val objectSHA = content.slice(index until index + 20).toByteArray().toHexString()
                index += 20

                val (mode, name) = modeAndName.toString().split(" ", limit = 2)
                result.add(Tree(mode, name, objectSHA))
            } else {
                break
            }
        }
        return result
    }

}