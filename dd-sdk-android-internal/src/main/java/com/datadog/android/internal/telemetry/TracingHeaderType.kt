/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.internal.telemetry

@Suppress("UndocumentedPublicClass")
enum class TracingHeaderType {
    DATADOG,
    B3,
    B3MULTI,
    TRACECONTEXT
}
