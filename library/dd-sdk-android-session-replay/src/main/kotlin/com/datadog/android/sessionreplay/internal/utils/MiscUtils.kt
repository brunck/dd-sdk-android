/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.utils

import android.app.Activity
import android.content.res.Resources.Theme
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.recorder.densityNormalized

internal object MiscUtils {

    internal const val OPAQUE_ALPHA_VALUE = 255

    fun resolveThemeColor(theme: Theme): Int? {
        val a = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        return if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
            a.type <= TypedValue.TYPE_LAST_COLOR_INT
        ) {
            // windowBackground is a color
            a.data
        } else {
            null
        }
    }

    fun resolveSystemInformation(activity: Activity): SystemInformation {
        val screenDensity = activity.resources.displayMetrics.density
        val themeColorAsHexa = resolveThemeColor(activity.theme)?.let {
            StringUtils.formatColorAndAlphaAsHexa(it, OPAQUE_ALPHA_VALUE)
        }
        return SystemInformation(
            screenBounds = resolveScreenBounds(activity, screenDensity),
            screenOrientation = activity.resources.configuration.orientation,
            screenDensity = screenDensity,
            themeColor = themeColorAsHexa
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveScreenBounds(activity: Activity, screenDensity: Float): GlobalBounds {
        val windowManager = activity.windowManager ?: return GlobalBounds(0, 0, 0, 0)
        val screenHeight: Long
        val screenWidth: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentWindowMetrics = windowManager.currentWindowMetrics
            val screenBounds = currentWindowMetrics.bounds
            screenHeight = (screenBounds.bottom - screenBounds.top).toLong()
                .densityNormalized(screenDensity)
            screenWidth = (screenBounds.right - screenBounds.left).toLong()
                .densityNormalized(screenDensity)
        } else {
            val size = Point()
            windowManager.defaultDisplay.getSize(size)
            screenHeight = size.y.toLong().densityNormalized(screenDensity)
            screenWidth = size.x.toLong().densityNormalized(screenDensity)
        }
        return GlobalBounds(0, 0, screenWidth, screenHeight)
    }
}
