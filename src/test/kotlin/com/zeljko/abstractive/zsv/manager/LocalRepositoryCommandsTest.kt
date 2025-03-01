package com.zeljko.abstractive.zsv.manager

import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode

@Testcontainers
class LocalRepositoryCommandsTest {
    companion object {

        @Container
        @JvmStatic
        val container = GenericContainer<Nothing>("ubuntu:latest").apply {
            withCommand("tail", "-f", "/dev/null")  // Keep container running
            withWorkingDirectory("/workspace") // Set the default working directory inside the container
            withFileSystemBind(".", "/workspace", BindMode.READ_WRITE) // Make project files available inside the container with read-write permissions
        }

    }
    @Test
    fun `test testcontainers`() {
        println("Container is running: ${container.isRunning}")
        val result = container.execInContainer("ls", "-la")
        println(result.stdout)
    }

}