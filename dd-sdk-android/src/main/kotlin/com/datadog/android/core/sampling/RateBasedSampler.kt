/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.sampling

import androidx.annotation.FloatRange
import com.datadog.android.v2.api.InternalLogger
import java.security.SecureRandom

/**
 * [Sampler] with the given sample rate which can be fixed or dynamic.
 *
 * @param samplingRateProvider Provider for the sample rate value which will be called each time
 * the sampling decision needs to be made.
 */
class RateBasedSampler(internal val samplingRateProvider: () -> Float) : Sampler {

    /**
     * Creates a new instance of [RateBasedSampler] with the given sample rate.
     *
     * @param sampleRate Sample rate to use.
     */
    constructor(@FloatRange(from = 0.0, to = 1.0) sampleRate: Float) : this({ sampleRate })

    /**
     * Creates a new instance of [RateBasedSampler] with the given sample rate.
     *
     * @param sampleRate Sample rate to use.
     */
    constructor(@FloatRange(from = 0.0, to = 1.0) sampleRate: Double) : this(sampleRate.toFloat())

    private val random by lazy { SecureRandom() }

    /** @inheritDoc */
    override fun sample(): Boolean {
        return when (val sampleRate = getSamplingRate()) {
            0f -> false
            1f -> true
            else -> random.nextFloat() <= sampleRate
        }
    }

    /** @inheritDoc */
    override fun getSamplingRate(): Float {
        val rawSampleRate = samplingRateProvider()
        return if (rawSampleRate < 0f) {
            InternalLogger.UNBOUND.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                "Sampling rate value provided $rawSampleRate is below 0, setting it to 0."
            )
            0f
        } else if (rawSampleRate > 1f) {
            InternalLogger.UNBOUND.log(
                InternalLogger.Level.WARN,
                InternalLogger.Target.USER,
                "Sampling rate value provided $rawSampleRate is above 1, setting it to 1."
            )
            1f
        } else {
            rawSampleRate
        }
    }
}
