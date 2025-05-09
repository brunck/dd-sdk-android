/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay

import androidx.annotation.FloatRange
import com.datadog.android.api.InternalLogger
import com.datadog.android.sessionreplay.internal.recorder.SessionReplayRecorder
import com.datadog.android.sessionreplay.recorder.OptionSelectorDetector
import com.datadog.android.sessionreplay.utils.DrawableToColorMapper
import java.util.Locale

/**
 * Describes configuration to be used for the Session Replay feature.
 */
data class SessionReplayConfiguration internal constructor(
    internal val customEndpointUrl: String?,
    internal val privacy: SessionReplayPrivacy,
    internal val customMappers: List<MapperTypeWrapper<*>>,
    internal val customOptionSelectorDetectors: List<OptionSelectorDetector>,
    internal val customDrawableMappers: List<DrawableToColorMapper>,
    internal val sampleRate: Float,
    internal val imagePrivacy: ImagePrivacy,
    internal val startRecordingImmediately: Boolean,
    internal val touchPrivacy: TouchPrivacy,
    internal val textAndInputPrivacy: TextAndInputPrivacy,
    internal val dynamicOptimizationEnabled: Boolean,
    internal val systemRequirementsConfiguration: SystemRequirementsConfiguration,
    internal val internalCallback: SessionReplayInternalCallback
) {

    /**
     * A Builder class for a [SessionReplayConfiguration].
     */
    @Suppress("TooManyFunctions")
    class Builder {
        private val logger: InternalLogger
        private val sampleRate: Float

        /**
         * Calling this constructor will default to a 100% session sampling rate.
         */
        constructor() : this(SAMPLE_IN_ALL_SESSIONS, InternalLogger.UNBOUND)

        /**
         * @param sampleRate must be a value between 0 and 100. A value of 0
         * means no session will be recorded, 100 means all sessions will be recorded.
         * If this value is not provided then Session Replay will default to a 100 sample rate.
         */
        constructor(
            @FloatRange(from = 0.0, to = 100.0) sampleRate: Float = SAMPLE_IN_ALL_SESSIONS
        ) : this(sampleRate, InternalLogger.UNBOUND)

        internal constructor(
            @FloatRange(from = 0.0, to = 100.0) sampleRate: Float,
            logger: InternalLogger
        ) {
            this.sampleRate = sampleRate
            this.logger = logger
        }

        private var customEndpointUrl: String? = null
        private var privacy = SessionReplayPrivacy.MASK

        // indicates whether fine grained masking levels have been explicitly set
        private var fineGrainedMaskingSet = false

        private var imagePrivacy = ImagePrivacy.MASK_ALL
        private var startRecordingImmediately = true
        private var touchPrivacy = TouchPrivacy.HIDE
        private var textAndInputPrivacy = TextAndInputPrivacy.MASK_ALL
        private var extensionSupportSet: MutableSet<ExtensionSupport> = mutableSetOf()
        private var dynamicOptimizationEnabled = true
        private var systemRequirementsConfiguration = SystemRequirementsConfiguration.NONE
        private var internalCallback: SessionReplayInternalCallback = NoOpSessionReplayInternalCallback()

        /**
         * Adds an extension support implementation. This is mostly used when you want to provide
         * different behaviour of the Session Replay when using other Android UI frameworks
         * than the default ones.
         * @see [ExtensionSupport.getLegacyCustomViewMappers]
         */
        fun addExtensionSupport(extensionSupport: ExtensionSupport): Builder {
            if (this.extensionSupportSet.any { it.name() == extensionSupport.name() }) {
                logger.log(
                    target = InternalLogger.Target.MAINTAINER,
                    level = InternalLogger.Level.WARN,
                    messageBuilder = { DUPLICATE_EXTENSION_DETECTED.format(Locale.US, extensionSupport.name()) }
                )
            } else {
                this.extensionSupportSet.add(extensionSupport)
            }

            return this
        }

        /**
         * Let the Session Replay target a custom server.
         */
        fun useCustomEndpoint(endpoint: String): Builder {
            customEndpointUrl = endpoint
            return this
        }

        /**
         * Sets the privacy rule for the Session Replay feature.
         * If not specified all the elements will be masked by default (MASK).
         * @see SessionReplayPrivacy.ALLOW
         * @see SessionReplayPrivacy.MASK
         * @see SessionReplayPrivacy.MASK_USER_INPUT
         */
        @Deprecated(
            message = "This method is deprecated and will be removed in future versions. " +
                "Use the new fine grained masking apis instead: " +
                "[setImagePrivacy], [setTouchPrivacy], [setTextAndInputPrivacy]."
        )
        fun setPrivacy(privacy: SessionReplayPrivacy): Builder {
            // if fgm levels have already been explicitly set then ignore legacy privacy.
            if (fineGrainedMaskingSet) return this

            when (privacy) {
                SessionReplayPrivacy.ALLOW -> {
                    this.touchPrivacy = TouchPrivacy.SHOW
                    this.imagePrivacy = ImagePrivacy.MASK_NONE
                    this.textAndInputPrivacy = TextAndInputPrivacy.MASK_SENSITIVE_INPUTS
                }

                SessionReplayPrivacy.MASK_USER_INPUT -> {
                    this.touchPrivacy = TouchPrivacy.HIDE
                    this.imagePrivacy = ImagePrivacy.MASK_LARGE_ONLY
                    this.textAndInputPrivacy = TextAndInputPrivacy.MASK_ALL_INPUTS
                }

                SessionReplayPrivacy.MASK -> {
                    this.touchPrivacy = TouchPrivacy.HIDE
                    this.imagePrivacy = ImagePrivacy.MASK_ALL
                    this.textAndInputPrivacy = TextAndInputPrivacy.MASK_ALL
                }
            }

            return this
        }

        /**
         * Sets the image recording level for the Session Replay feature.
         * If not specified then all images that are considered to be content images will be masked by default.
         * @see ImagePrivacy.MASK_NONE
         * @see ImagePrivacy.MASK_LARGE_ONLY
         * @see ImagePrivacy.MASK_ALL
         */
        fun setImagePrivacy(level: ImagePrivacy): Builder {
            fineGrainedMaskingSet = true
            this.imagePrivacy = level
            return this
        }

        /**
         * Sets the touch recording level for the Session Replay feature.
         * If not specified then all touches will be hidden by default.
         * @see TouchPrivacy.HIDE
         * @see TouchPrivacy.SHOW
         */
        fun setTouchPrivacy(level: TouchPrivacy): Builder {
            fineGrainedMaskingSet = true
            this.touchPrivacy = level
            return this
        }

        /**
         * Should recording start automatically (or be manually started).
         * If not specified then by default it starts automatically.
         * @param enabled whether recording should start automatically or not.
         */
        fun startRecordingImmediately(enabled: Boolean): Builder {
            this.startRecordingImmediately = enabled
            return this
        }

        /**
         * Sets the text and input recording level for the Session Replay feature.
         * If not specified then sensitive text will be masked by default.
         * @see TextAndInputPrivacy.MASK_SENSITIVE_INPUTS
         * @see TextAndInputPrivacy.MASK_ALL_INPUTS
         * @see TextAndInputPrivacy.MASK_ALL
         */
        fun setTextAndInputPrivacy(level: TextAndInputPrivacy): Builder {
            fineGrainedMaskingSet = true
            this.textAndInputPrivacy = level
            return this
        }

        /**
         * This option controls whether optimization is enabled or disabled for recording Session Replay data.
         * By default the value is true, meaning the dynamic optimization is enabled.
         */
        fun setDynamicOptimizationEnabled(dynamicOptimizationEnabled: Boolean): Builder {
            this.dynamicOptimizationEnabled = dynamicOptimizationEnabled
            return this
        }

        /**
         * Defines the minimum system requirements for enabling the Session Replay feature.
         * When [SessionReplay.enable] is invoked, the system configuration is verified against these requirements.
         * If the system meets the specified criteria, Session Replay will be successfully enabled.
         * If this function is not invoked, no minimum requirements will be enforced, and Session Replay will be
         * enabled on all devices.
         */
        fun setSystemRequirements(systemRequirementsConfiguration: SystemRequirementsConfiguration): Builder {
            this.systemRequirementsConfiguration = systemRequirementsConfiguration
            return this
        }

        /**
         * Allows definition of custom callback functions for Session Replay
         * that may require platform-specific behavior.
         * Currently, this enables defining:
         * - [SessionReplayInternalCallback.getCurrentActivity]:
         *   Used in [SessionReplayRecorder] to register fragment lifecycle callbacks
         *   for clients initialized after the `Application.onCreate` phase.
         */
        internal fun setInternalCallback(internalCallback: SessionReplayInternalCallback): Builder {
            this.internalCallback = internalCallback
            return this
        }

        /**
         * Builds a [SessionReplayConfiguration] based on the current state of this Builder.
         */
        fun build(): SessionReplayConfiguration {
            return SessionReplayConfiguration(
                customEndpointUrl = customEndpointUrl,
                privacy = privacy,
                imagePrivacy = imagePrivacy,
                touchPrivacy = touchPrivacy,
                textAndInputPrivacy = textAndInputPrivacy,
                customMappers = customMappers(),
                customOptionSelectorDetectors = optionsSelectorDetectors(),
                customDrawableMappers = customDrawableMappers(),
                sampleRate = sampleRate,
                startRecordingImmediately = startRecordingImmediately,
                dynamicOptimizationEnabled = dynamicOptimizationEnabled,
                systemRequirementsConfiguration = systemRequirementsConfiguration,
                internalCallback = internalCallback
            )
        }

        private fun customMappers(): List<MapperTypeWrapper<*>> {
            val allItems = extensionSupportSet.flatMap { it.getCustomViewMappers() }

            allItems.groupBy { it }
                .filter { it.value.size > 1 }
                .forEach { (item, _) ->
                    logger.log(
                        target = InternalLogger.Target.MAINTAINER,
                        level = InternalLogger.Level.WARN,
                        messageBuilder = { DUPLICATE_MAPPER_DETECTED.format(Locale.US, item.type) }
                    )
                }

            return allItems.distinct().toList()
        }

        private fun customDrawableMappers(): List<DrawableToColorMapper> =
            extensionSupportSet.flatMap { it.getCustomDrawableMapper() }.toList()

        private fun optionsSelectorDetectors(): List<OptionSelectorDetector> =
            extensionSupportSet.flatMap { it.getOptionSelectorDetectors() }.toList()

        internal companion object {
            internal const val SAMPLE_IN_ALL_SESSIONS = 100.0f
            internal const val DUPLICATE_EXTENSION_DETECTED =
                "Attempting to add support twice for the same extension %s. The duplicate will be ignored."
            internal const val DUPLICATE_MAPPER_DETECTED = "Duplicate mapper for %s. The duplicate will be ignored."
        }
    }
}
