package com.zeljko.abstractive.zsv.manager.transport.client

import com.zeljko.abstractive.zsv.manager.core.services.BlobService
import com.zeljko.abstractive.zsv.manager.core.services.CommitService
import com.zeljko.abstractive.zsv.manager.core.services.TreeService
import com.zeljko.abstractive.zsv.manager.transport.model.GitUrl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.net.URI


@Component
@Qualifier("http")
class GitHttpClient(
    private val blobService: BlobService,
    private val treeService: TreeService,
    private val commitService: CommitService,
) : GitClient {
    override fun clone(url: String) {
        val gitUrl = parseHttpUrl(url)
        println(gitUrl)
    }

    private fun parseHttpUrl(url: String): GitUrl {
        val uri = URI(url)
        return GitUrl(
            host = uri.host,

            port = if (uri.port == -1)
                if (url.startsWith("https")) 443
                else 80
            else uri.port,

            path = uri.path
        )
    }
}