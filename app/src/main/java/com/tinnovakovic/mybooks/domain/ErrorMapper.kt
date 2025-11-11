package com.tinnovakovic.mybooks.domain

import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ErrorMapper @Inject constructor() {

    fun map(error: Throwable): Exception {
        return when (error) {
            is IOException -> NetworkException.NoConnection("Network connection failed", error)
            is HttpException -> when (error.code()) {
                in 400..499 -> NetworkException.ClientError(
                    "Client error: ${error.code()}",
                    error
                )

                in 500..599 -> NetworkException.ServerError(
                    "Server error: ${error.code()}",
                    error
                )

                else -> NetworkException.Unknown("Unexpected error: ${error.code()}", error)
            }

            else -> NetworkException.Unknown(error.message ?: "Unknown error occurred", error)
        }
    }

    sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class NoConnection(message: String, cause: Throwable? = null) : NetworkException(message, cause)
        class ClientError(message: String, cause: Throwable? = null) : NetworkException(message, cause)
        class ServerError(message: String, cause: Throwable? = null) : NetworkException(message, cause)
        class Unknown(message: String, cause: Throwable? = null) : NetworkException(message, cause)
    }
}
