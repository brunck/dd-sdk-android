/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.monitor

import com.datadog.android.rum.model.ActionEvent

internal sealed class StorageEvent {
    object View : StorageEvent()
    data class Action(
        val frustrationCount: Int,
        val type: ActionEvent.ActionEventActionType,
        val eventEndTimestampInNanos: Long
    ) : StorageEvent()

    data class Resource(val resourceId: String, val resourceStopTimestampInNanos: Long) : StorageEvent()
    data class Error(val resourceId: String? = null, val resourceStopTimestampInNanos: Long? = null) : StorageEvent()
    object LongTask : StorageEvent()
    object FrozenFrame : StorageEvent()
}
