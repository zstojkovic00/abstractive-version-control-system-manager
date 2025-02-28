package com.zeljko.abstractive.zsv.manager.transport.client

import com.zeljko.abstractive.zsv.manager.transport.client.MinioConfig.MinioConstants.DEFAULT_BUCKET_NAME
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.HEADS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.OBJECTS_DIR
import io.minio.*
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MinioService(
    private val minioClient: MinioClient,
) {


    fun push(branchName: String): String {
        val bucket = DEFAULT_BUCKET_NAME
        createBucketIfNotExist(bucket)

        val objectsPushed = pushMissingObjects(bucket)
        val refPushed = pushBranchReference(bucket, branchName)

        return "Push completed: $objectsPushed objects, branch reference ${if (refPushed) "updated" else "unchanged"}"
    }

    fun pull(branchName: String): String {
        val bucket = DEFAULT_BUCKET_NAME

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            return "Remote repository does not exist"
        }

        val objectsPulled = pullMissingObjects(bucket)
        val refUpdated = updateLocalBranchReference(bucket, branchName)

        return "Pull completed: $objectsPulled objects, branch reference ${if (refUpdated) "updated" else "unchanged"}"
    }

    private fun createBucketIfNotExist(bucket: String) {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    private fun pullMissingObjects(bucket: String): Int {
        val localObjects = getLocalObjects()
        val remoteObjects = getRemoteObjectsWithPaths(bucket)
        var pulledCount = 0

        for ((objectPath, _) in remoteObjects) {
            if (!localObjects.containsKey(objectPath)) {
                val targetDir = Paths.get(OBJECTS_DIR).resolve(objectPath.removePrefix("objects/")).parent
                Files.createDirectories(targetDir)

                val targetFile = targetDir.resolve(objectPath.substringAfterLast("/"))

                minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(objectPath)
                        .build()
                ).use { response ->
                    Files.copy(response, targetFile)
                }

                pulledCount++
            }
        }

        return pulledCount
    }

    private fun updateLocalBranchReference(bucket: String, branchName: String): Boolean {
        val remoteBranchPath = "refs/heads/$branchName"
        val localBranchPath = Paths.get("$HEADS_DIR/$branchName")

        val remoteCommitSha = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(remoteBranchPath)
                .build()
        ).use { it.bufferedReader().readText().trim() }

        val currentLocalSha = if (Files.exists(localBranchPath)) {
            Files.readString(localBranchPath).trim()
        } else {
            ""
        }

        if (remoteCommitSha != currentLocalSha) {
            Files.createDirectories(localBranchPath.parent)
            Files.writeString(localBranchPath, "$remoteCommitSha\n")
            return true
        }

        return false
    }

    private fun getRemoteObjectsWithPaths(bucket: String): Map<String, String> {
        val result = HashMap<String, String>()

        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucket)
                .recursive(true)
                .build()
        )

        for (obj in objects) {
            val item = obj.get()
            result[item.objectName()] = item.objectName()
        }

        return result
    }

    private fun pushBranchReference(bucket: String, branchName: String): Boolean {
        val branchPath = Paths.get("$HEADS_DIR/$branchName")

        if (!Files.exists(branchPath)) {
            val commitSha = Files.readString(branchPath).trim()
            val remoteBranchPath = "refs/heads/$branchName"

            val currentRemoteSha = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(remoteBranchPath)
                    .build()
            ).use { it.bufferedReader().readText().trim() }

            if (commitSha == currentRemoteSha) {
                return false
            }

            val refContent = "$commitSha\n"
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(remoteBranchPath)
                    .contentType("text/plain")
                    .stream(refContent.byteInputStream(), refContent.length.toLong(), -1)
                    .build()
            )
            return true
        }

        return false
    }


    private fun pushMissingObjects(bucket: String): Int {
        val localObjects = getLocalObjects()
        val remoteObjects = getRemoteObjects(bucket)
        var pushedCount = 0

        for ((objectPath, localFile) in localObjects) {
            if (!remoteObjects.contains(objectPath)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(objectPath)
                        .stream(FileInputStream(localFile.toFile()), Files.size(localFile), -1)
                        .build()
                )
                pushedCount++
            }
        }
        return pushedCount
    }

    private fun getLocalObjects(): Map<String, Path> {
        val result = HashMap<String, Path>()
        val objectsDir = Paths.get(OBJECTS_DIR)

        if (Files.exists(objectsDir)) {
            Files.walk(objectsDir)
                .filter(Files::isRegularFile)
                .forEach { path ->
                    val dirName = path.parent.fileName.toString()
                    val fileName = path.fileName.toString()
                    val objectsPath = "objects/$dirName/$fileName"
                    result[objectsPath] = path
                }
        }
        return result
    }

    private fun getRemoteObjects(bucket: String): Set<String> {
        val result = HashSet<String>()

        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix("objects/")
                .recursive(true)
                .build()
        )

        for (obj in objects) {
            result.add(obj.get().objectName())
        }

        return result
    }

}