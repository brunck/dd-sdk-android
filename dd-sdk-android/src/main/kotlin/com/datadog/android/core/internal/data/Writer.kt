/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.core.internal.data

/**
 * Writes a log to a persistent location, for them to be sent at a later time (undefined).
 * @see [Reader]
 */
internal interface Writer<T : Any> {

    fun write(model: T)

    fun write(models: List<T>)
}
