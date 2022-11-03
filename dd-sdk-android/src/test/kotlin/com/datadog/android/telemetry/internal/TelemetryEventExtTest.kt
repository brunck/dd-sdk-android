/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.telemetry.internal

import android.util.Log
import com.datadog.android.telemetry.model.TelemetryDebugEvent
import com.datadog.android.telemetry.model.TelemetryErrorEvent
import com.datadog.android.utils.config.LoggerTestConfiguration
import com.datadog.android.utils.forge.Configurator
import com.datadog.android.utils.forge.aStringNotMatchingSet
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import java.util.Locale

@Extensions(
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@ForgeConfiguration(Configurator::class)
internal class TelemetryEventExtTest {

    private lateinit var fakeInvalidSource: String
    private lateinit var fakeValidTelemetrySource: String

    @BeforeEach
    fun `set up`(forge: Forge) {
        // we are using the TelemetryDebugEvent.Source here as the source enum for all the events is
        // generated from the same _common-schema.json
        fakeInvalidSource = forge.aStringNotMatchingSet(
            TelemetryDebugEvent.Source.values()
                .map {
                    it.toJson().asString
                }.toSet()
        )
        fakeValidTelemetrySource = forge.aValueFrom(TelemetryDebugEvent.Source::class.java)
            .toJson().asString
    }

    // region TelemetryDebugEvent

    @Test
    fun `M resolve the TelemetryDebugEvent source W telemetryDebugEventSource`() {
        assertThat(
            TelemetryDebugEvent.Source.tryFromSource(fakeValidTelemetrySource)
                ?.toJson()?.asString
        )
            .isEqualTo(fakeValidTelemetrySource)
    }

    @Test
    fun `M return null W telemetryDebugEventSource { unknown source }`() {
        assertThat(TelemetryDebugEvent.Source.tryFromSource(fakeInvalidSource)).isNull()
    }

    @Test
    fun `M send an error dev log W telemetryDebugEventSource { unknown source }`() {
        // When
        TelemetryDebugEvent.Source.tryFromSource(fakeInvalidSource)

        // Then
        verify(logger.mockDevLogHandler).handleLog(
            eq(Log.ERROR),
            eq(
                UNKNOWN_SOURCE_WARNING_MESSAGE_FORMAT
                    .format(Locale.US, fakeInvalidSource)
            ),
            argThat { this is NoSuchElementException },
            eq(emptyMap()),
            eq(emptySet()),
            eq(null)
        )
    }

    // endregion

    // region TelemetryErrorEvent

    @Test
    fun `M resolve the TelemetryErrorEvent source W telemetryErrorEventSource`() {
        assertThat(
            TelemetryErrorEvent.Source.tryFromSource(fakeValidTelemetrySource)
                ?.toJson()?.asString
        )
            .isEqualTo(fakeValidTelemetrySource)
    }

    @Test
    fun `M return null W telemetryErrorEventSource { unknown source }`() {
        assertThat(TelemetryErrorEvent.Source.tryFromSource(fakeInvalidSource)).isNull()
    }

    @Test
    fun `M send an error dev log W telemetryErrorEventSource { unknown source }`() {
        // When
        TelemetryErrorEvent.Source.tryFromSource(fakeInvalidSource)

        // Then
        verify(logger.mockDevLogHandler).handleLog(
            eq(Log.ERROR),
            eq(
                UNKNOWN_SOURCE_WARNING_MESSAGE_FORMAT
                    .format(Locale.US, fakeInvalidSource)
            ),
            argThat { this is NoSuchElementException },
            eq(emptyMap()),
            eq(emptySet()),
            eq(null)
        )
    }

    // endregion

    companion object {
        val logger = LoggerTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(logger)
        }
    }
}
