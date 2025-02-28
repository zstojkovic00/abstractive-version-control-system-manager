package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.transport.client.GitClient
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import java.io.File
import java.io.FileInputStream


@Command(command = ["zsv"], description = "Zsv commands")
class NetworkCommands(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String,
) {


    @Command(command = ["push"], description = "Push to remote server")
    fun push(
        @Option(shortNames = ['f'], description = "File path") filePath: String

    ): String {
        val bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            println("Bucket '$bucketName' is created")
        }

        val file = File(filePath)
        if (!file.exists()) {
            return "File $filePath does not exist"
        }

        val objectName = file.name
        val inputStream = FileInputStream(file)

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(inputStream, file.length(), -1)
                .build()
        )

        return "File is successfully uploaded";
    }
}