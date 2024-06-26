/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.utils

import com.datadog.android.api.InternalLogger

internal class InvocationUtils {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    inline fun<R> safeCallWithErrorLogging(
        // Temporarily use UNBOUND until we handle the loggers
        logger: InternalLogger = InternalLogger.UNBOUND,
        call: () -> R,
        failureMessage: String,
        level: InternalLogger.Level = InternalLogger.Level.WARN,
        target: InternalLogger.Target = InternalLogger.Target.MAINTAINER
    ): R? {
        try {
            return call()
        } catch (e: Exception) {
            // TODO RUM-806 Add logs here once the sdkLogger is added
            logger.log(
                level,
                target,
                { failureMessage },
                e
            )
        }

        return null
    }
}
