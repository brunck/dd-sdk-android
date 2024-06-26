/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.trace.internal.coroutines

import com.datadog.android.trace.coroutines.CoroutineScopeSpan
import io.opentracing.Span
import kotlinx.coroutines.CoroutineScope

internal class CoroutineScopeSpanImpl(
    private val scope: CoroutineScope,
    private val span: Span
) : CoroutineScopeSpan,
    CoroutineScope by scope,
    Span by span
