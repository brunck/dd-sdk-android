/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.storage

import com.datadog.android.v2.api.EventBatchWriter
import com.datadog.android.v2.core.storage.DataWriter

internal class NoOpDataWriter<T> : DataWriter<T> {
    public override fun write(writer: EventBatchWriter, element: T): Boolean = false
}
