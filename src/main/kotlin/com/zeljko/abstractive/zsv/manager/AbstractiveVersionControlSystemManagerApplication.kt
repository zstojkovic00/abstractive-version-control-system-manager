package com.zeljko.abstractive.zsv.manager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.shell.command.annotation.CommandScan

@SpringBootApplication
@CommandScan
class AbstractiveVersionControlSystemManagerApplication

fun main(args: Array<String>) {
    runApplication<AbstractiveVersionControlSystemManagerApplication>(*args)
}
