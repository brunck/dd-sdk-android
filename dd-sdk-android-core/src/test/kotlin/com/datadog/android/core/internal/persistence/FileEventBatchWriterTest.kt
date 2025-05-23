/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.persistence

import com.datadog.android.api.InternalLogger
import com.datadog.android.api.storage.EventBatchWriter
import com.datadog.android.api.storage.EventType
import com.datadog.android.api.storage.RawBatchEvent
import com.datadog.android.core.internal.persistence.FileEventBatchWriter.Companion.ERROR_LARGE_DATA
import com.datadog.android.core.internal.persistence.FileEventBatchWriter.Companion.WARNING_METADATA_WRITE_FAILED
import com.datadog.android.core.internal.persistence.file.FilePersistenceConfig
import com.datadog.android.core.internal.persistence.file.FileReaderWriter
import com.datadog.android.core.internal.persistence.file.FileWriter
import com.datadog.android.utils.forge.Configurator
import com.datadog.android.utils.verifyLog
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.util.Locale

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class FileEventBatchWriterTest {

    private lateinit var testedWriter: EventBatchWriter

    @Mock
    lateinit var mockBatchWriter: FileWriter<RawBatchEvent>

    @Mock
    lateinit var mockMetaReaderWriter: FileReaderWriter

    @Mock
    lateinit var mockInternalLogger: InternalLogger

    @Mock
    lateinit var mockFilePersistenceConfig: FilePersistenceConfig

    @Mock
    lateinit var mockBatchWriteEventListener: BatchWriteEventListener

    @Forgery
    lateinit var fakeBatchFile: File

    @Forgery
    lateinit var fakeBatchMetadataFile: File

    @Forgery
    lateinit var fakeEventType: EventType

    @BeforeEach
    fun `set up`() {
        testedWriter = FileEventBatchWriter(
            batchFile = fakeBatchFile,
            metadataFile = fakeBatchMetadataFile,
            eventsWriter = mockBatchWriter,
            metadataReaderWriter = mockMetaReaderWriter,
            filePersistenceConfig = mockFilePersistenceConfig,
            internalLogger = mockInternalLogger,
            batchWriteEventListener = mockBatchWriteEventListener
        )
        whenever(mockFilePersistenceConfig.maxItemSize) doReturn Long.MAX_VALUE
    }

    // region write

    @Test
    fun `M write event W write()`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        whenever(mockMetaReaderWriter.readData(fakeBatchMetadataFile)) doReturn serializedMetadata
        whenever(
            mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)
        ) doReturn true

        // When
        val result = testedWriter.write(batchEvent, serializedMetadata, fakeEventType)

        // Then
        assertThat(result).isTrue()

        verify(mockBatchWriter).writeData(
            fakeBatchFile,
            batchEvent,
            append = true
        )
        verify(mockMetaReaderWriter).writeData(
            fakeBatchMetadataFile,
            serializedMetadata,
            append = false
        )

        verifyNoMoreInteractions(
            mockBatchWriter,
            mockMetaReaderWriter
        )
    }

    @Test
    fun `M do nothing W write() {empty array}`(
        @StringForgery batchMetadata: String
    ) {
        // Given
        val rawBatchEvent = RawBatchEvent(data = ByteArray(0))
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)

        // When
        val result = testedWriter.write(rawBatchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isTrue

        verifyNoInteractions(
            mockBatchWriter,
            mockMetaReaderWriter
        )
    }

    @Test
    fun `M return false W write() {item is too big}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        val maxItemSize = batchEvent.data.size - 1
        whenever(mockFilePersistenceConfig.maxItemSize) doReturn maxItemSize.toLong()

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isFalse

        mockInternalLogger.verifyLog(
            InternalLogger.Level.ERROR,
            InternalLogger.Target.USER,
            ERROR_LARGE_DATA.format(
                Locale.US,
                batchEvent.data.size,
                maxItemSize
            )
        )
    }

    @Test
    fun `M return false W write() {write operation failed}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String,
        @Forgery batchFile: File
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        whenever(mockBatchWriter.writeData(batchFile, batchEvent, true)) doReturn false

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isFalse
    }

    @Test
    fun `M not write metadata W write() {no available file}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        testedWriter = FileEventBatchWriter(
            batchFile = fakeBatchFile,
            metadataFile = null,
            eventsWriter = mockBatchWriter,
            metadataReaderWriter = mockMetaReaderWriter,
            filePersistenceConfig = mockFilePersistenceConfig,
            internalLogger = mockInternalLogger,
            batchWriteEventListener = mockBatchWriteEventListener
        )

        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)

        whenever(mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)) doReturn true

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isTrue

        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M not write metadata W write() {null or empty metadata}`(
        @Forgery batchEvent: RawBatchEvent,
        forge: Forge
    ) {
        // Given
        whenever(mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)) doReturn true

        // When
        val result = testedWriter.write(batchEvent, forge.aNullable { ByteArray(0) }, fakeEventType)

        // Then
        assertThat(result).isTrue

        verify(mockBatchWriter).writeData(
            fakeBatchFile,
            batchEvent,
            true
        )
        verifyNoMoreInteractions(mockBatchWriter)
        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M not write metadata W write() {item is too big}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        val maxItemSize = batchEvent.data.size - 1
        whenever(mockFilePersistenceConfig.maxItemSize) doReturn maxItemSize.toLong()

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isFalse

        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M not write metadata W write() {write operation failed}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        whenever(
            mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)
        ) doReturn false

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isFalse

        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M log warning W write() {write metadata failed}`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        whenever(
            mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)
        ) doReturn true
        whenever(
            mockMetaReaderWriter.writeData(fakeBatchMetadataFile, serializedBatchMetadata, false)
        ) doReturn false

        // When
        val result = testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        assertThat(result).isTrue()

        mockInternalLogger.verifyLog(
            InternalLogger.Level.WARN,
            InternalLogger.Target.USER,
            WARNING_METADATA_WRITE_FAILED.format(
                Locale.US,
                fakeBatchMetadataFile
            )
        )
    }

    // endregion

    // region currentMetadata

    @Test
    fun `M not read metadata W currentMetadata() {no available file}`() {
        // Given
        testedWriter = FileEventBatchWriter(
            batchFile = fakeBatchFile,
            metadataFile = null,
            eventsWriter = mockBatchWriter,
            metadataReaderWriter = mockMetaReaderWriter,
            filePersistenceConfig = mockFilePersistenceConfig,
            internalLogger = mockInternalLogger,
            batchWriteEventListener = mockBatchWriteEventListener
        )

        // When
        val meta = testedWriter.currentMetadata()

        // Then
        assertThat(meta).isNull()
        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M not read metadata W currentMetadata() { file doesn't exist }`() {
        // Given
        val metaFile = mock<File>().apply {
            whenever(exists()) doReturn false
        }

        testedWriter = FileEventBatchWriter(
            batchFile = fakeBatchFile,
            metadataFile = metaFile,
            eventsWriter = mockBatchWriter,
            metadataReaderWriter = mockMetaReaderWriter,
            filePersistenceConfig = mockFilePersistenceConfig,
            internalLogger = mockInternalLogger,
            batchWriteEventListener = mockBatchWriteEventListener
        )

        // When
        val meta = testedWriter.currentMetadata()

        // Then
        assertThat(meta).isNull()
        verifyNoInteractions(mockMetaReaderWriter)
    }

    @Test
    fun `M read metadata W currentMetadata()`(
        @StringForgery fakeMetadata: String,
        @TempDir fakeMetadataDir: File,
        forge: Forge
    ) {
        // Given
        val fakeMetaFile = File(fakeMetadataDir, forge.anAlphabeticalString())
        fakeMetaFile.createNewFile()

        testedWriter = FileEventBatchWriter(
            batchFile = fakeBatchFile,
            metadataFile = fakeMetaFile,
            eventsWriter = mockBatchWriter,
            metadataReaderWriter = mockMetaReaderWriter,
            filePersistenceConfig = mockFilePersistenceConfig,
            internalLogger = mockInternalLogger,
            batchWriteEventListener = mockBatchWriteEventListener
        )
        whenever(mockMetaReaderWriter.readData(fakeMetaFile)) doReturn
            fakeMetadata.toByteArray()

        // When
        val meta = testedWriter.currentMetadata()

        // Then
        assertThat(meta).isEqualTo(fakeMetadata.toByteArray())
    }

    // endregion

    // region benchmark

    @Test
    fun `M send onWriteEvent W write`(
        @Forgery batchEvent: RawBatchEvent,
        @StringForgery batchMetadata: String
    ) {
        // Given
        val serializedBatchMetadata = batchMetadata.toByteArray(Charsets.UTF_8)
        whenever(
            mockBatchWriter.writeData(fakeBatchFile, batchEvent, true)
        ) doReturn true
        whenever(
            mockMetaReaderWriter.writeData(fakeBatchMetadataFile, serializedBatchMetadata, false)
        ) doReturn false

        // When
        testedWriter.write(batchEvent, serializedBatchMetadata, fakeEventType)

        // Then
        verify(mockBatchWriteEventListener).onWriteEvent(batchEvent.data.size.toLong())
    }

    // endregion
}
