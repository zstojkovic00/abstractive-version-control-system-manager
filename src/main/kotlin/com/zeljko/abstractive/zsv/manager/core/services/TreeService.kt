package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.Blob
import com.zeljko.abstractive.zsv.manager.core.objects.Tree
import com.zeljko.abstractive.zsv.manager.utils.*
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import com.zeljko.abstractive.zsv.manager.core.objects.ObjectType.*
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Service
class TreeService(private val blobService: BlobService) {
    fun decompress(nameOnly: Boolean, treeSha: String): List<Tree> {

        val path = getObjectShaPath(FileUtils.getCurrentPath(), treeSha)
        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        val header = decompressedContent.take(4).toByteArray().toString(Charsets.UTF_8)

        if (header != "tree") {
            throw InvalidObjectHeaderException("Not a tree object")
        }

        return parseTreeContent(decompressedContent)
    }

    fun getDecompressedTreeContent(treeSha: String, repositoryPath: Path): ByteArray {
        val path = getObjectShaPath(repositoryPath, treeSha)
        val compressedContent = Files.readAllBytes(path)
        return compressedContent.zlibDecompress()
    }

    fun compressFromFile(path: Path): String {
        // TODO: .gitignore
        val ignoredItems = setOf(".zsv", ".git", ".gradle", ".idea", "build", "HELP.md",
            "abstractive-version-control-system-manager.log")

        val objects = mutableListOf<Tree>()
        Files.list(path).use { stream ->
            stream
                .filter { file ->
                    !ignoredItems.contains(file.fileName.toString())
                }
                .forEach { file ->
                    val name = file.fileName.toString()
                    if (Files.isDirectory(file)) {
                        val treeSha = compressFromFile(file)
                        objects.add(Tree(DIRECTORY.mode, name, treeSha))
                    } else {
                        val blobSha = blobService.compressFromFile(true, file)
                        val fileMode = when {
                            Files.isExecutable(file) -> EXECUTABLE_FILE
                            // TODO: fix -> Seems like compressFromFile for symbolic link is not working well
                            // zsv sha -> a3c241ab148df99bc5924738958c3aaad76a322b
                            // git sha -> 541cb64f9b85000af670c5b925fa216ac6f98291
                            Files.isSymbolicLink(file) -> SYMBOLIC_LINK
                            else -> REGULAR_FILE
                        }
                        objects.add(Tree(fileMode.mode, name, blobSha))
                    }
                }
        }
        return storeTree(objects)
    }

    fun compressFromContent(repositoryPath: Path, decompressedContent: ByteArray): String {
        val treeHeader = "tree ${decompressedContent.size}\u0000".toByteArray()
        val content = treeHeader + decompressedContent

        val compressedContent = content.zlibCompress()
        val treeSha = content.toSha1()

        val objectsDirectory = repositoryPath.resolve(".git/objects")
        storeObject(objectsDirectory, treeSha, compressedContent)
        return treeSha
    }

    private fun storeTree(objects: List<Tree>): String {
        val sortedObjects = objects.sortedBy { it.fileName }
        val treeContent = buildTreeContent(sortedObjects)
        val treeHeader = "tree ${treeContent.size}\u0000".toByteArray()
        val content = treeHeader + treeContent

        val compressedContent = content.zlibCompress()
        val treeSha = content.toSha1()

        val currentDirectory = Paths.get("").toAbsolutePath()
        val objectsDirectory = currentDirectory.resolve(".git/objects")
        storeObject(objectsDirectory, treeSha, compressedContent)
        return treeSha
    }


    private fun buildTreeContent(trees: List<Tree>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        trees.forEach { tree ->
            val modeAndName = "${tree.fileMode} ${tree.fileName}\u0000".toByteArray()
            val sha = tree.objectSha.toShaByte()
            outputStream.write(modeAndName)
            outputStream.write(sha)
        }
        return outputStream.toByteArray()
    }


    fun parseTreeContent(content: ByteArray, skipHeader: Boolean = true): List<Tree> {
        var index = if (skipHeader) content.indexOf(0) + 1 else 0
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

    fun extractToDisk(decompressed: ByteArray, treeSha: String, extractPath: Path, repositoryPath: Path) {
        val trees = parseTreeContent(decompressed, true)
        println("Trees: $trees")

        trees.forEach { tree ->
            val fullPath = extractPath.resolve(tree.fileName)

            when (tree.fileMode) {
                DIRECTORY.mode -> {
                    Files.createDirectories(fullPath)
                    val decompressedTree = getDecompressedTreeContent(tree.objectSha, repositoryPath)
                    extractToDisk(decompressedTree, tree.objectSha, fullPath, repositoryPath)
                }
                REGULAR_FILE.mode -> {
                    val content = blobService.decompress(tree.objectSha, repositoryPath)
                    Files.writeString(fullPath, content.toString())
                }
            }
        }
    }
}