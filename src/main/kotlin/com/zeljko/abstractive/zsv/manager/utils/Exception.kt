package com.zeljko.abstractive.zsv.manager.utils

class InvalidHashException(message: String) : RuntimeException(message)
class ObjectNotFoundException(message: String) : RuntimeException(message)
class InvalidObjectHeaderException(message: String) : RuntimeException(message)
class RepositoryAlreadyExistsException(message: String) : RuntimeException(message)