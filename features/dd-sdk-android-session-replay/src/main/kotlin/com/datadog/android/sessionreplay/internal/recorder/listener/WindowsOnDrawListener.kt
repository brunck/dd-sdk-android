/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.listener

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import com.datadog.android.api.InternalLogger
import com.datadog.android.api.feature.measureMethodCallPerf
import com.datadog.android.sessionreplay.SessionReplayPrivacy
import com.datadog.android.sessionreplay.internal.async.RecordedDataQueueHandler
import com.datadog.android.sessionreplay.internal.async.RecordedDataQueueRefs
import com.datadog.android.sessionreplay.internal.recorder.Debouncer
import com.datadog.android.sessionreplay.internal.recorder.SnapshotProducer
import com.datadog.android.sessionreplay.internal.utils.MiscUtils
import java.lang.ref.WeakReference

internal class WindowsOnDrawListener(
    zOrderedDecorViews: List<View>,
    private val recordedDataQueueHandler: RecordedDataQueueHandler,
    private val snapshotProducer: SnapshotProducer,
    private val privacy: SessionReplayPrivacy,
    private val debouncer: Debouncer = Debouncer(),
    private val miscUtils: MiscUtils = MiscUtils,
    private val internalLogger: InternalLogger,
    private val methodCallSamplingRate: Float
) : ViewTreeObserver.OnDrawListener {

    internal val weakReferencedDecorViews: List<WeakReference<View>> = zOrderedDecorViews.map { WeakReference(it) }

    @MainThread
    override fun onDraw() {
        debouncer.debounce(snapshotRunnable)
    }

    // Note: we declare the anonymous object explicitly to annotate the run method as @UiThread
    private val snapshotRunnable: Runnable = object : Runnable {

        @UiThread
        override fun run() {
            val rootViews = weakReferencedDecorViews.mapNotNull { it.get() }

            // is is very important to have the windows sorted by their z-order
            val context = rootViews.firstOrNull()?.context ?: return
            val systemInformation = miscUtils.resolveSystemInformation(context)
            val item = recordedDataQueueHandler.addSnapshotItem(systemInformation) ?: return

            val nodes = internalLogger.measureMethodCallPerf(
                METHOD_CALL_CALLER_CLASS,
                METHOD_CALL_CAPTURE_RECORD,
                methodCallSamplingRate
            ) {
                val recordedDataQueueRefs = RecordedDataQueueRefs(recordedDataQueueHandler)
                recordedDataQueueRefs.recordedDataQueueItem = item
                rootViews.mapNotNull {
                    snapshotProducer.produce(it, systemInformation, privacy, recordedDataQueueRefs)
                }
            }

            if (nodes.isNotEmpty()) {
                item.nodes = nodes
            }

            item.isFinishedTraversal = true

            if (item.isReady()) {
                recordedDataQueueHandler.tryToConsumeItems()
            }
        }
    }

    companion object {
        const val METHOD_CALL_SAMPLING_RATE = 0.1f
        private const val METHOD_CALL_CAPTURE_RECORD: String = "Capture Record"

        private val METHOD_CALL_CALLER_CLASS = WindowsOnDrawListener::class.java
    }
}
