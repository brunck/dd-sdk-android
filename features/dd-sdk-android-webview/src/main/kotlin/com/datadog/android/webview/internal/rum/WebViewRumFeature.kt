/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.webview.internal.rum

import android.content.Context
import com.datadog.android.api.InternalLogger
import com.datadog.android.api.feature.Feature
import com.datadog.android.api.feature.FeatureContextUpdateReceiver
import com.datadog.android.api.feature.FeatureSdkCore
import com.datadog.android.api.feature.StorageBackedFeature
import com.datadog.android.api.net.RequestFactory
import com.datadog.android.api.storage.DataWriter
import com.datadog.android.api.storage.FeatureStorageConfiguration
import com.datadog.android.api.storage.NoOpDataWriter
import com.datadog.android.webview.internal.rum.domain.NativeRumViewsCache
import com.datadog.android.webview.internal.rum.domain.WebViewNativeRumViewsCache
import com.datadog.android.webview.internal.storage.WebViewDataWriter
import com.datadog.android.webview.internal.storage.WebViewEventSerializer
import com.google.gson.JsonObject
import java.util.concurrent.atomic.AtomicBoolean

internal class WebViewRumFeature(
    private val sdkCore: FeatureSdkCore,
    override val requestFactory: RequestFactory,
    internal val nativeRumViewsCache: NativeRumViewsCache = WebViewNativeRumViewsCache()
) : StorageBackedFeature, FeatureContextUpdateReceiver {

    internal var dataWriter: DataWriter<JsonObject> = NoOpDataWriter()

    internal val initialized = AtomicBoolean(false)

    // region Feature

    override val name: String = WEB_RUM_FEATURE_NAME

    override fun onInitialize(appContext: Context) {
        sdkCore.setContextUpdateReceiver(WEB_RUM_FEATURE_NAME, this)
        dataWriter = createDataWriter(sdkCore.internalLogger)
        initialized.set(true)
        val currentRumContext = sdkCore.getFeatureContext(Feature.RUM_FEATURE_NAME)
        nativeRumViewsCache.addToCache(currentRumContext)
    }

    override fun onContextUpdate(featureName: String, event: Map<String, Any?>) {
        if (featureName == Feature.RUM_FEATURE_NAME) {
            nativeRumViewsCache.addToCache(event)
        }
    }

    override val storageConfiguration: FeatureStorageConfiguration =
        FeatureStorageConfiguration.DEFAULT

    override fun onStop() {
        sdkCore.removeContextUpdateReceiver(WEB_RUM_FEATURE_NAME, this)
        dataWriter = NoOpDataWriter()
        initialized.set(false)
    }

    // endregion

    private fun createDataWriter(internalLogger: InternalLogger): DataWriter<JsonObject> {
        return WebViewDataWriter(
            serializer = WebViewEventSerializer(),
            internalLogger = internalLogger
        )
    }

    companion object {
        internal const val WEB_RUM_FEATURE_NAME = "web-rum"
    }
}
