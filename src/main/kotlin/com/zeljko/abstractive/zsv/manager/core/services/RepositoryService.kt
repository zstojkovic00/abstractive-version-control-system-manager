package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getAllFilesWithAttributes
import org.springframework.stereotype.Service

@Service
class RepositoryService(
    private val indexService: IndexService,
    private val branchService: BranchService
) {
    fun status() {
        // fajlovi koji nisu komitovani, ovo je razlika izmedju index i all files
        // fajlovi koji su modifikovani, ovo je ono sto je u indexu i all files ali je mode time drugacije, takodje i ino?
        // current branch?
        val branchName = branchService.getCurrentBranchName()
        val indexFiles = indexService.getIndexFiles()
        val files = getAllFilesWithAttributes()

        // 1. Novi fajlovi, postoje u working dir ali ne i u index-u
        // 2. Obrisani fajlovi, postoje u idnex-u ali ne u working dir
        // 3. modifikovani fajlovi, postoje i u indexu i u working dir ali im se razlikuju po mtime ili ino


    }
}