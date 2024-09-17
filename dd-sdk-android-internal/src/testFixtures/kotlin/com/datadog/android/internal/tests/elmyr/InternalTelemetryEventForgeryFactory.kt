/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.internal.tests.elmyr

import com.datadog.android.internal.telemetry.InternalTelemetryEvent
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

class InternalTelemetryEventForgeryFactory : ForgeryFactory<InternalTelemetryEvent> {

    override fun getForgery(forge: Forge): InternalTelemetryEvent {
        val random = forge.anInt(min = 0, max = 6)
        return when (random) {
            0 -> forge.getForgery<InternalTelemetryEvent.Log.Debug>()

            1 -> forge.getForgery<InternalTelemetryEvent.Log.Error>()

            2 -> forge.getForgery<InternalTelemetryEvent.Configuration>()
            3 -> InternalTelemetryEvent.InterceptorInstantiated
            4 -> forge.getForgery<InternalTelemetryEvent.Metric>()
            else -> forge.getForgery<InternalTelemetryEvent.ApiUsage>()
        }
    }
}
