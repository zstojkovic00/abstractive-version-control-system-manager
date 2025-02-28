package com.zeljko.abstractive.zsv.manager.utils

import com.zeljko.abstractive.zsv.manager.core.objects.IndexEntry
import java.nio.charset.StandardCharsets.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.TreeSet
import java.util.stream.Collectors


object FileUtils {

    const val ZSV_DIR = ".zsv"
    const val OBJECTS_DIR = "$ZSV_DIR/objects"
    const val REFS_DIR = "$ZSV_DIR/refs"
    const val HEAD_FILE = "$ZSV_DIR/HEAD"
    const val HEADS_DIR = "$REFS_DIR/heads"
    const val INDEX_DIR = "$ZSV_DIR/index"
    public val ignoredItems = setOf(
        ZSV_DIR, ".git", ".gradle", ".idea", "build", "HELP.md",
        "abstractive-version-control-system-manager.log"
    )

    fun createZsvStructure(path: Path = getCurrentPath()): Path {
        val zsvPath = path.resolve(ZSV_DIR)

        listOf("objects", "refs/heads", "refs/tags").forEach {
            Files.createDirectories(zsvPath.resolve(it))
        }

        Files.writeString(zsvPath.resolve("HEAD"), "ref: refs/heads/master\n", UTF_8)
        Files.writeString(
            zsvPath.resolve("description"),
            "Unnamed repository; edit this file 'description' to name the repository.\n", UTF_8
        )
        return zsvPath
    }

    fun getObjectShaPath(path: Path, objectSha: String): Path {
        val objectPath = path.resolve("$OBJECTS_DIR/${objectSha.substring(0, 2)}/${objectSha.substring(2)}")

        if (!Files.exists(objectPath)) {
            throw ObjectNotFoundException("Object not found.")
        }
        return objectPath
    }

    fun storeObject(directory: Path, objectSha: String, compressedContent: ByteArray) {
        val subDirectory = directory.resolve(objectSha.substring(0, 2))
        Files.createDirectories(subDirectory)
        val blobFile = subDirectory.resolve(objectSha.substring(2))
        Files.write(blobFile, compressedContent)
    }

    fun getCurrentPath(): Path = Paths.get("").toAbsolutePath()

    fun getAllFiles(path: Path = getCurrentPath()): TreeSet<String> {
        return Files.walk(path)
            .filter { file ->
                if (file == path || Files.isDirectory(file)) {
                    return@filter false
                }

                val relativePath = path.relativize(file.toAbsolutePath())
                !relativePath.any { ignoredItems.contains(it.toString()) }
            }
            .map { path.relativize(it).toString() }
            .sorted()
            .collect(Collectors.toCollection(::TreeSet))
    }

    fun getAllFilesWithAttributes(path: Path = getCurrentPath()): TreeSet<IndexEntry.FileStatus> {
        return Files.walk(path)
            .filter { file ->
                if (file == path || Files.isDirectory(file)) {
                    return@filter false
                }

                val relativePath = path.relativize(file.toAbsolutePath())
                !relativePath.any { ignoredItems.contains(it.toString()) }
            }
            .map { file ->
                val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)
                IndexEntry.FileStatus(
                    mtime = attributes.lastModifiedTime().toMillis(),
                    ino = Files.getAttribute(file, "unix:ino") as Long,
                    pathName = path.relativize(file).toString()
                )
            }
            .collect(Collectors.toCollection(::TreeSet))
    }

     fun cleanWorkingDirectory(path: Path) {
        Files.walk(path)
            .filter {
                !it.startsWith(path.resolve(ZSV_DIR)) &&
                        !it.startsWith(path.resolve(".git"))
            }
            .filter { it != path }
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }

    fun getZsvDir(): Path {
        val currentPath = getCurrentPath()
        val zsvPath = currentPath.resolve(ZSV_DIR)

        if (Files.exists(zsvPath)) {
            throw RepositoryAlreadyExistsException("zsv repository already exists in this directory")
        }

        return zsvPath
    }

    fun getCurrentHead(): String {
        val headPath = Paths.get(HEAD_FILE)

        if (!Files.exists(headPath)) {
            throw ObjectNotFoundException("Not a zsv repository")
        }

        val headContent = Files.readString(headPath).trim()

        return if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get("$ZSV_DIR/$branchReference")

            if (Files.exists(branchPath)) {
                Files.readString(branchPath).trim()
            } else {
                "" // New branch
            }
        } else {
            headContent
        }
    }

    fun updateHeadReference(branchName: String) {
        val headPath = Paths.get(HEAD_FILE)
        Files.writeString(headPath, "ref: refs/heads/$branchName\n")
    }
}