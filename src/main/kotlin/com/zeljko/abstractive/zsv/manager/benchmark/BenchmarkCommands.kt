package com.zeljko.abstractive.zsv.manager.benchmark

import org.springframework.shell.command.annotation.Command


@Command(command = ["zsv"], description = "Zsv commands")
class BenchmarkCommands(private val benchmarkService: BenchmarkService) {


}