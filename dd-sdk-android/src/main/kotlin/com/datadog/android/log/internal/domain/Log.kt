/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.log.internal.domain

import com.datadog.android.core.model.NetworkInfo
import com.datadog.android.core.model.UserInfo

/**
 * Represents a Log before it is persisted and sent to Datadog servers.
 */
@Deprecated(
    message = "This class is going to be removed soon. " +
        "Use the LogEvent class instead."
)
internal data class Log(
    val serviceName: String,
    val level: Int,
    val message: String,
    val timestamp: Long,
    val attributes: Map<String, Any?>,
    val tags: List<String>,
    val throwable: Throwable?,
    val networkInfo: NetworkInfo?,
    val userInfo: UserInfo,
    val loggerName: String,
    val threadName: String
) {

    companion object {
        internal const val CRASH: Int = 9
    }
}
