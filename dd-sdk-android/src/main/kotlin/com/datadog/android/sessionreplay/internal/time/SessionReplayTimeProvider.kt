/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.time

import com.datadog.android.rum.internal.RumFeature
import com.datadog.android.sessionreplay.utils.TimeProvider
import com.datadog.android.v2.api.context.DatadogContext
import com.datadog.android.v2.core.internal.ContextProvider

internal class SessionReplayTimeProvider(
    private val contextProvider: ContextProvider,
    private val currentTimeProvider: () -> Long =
        { System.currentTimeMillis() }
) : TimeProvider {
    override fun getDeviceTimestamp(): Long {
        return currentTimeProvider() +
            resolveRumViewTimestampOffset(contextProvider.context)
    }

    private fun resolveRumViewTimestampOffset(datadogContext: DatadogContext): Long {
        val rumFeatureContext = datadogContext.featuresContext[RumFeature.RUM_FEATURE_NAME]
        val timestampOffset = rumFeatureContext?.get(RumFeature.VIEW_TIMESTAMP_OFFSET_IN_MS_KEY)
        return if (timestampOffset is Long) timestampOffset else 0L
    }
}