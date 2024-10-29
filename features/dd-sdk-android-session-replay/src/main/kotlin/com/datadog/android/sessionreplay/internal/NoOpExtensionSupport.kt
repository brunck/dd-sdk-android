/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal

import com.datadog.android.sessionreplay.ExtensionSupport
import com.datadog.android.sessionreplay.MapperTypeWrapper
import com.datadog.android.sessionreplay.recorder.OptionSelectorDetector
import com.datadog.android.sessionreplay.utils.DrawableToColorMapper

internal class NoOpExtensionSupport : ExtensionSupport {

    override fun getCustomViewMappers(): List<MapperTypeWrapper<*>> {
        return emptyList()
    }

    override fun getOptionSelectorDetectors(): List<OptionSelectorDetector> {
        return emptyList()
    }

    override fun getCustomDrawableMapper(): List<DrawableToColorMapper> {
        return emptyList()
    }
}
