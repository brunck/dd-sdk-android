/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.internal.profiler

import com.datadog.tools.annotation.NoOpImplementation

/**
 * Interface of benchmark counter to be implemented. This should only be used by internal benchmarking.
 */
@NoOpImplementation
interface BenchmarkCounter {

    /**
     * Adds a value to the benchmark counter.
     * @param value The value to be added.
     * @param attributes The attributes to be associated with the value.
     */
    fun add(
        value: Long,
        attributes: Map<String, String>
    )
}
