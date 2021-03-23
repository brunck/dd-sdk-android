/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.persistence.file.batch

import com.datadog.android.core.internal.domain.PayloadDecoration
import com.datadog.android.core.internal.persistence.PersistenceStrategy
import com.datadog.android.core.internal.persistence.Serializer
import com.datadog.android.core.internal.persistence.file.FileOrchestrator
import com.datadog.android.log.Logger
import com.datadog.android.log.internal.logger.LogHandler
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.extensions.ApiLevelExtension
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(ApiLevelExtension::class)
)
@ForgeConfiguration(Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class BatchFilePersistenceStrategyTest {

    lateinit var testedStrategy: PersistenceStrategy<String>

    @Mock
    lateinit var mockFileOrchestrator: FileOrchestrator

    @Mock
    lateinit var mockSerializer: Serializer<String>

    @Mock
    lateinit var mockLogHandler: LogHandler

    @Forgery
    lateinit var fakePayloadDecoration: PayloadDecoration

    @BeforeEach
    fun `set up`() {
        whenever(mockSerializer.serialize(any())) doAnswer {
            (it.arguments[0] as String).reversed()
        }

        testedStrategy = BatchFilePersistenceStrategy(
            mockFileOrchestrator,
            mockSerializer,
            fakePayloadDecoration,
            Logger(mockLogHandler)
        )
    }

    @Test
    fun `𝕄 return batch file writer 𝕎 getWriter()`() {
        // When
        val writer = testedStrategy.getWriter()

        // Then
        assertThat(writer).isInstanceOf(BatchFileDataWriter::class.java)
    }

    @Test
    fun `𝕄 return same batch file writer 𝕎 getWriter() twice`() {
        // When
        val writer1 = testedStrategy.getWriter()
        val writer2 = testedStrategy.getWriter()

        // Then
        assertThat(writer1).isSameAs(writer2)
    }

    @Test
    fun `𝕄 return batch file reader 𝕎 getReader()`() {
        // When
        val reader = testedStrategy.getReader()

        // Then
        assertThat(reader).isInstanceOf(BatchFileDataReader::class.java)
    }

    @Test
    fun `𝕄 return same batch file reader 𝕎 getReader() twice`() {
        // When
        val reader1 = testedStrategy.getReader()
        val reader2 = testedStrategy.getReader()

        // Then
        assertThat(reader1).isSameAs(reader2)
    }

    @Test
    fun `𝕄 share orchestrator 𝕎 getWriter() + getReader()`() {
        // Given

        // When
        val writer = testedStrategy.getWriter()
        val reader = testedStrategy.getReader()

        // Then
        check(writer is BatchFileDataWriter)
        check(reader is BatchFileDataReader)
        assertThat(writer.fileOrchestrator).isSameAs(reader.fileOrchestrator)
    }

    @Test
    fun `𝕄 share decoration 𝕎 getWriter() + getReader()`() {
        // Given

        // When
        val writer = testedStrategy.getWriter()
        val reader = testedStrategy.getReader()

        // Then
        check(writer is BatchFileDataWriter)
        check(reader is BatchFileDataReader)
        assertThat(writer.decoration).isSameAs(reader.decoration)
    }

    @Test
    fun `𝕄 share handler 𝕎 getWriter() + getReader()`() {
        // Given

        // When
        val writer = testedStrategy.getWriter()
        val reader = testedStrategy.getReader()

        // Then
        check(writer is BatchFileDataWriter)
        check(reader is BatchFileDataReader)
        assertThat(writer.handler).isSameAs(reader.handler)
    }
}
