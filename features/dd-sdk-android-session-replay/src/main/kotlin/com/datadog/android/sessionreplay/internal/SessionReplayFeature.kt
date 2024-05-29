/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal

import android.app.Application
import android.content.Context
import com.datadog.android.api.InternalLogger
import com.datadog.android.api.SdkCore
import com.datadog.android.api.feature.Feature
import com.datadog.android.api.feature.FeatureEventReceiver
import com.datadog.android.api.feature.FeatureSdkCore
import com.datadog.android.api.feature.StorageBackedFeature
import com.datadog.android.api.net.RequestFactory
import com.datadog.android.api.storage.FeatureStorageConfiguration
import com.datadog.android.core.sampling.RateBasedSampler
import com.datadog.android.core.sampling.Sampler
import com.datadog.android.sessionreplay.MapperTypeWrapper
import com.datadog.android.sessionreplay.SessionReplayPrivacy
import com.datadog.android.sessionreplay.internal.net.BatchesToSegmentsMapper
import com.datadog.android.sessionreplay.internal.net.SegmentRequestFactory
import com.datadog.android.sessionreplay.internal.recorder.NoOpRecorder
import com.datadog.android.sessionreplay.internal.recorder.Recorder
import com.datadog.android.sessionreplay.internal.resources.ResourcesDataStoreManager
import com.datadog.android.sessionreplay.internal.resources.StringSetDeserializer
import com.datadog.android.sessionreplay.internal.resources.StringSetSerializer
import com.datadog.android.sessionreplay.internal.storage.NoOpRecordWriter
import com.datadog.android.sessionreplay.internal.storage.RecordWriter
import com.datadog.android.sessionreplay.internal.storage.SessionReplayRecordWriter
import com.datadog.android.sessionreplay.recorder.OptionSelectorDetector
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Session Replay feature class, which needs to be registered with Datadog SDK instance.
 */
