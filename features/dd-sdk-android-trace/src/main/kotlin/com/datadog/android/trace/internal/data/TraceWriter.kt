/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.trace.internal.data

import androidx.annotation.WorkerThread
import com.datadog.android.api.InternalLogger
import com.datadog.android.api.context.DatadogContext
import com.datadog.android.api.feature.Feature
import com.datadog.android.api.feature.FeatureSdkCore
import com.datadog.android.api.storage.EventBatchWriter
import com.datadog.android.api.storage.EventType
import com.datadog.android.api.storage.RawBatchEvent
import com.datadog.android.event.EventMapper
import com.datadog.android.trace.internal.domain.event.ContextAwareMapper
import com.datadog.android.trace.internal.storage.ContextAwareSerializer
import com.datadog.android.trace.model.SpanEvent
import com.datadog.legacy.trace.api.sampling.PrioritySampling
import com.datadog.legacy.trace.common.writer.Writer
import com.datadog.opentracing.DDSpan
import java.util.Locale

internal class TraceWriter(
    private val sdkCore: FeatureSdkCore,
    internal val ddSpanToSpanEventMapper: ContextAwareMapper<DDSpan, SpanEvent>,
    internal val eventMapper: EventMapper<SpanEvent>,
    private val serializer: ContextAwareSerializer<SpanEvent>,
    private val internalLogger: InternalLogger
) : Writer {

    // region Writer
    override fun start() {
        // NO - OP
    }

    override fun write(trace: List<DDSpan>?) {
        if (trace == null) return
        sdkCore.getFeature(Feature.TRACING_FEATURE_NAME)
            ?.withWriteContext { datadogContext, eventBatchWriter ->
                trace
                    .filter {
                        it.samplingPriority == null || it.samplingPriority !in DROP_SAMPLING_PRIORITIES
                    }
                    .forEach { span ->
                        writeSpan(datadogContext, eventBatchWriter, span)
                    }
            }
    }

    override fun close() {
        // NO - OP
    }

    override fun incrementTraceCount() {
        // NO - OP
    }

    // endregion

    @WorkerThread
    private fun writeSpan(
        datadogContext: DatadogContext,
        writer: EventBatchWriter,
        span: DDSpan
    ) {
        val spanEvent = ddSpanToSpanEventMapper.map(datadogContext, span)
        val mapped = eventMapper.map(spanEvent) ?: return
        try {
            val serialized = serializer
                .serialize(datadogContext, mapped)
                ?.toByteArray(Charsets.UTF_8) ?: return
            synchronized(this) {
                writer.write(
                    event = RawBatchEvent(data = serialized),
                    batchMetadata = null,
                    eventType = EventType.DEFAULT
                )
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.USER, InternalLogger.Target.TELEMETRY),
                { ERROR_SERIALIZING.format(Locale.US, mapped.javaClass.simpleName) },
                e
            )
        }
    }

    companion object {
        internal const val ERROR_SERIALIZING = "Error serializing %s model"
        internal val DROP_SAMPLING_PRIORITIES = setOf(PrioritySampling.SAMPLER_DROP, PrioritySampling.USER_DROP)
        private val KEEP_SAMPLING_PRIORITIES = setOf(PrioritySampling.SAMPLER_KEEP, PrioritySampling.USER_KEEP)
        internal val KEEP_AND_UNSET_SAMPLING_PRIORITIES = KEEP_SAMPLING_PRIORITIES + PrioritySampling.UNSET
    }
}
