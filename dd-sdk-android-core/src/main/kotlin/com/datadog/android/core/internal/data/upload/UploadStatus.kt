/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.data.upload

import com.datadog.android.api.InternalLogger

internal sealed class UploadStatus(
    val shouldRetry: Boolean = false,
    val code: Int = UNKNOWN_RESPONSE_CODE,
    val throwable: Throwable? = null
) {

    internal class Success(responseCode: Int) : UploadStatus(shouldRetry = false, code = responseCode)

    internal class NetworkError(throwable: Throwable) : UploadStatus(shouldRetry = true, throwable = throwable)
    internal class DNSError(throwable: Throwable) : UploadStatus(shouldRetry = true, throwable = throwable)
    internal class RequestCreationError(throwable: Throwable?) :
        UploadStatus(shouldRetry = false, throwable = throwable)

    internal class InvalidTokenError(responseCode: Int) : UploadStatus(shouldRetry = false, code = responseCode)
    internal class HttpRedirection(responseCode: Int) : UploadStatus(shouldRetry = false, code = responseCode)
    internal class HttpClientError(responseCode: Int) : UploadStatus(shouldRetry = false, code = responseCode)
    internal class HttpServerError(responseCode: Int) : UploadStatus(shouldRetry = true, code = responseCode)
    internal class HttpClientRateLimiting(responseCode: Int) : UploadStatus(shouldRetry = true, code = responseCode)
    internal class UnknownHttpError(responseCode: Int) : UploadStatus(shouldRetry = false, code = responseCode)

    internal class UnknownException(throwable: Throwable) : UploadStatus(shouldRetry = true, throwable = throwable)

    internal object UnknownStatus : UploadStatus(shouldRetry = false, code = UNKNOWN_RESPONSE_CODE)

    fun logStatus(
        context: String,
        byteSize: Int,
        logger: InternalLogger,
        requestId: String? = null
    ) {
        val level = when (this) {
            is HttpClientError,
            is HttpServerError,
            is InvalidTokenError,
            is RequestCreationError,
            is UnknownException,
            is UnknownHttpError -> InternalLogger.Level.ERROR

            is DNSError,
            is HttpClientRateLimiting,
            is HttpRedirection,
            is NetworkError -> InternalLogger.Level.WARN

            is Success -> InternalLogger.Level.INFO

            else -> InternalLogger.Level.VERBOSE
        }

        val targets = when (this) {
            is HttpClientError,
            is HttpClientRateLimiting -> listOf(InternalLogger.Target.USER, InternalLogger.Target.TELEMETRY)

            is DNSError,
            is HttpRedirection,
            is HttpServerError,
            is InvalidTokenError,
            is NetworkError,
            is RequestCreationError,
            is Success,
            is UnknownException,
            is UnknownHttpError -> listOf(InternalLogger.Target.USER)

            else -> emptyList()
        }

        logger.log(
            level,
            targets,
            {
                buildStatusMessage(requestId, byteSize, context, throwable)
            }
        )
    }

    private fun buildStatusMessage(
        requestId: String?,
        byteSize: Int,
        context: String,
        throwable: Throwable?
    ): String {
        return buildString {
            if (requestId == null) {
                append("Batch [$byteSize bytes] ($context)")
            } else {
                append("Batch $requestId [$byteSize bytes] ($context)")
            }

            if (this@UploadStatus is Success) {
                append(" sent successfully.")
            } else if (this@UploadStatus is UnknownStatus) {
                append(" status is unknown")
            } else {
                append(" failed because ")
                when (this@UploadStatus) {
                    is DNSError -> append("of a DNS error")
                    is HttpClientError -> append("of a processing error or invalid data")
                    is HttpClientRateLimiting -> append("of an intake rate limitation")
                    is HttpRedirection -> append("of a network redirection")
                    is HttpServerError -> append("of a server processing error")
                    is InvalidTokenError -> append("your token is invalid")
                    is NetworkError -> append("of a network error")
                    is RequestCreationError -> append("of an error when creating the request")
                    is UnknownException -> append("of an unknown error")
                    is UnknownHttpError -> append("of an unexpected HTTP error (status code = $code)")
                    else -> {}
                }

                if (throwable != null) {
                    append(" (")
                    append(throwable.message)
                    append(")")
                }

                if (shouldRetry) {
                    append("; we will retry later.")
                } else {
                    append("; the batch was dropped.")
                }
            }

            if (this@UploadStatus is InvalidTokenError) {
                append(
                    " Make sure that the provided token still exists " +
                        "and you're targeting the relevant Datadog site."
                )
            }
        }
    }

    companion object {
        internal const val UNKNOWN_RESPONSE_CODE = 0
    }
}
