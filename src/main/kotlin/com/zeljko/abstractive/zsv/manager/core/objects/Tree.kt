package com.zeljko.abstractive.zsv.manager.core.objects
import com.zeljko.abstractive.zsv.manager.core.objects.ObjectType.*


/**
100644 (regular file)
100766 (executable file)
120000 (symbolic link)
040000 (directory)

fileMode     objectType   objectSha                                   fileName
120000       blob         541cb64f9b85000af670c5b925fa216ac6f98291    link_to_test.txt
040000       tree         f433240f70c738bfcc2f48994d7ca0c843a763ad    main
100644       blob         16e9ca692c04fe47655e06ed163cddd0f4c49687    test.sh
100644       blob         10500012fca9b4425b50de67a7258a12cba0c076    test.txt
040000       tree         81803f67f37d96f8b76ae69719ebb5e5cbcbb869    test
 */
data class Tree(val fileMode: String, val fileName: String, val objectSha: String) {

    override fun toString(): String {
        return String.format("%-6s %-4s %-40s %s",
            fileMode,
            getObjectType(),
            objectSha,
            fileName)
    }

    private val objectType: ObjectType
        get() = ObjectType.fromMode(fileMode)

    private fun getObjectType(): String {
        return when (objectType) {
            REGULAR_FILE, EXECUTABLE_FILE, SYMBOLIC_LINK -> "blob"
            DIRECTORY -> "tree"
        }
    }
}