internal class SessionReplayFeature(
    private val sdkCore: FeatureSdkCore,
    private val customEndpointUrl: String?,
    internal val privacy: SessionReplayPrivacy,
    private val rateBasedSampler: Sampler,
    private val recorderProvider: RecorderProvider
) : StorageBackedFeature, FeatureEventReceiver {

    private val currentRumSessionId = AtomicReference<String>()

    internal constructor(
        sdkCore: FeatureSdkCore,
        customEndpointUrl: String?,
        privacy: SessionReplayPrivacy,
        customMappers: List<MapperTypeWrapper<*>>,
        customOptionSelectorDetectors: List<OptionSelectorDetector>,
        sampleRate: Float
    ) : this(
        sdkCore,
        customEndpointUrl,
        privacy,
        RateBasedSampler(sampleRate),
        DefaultRecorderProvider(
            sdkCore,
            privacy,
            customMappers,
            customOptionSelectorDetectors
        )
    )

    private lateinit var appContext: Context
    private var isRecording = AtomicBoolean(false)
    internal var sessionReplayRecorder: Recorder = NoOpRecorder()
    internal var dataWriter: RecordWriter = NoOpRecordWriter()
    internal val initialized = AtomicBoolean(false)

    // region Feature

    override val name: String = Feature.SESSION_REPLAY_FEATURE_NAME

    override fun onInitialize(appContext: Context) {
        if (appContext !is Application) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                { REQUIRES_APPLICATION_CONTEXT_WARN_MESSAGE }
            )
            return
        }

        this.appContext = appContext
        sdkCore.setEventReceiver(SESSION_REPLAY_FEATURE_NAME, this)

        val resourcesFeature = registerResourceFeature(sdkCore)
        val dataStoreWriter = ResourcesDataStoreManager(
            featureSdkCore = sdkCore,
            serializer = StringSetSerializer(),
            deserializer = StringSetDeserializer()
        )

        dataWriter = createDataWriter()
        sessionReplayRecorder =
            recorderProvider.provideSessionReplayRecorder(
                resourcesDataStoreManager = dataStoreWriter,
                resourceWriter = resourcesFeature.dataWriter,
                recordWriter = dataWriter,
                application = appContext
            )
        sessionReplayRecorder.registerCallbacks()
        initialized.set(true)
        sdkCore.updateFeatureContext(SESSION_REPLAY_FEATURE_NAME) {
            it[SESSION_REPLAY_SAMPLE_RATE_KEY] = rateBasedSampler.getSampleRate()?.toLong()
            it[SESSION_REPLAY_PRIVACY_KEY] = privacy.toString().lowercase(Locale.US)
            // False by default. This will be changed once we will conform to the browser SR
            // implementation where a parameter will be passed in the Configuration constructor
            // to enable manual recording.
            it[SESSION_REPLAY_MANUAL_RECORDING_KEY] = false
        }
    }

    override val requestFactory: RequestFactory =
        SegmentRequestFactory(
            customEndpointUrl,
            BatchesToSegmentsMapper(sdkCore.internalLogger)
        )

    override val storageConfiguration: FeatureStorageConfiguration =
        STORAGE_CONFIGURATION

    override fun onStop() {
        stopRecording()
        sessionReplayRecorder.unregisterCallbacks()
        sessionReplayRecorder.stopProcessingRecords()
        dataWriter = NoOpRecordWriter()
        sessionReplayRecorder = NoOpRecorder()
        initialized.set(false)
    }

    // endregion

    // region EventReceiver

    override fun onReceive(event: Any) {
        if (event !is Map<*, *>) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                { UNSUPPORTED_EVENT_TYPE.format(Locale.US, event::class.java.canonicalName) }
            )
            return
        }

        handleRumSession(event)
    }

    // endregion

    // region Internal

    private fun handleRumSession(sessionMetadata: Map<*, *>) {
        if (sessionMetadata[SESSION_REPLAY_BUS_MESSAGE_TYPE_KEY] ==
            RUM_SESSION_RENEWED_BUS_MESSAGE
        ) {
            checkStatusAndApplySample(sessionMetadata)
        } else {
            sdkCore.internalLogger.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                {
                    UNKNOWN_EVENT_TYPE_PROPERTY_VALUE.format(
                        Locale.US,
                        sessionMetadata[SESSION_REPLAY_BUS_MESSAGE_TYPE_KEY]
                    )
                }
            )
        }
    }

    @Suppress("ReturnCount")
    private fun checkStatusAndApplySample(sessionMetadata: Map<*, *>) {
        val keepSession = sessionMetadata[RUM_KEEP_SESSION_BUS_MESSAGE_KEY] as? Boolean
        val sessionId = sessionMetadata[RUM_SESSION_ID_BUS_MESSAGE_KEY] as? String

        if (keepSession == null || sessionId == null) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                { EVENT_MISSING_MANDATORY_FIELDS }
            )
            return
        }

        if (currentRumSessionId.get() == sessionId) {
            // we already handled this session
            return
        }

        if (!checkIfInitialized()) {
            return
        }

        if (keepSession && rateBasedSampler.sample()) {
            startRecording()
        } else {
            sdkCore.internalLogger.log(
                InternalLogger.Level.INFO,
                InternalLogger.Target.USER,
                { SESSION_SAMPLED_OUT_MESSAGE }
            )
            stopRecording()
        }

        currentRumSessionId.set(sessionId)
    }

    private fun checkIfInitialized(): Boolean {
        if (!initialized.get()) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                { CANNOT_START_RECORDING_NOT_INITIALIZED }
            )
            return false
        }
        return true
    }

    /**
     * Resumes the replay recorder.
     */
    internal fun startRecording() {
        // Check initialization again so we don't forget to do it when this method is made public
        if (checkIfInitialized() && !isRecording.getAndSet(true)) {
            sdkCore.updateFeatureContext(SESSION_REPLAY_FEATURE_NAME) {
                it[SESSION_REPLAY_ENABLED_KEY] = true
            }
            sessionReplayRecorder.resumeRecorders()
        }
    }

    private fun createDataWriter(): RecordWriter {
        val recordCallback = SessionReplayRecordCallback(sdkCore)
        return SessionReplayRecordWriter(sdkCore, recordCallback)
    }

    /**
     * Stops the replay recorder.
     */
    internal fun stopRecording() {
        if (isRecording.getAndSet(false)) {
            sdkCore.updateFeatureContext(SESSION_REPLAY_FEATURE_NAME) {
                it[SESSION_REPLAY_ENABLED_KEY] = false
            }
            sessionReplayRecorder.stopRecorders()
        }
    }

    // endregion

    // region resourcesFeature

    private fun registerResourceFeature(sdkCore: SdkCore): ResourcesFeature {
        val resourcesFeature = ResourcesFeature(
            sdkCore = sdkCore as FeatureSdkCore,
            customEndpointUrl = customEndpointUrl
        )
        sdkCore.registerFeature(resourcesFeature)

        return resourcesFeature
    }

    // endregion

    internal companion object {

        /**
         * Session Replay storage configuration with the following parameters:
         * max item size = 10 MB,
         * max items per batch = 500,
         * max batch size = 10 MB, SR intake batch limit is 10MB
         * old batch threshold = 18 hours.
         */
        internal val STORAGE_CONFIGURATION: FeatureStorageConfiguration =
            FeatureStorageConfiguration.DEFAULT.copy(
                maxItemSize = 10 * 1024 * 1024,
                maxBatchSize = 10 * 1024 * 1024
            )

        internal const val REQUIRES_APPLICATION_CONTEXT_WARN_MESSAGE = "Session Replay could not " +
                "be initialized without the Application context."
        internal const val SESSION_SAMPLED_OUT_MESSAGE = "This session was sampled out from" +
                " recording. No replay will be provided for it."
        internal const val UNSUPPORTED_EVENT_TYPE =
            "Session Replay feature receive an event of unsupported type=%s."
        internal const val UNKNOWN_EVENT_TYPE_PROPERTY_VALUE =
            "Session Replay feature received an event with unknown value of \"type\" property=%s."
        internal const val EVENT_MISSING_MANDATORY_FIELDS = "Session Replay feature received an " +
                "event where one or more mandatory (keepSession) fields" +
                " are either missing or have wrong type."
        internal const val CANNOT_START_RECORDING_NOT_INITIALIZED =
            "Cannot start session recording, because Session Replay feature is not initialized."
        const val SESSION_REPLAY_FEATURE_NAME = "session-replay"
        const val SESSION_REPLAY_BUS_MESSAGE_TYPE_KEY = "type"
        const val RUM_SESSION_RENEWED_BUS_MESSAGE = "rum_session_renewed"
        const val RUM_KEEP_SESSION_BUS_MESSAGE_KEY = "keepSession"
        const val RUM_SESSION_ID_BUS_MESSAGE_KEY = "sessionId"
        internal const val SESSION_REPLAY_SAMPLE_RATE_KEY = "session_replay_sample_rate"
        internal const val SESSION_REPLAY_PRIVACY_KEY = "session_replay_privacy"
        internal const val SESSION_REPLAY_MANUAL_RECORDING_KEY =
            "session_replay_requires_manual_recording"
        internal const val SESSION_REPLAY_ENABLED_KEY =
            "session_replay_is_enabled"
    }
}
