package com.zeljko.abstractive.zsv.manager.core.objects

enum class ObjectType(val mode: String) {

    REGULAR_FILE("100644"),
    EXECUTABLE_FILE("100755"),
    SYMBOLIC_LINK("120000"),
    DIRECTORY("40000");

    companion object {
        fun fromMode(mode: String): ObjectType {
            return when (mode) {
                "100644" -> REGULAR_FILE
                "100755" -> EXECUTABLE_FILE
                "120000" -> SYMBOLIC_LINK
                "040000", "40000" -> DIRECTORY
                else -> throw IllegalArgumentException("Invalid file mode: $mode")
            }
        }
    }
}