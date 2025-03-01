package com.zeljko.abstractive.zsv.manager.transport.impl

import com.zeljko.abstractive.zsv.manager.transport.impl.MinioConfig.MinioConstants.PROJECT_NAME
import com.zeljko.abstractive.zsv.manager.transport.RemoteRepositoryClient
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.HEADS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.OBJECTS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.REFS_DIR
import io.minio.*
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists


@Service
class MinioRepositoryClient(
    private val minioClient: MinioClient,
) : RemoteRepositoryClient {

    override fun push(branchName: String): String {
        val bucket = PROJECT_NAME // ovo je bukvalno jednako git projektu

        // kreiraj bucket ako nije kreiran
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }

        if(Path.of("$HEADS_DIR/$branchName").notExists()){
            return "Nothing is committed, what are you trying to push!?"
        }

        // trazimo razliku izmedju postojeceg .zsv fajla i onog na serveru i to saljemo?
        // Sta je ovde lose, pa trebali bi da saljemo samo objekte za granu koju pushujemo ne sve?
        val objectsPushed = pushMissingObjects(bucket)

        // kada smo pushovali moramo da uradimo i update da grana pokazuje na zadnji commit
        val refPushed = pushBranchReference(bucket, branchName)

        return "Push completed: $objectsPushed objects, branch reference ${if (refPushed) "updated" else "unchanged"}"
    }

    override fun pull(branchName: String): String {
        val bucket = PROJECT_NAME

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            return "Remote repository does not exist"
        }

        val objectsPulled = pullMissingObjects(bucket)
        val refUpdated = updateLocalBranchReference(bucket, branchName)

        return "Pull completed: $objectsPulled objects, branch reference ${if (refUpdated) "updated" else "unchanged"}"
    }

}