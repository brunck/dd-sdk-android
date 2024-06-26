/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.persistence.file.batch

import com.datadog.android.api.InternalLogger
import com.datadog.android.api.storage.RawBatchEvent
import com.datadog.android.core.internal.persistence.Batch
import com.datadog.android.core.internal.persistence.DataReader
import com.datadog.android.core.internal.persistence.PayloadDecoration
import com.datadog.android.core.internal.persistence.file.FileMover
import com.datadog.android.core.internal.persistence.file.FileOrchestrator
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
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@ForgeConfiguration(Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class BatchFileDataReaderTest {

    lateinit var testedReader: DataReader

    @Mock
    lateinit var mockOrchestrator: FileOrchestrator

    @Mock
    lateinit var mockFileReader: BatchFileReader

    @Mock
    lateinit var mockFileMover: FileMover

    @Mock
    lateinit var mockInternalLogger: InternalLogger

    @Forgery
    lateinit var fakeDecoration: PayloadDecoration

    @BeforeEach
    fun `set up`() {
        testedReader = BatchFileDataReader(
            mockOrchestrator,
            fakeDecoration,
            mockFileReader,
            mockFileMover,
            mockInternalLogger
        )
    }

    // region lockAndReadNext

    @Test
    fun `M read batch W lockAndReadNext()`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData

        // When
        val result = testedReader.lockAndReadNext()

        // Then
        checkNotNull(result)
        assertThat(result.id).isEqualTo(file.name)
        assertThat(result.data).isEqualTo(
            fakeData.map { it.data }.join(
                fakeDecoration.separatorBytes,
                fakeDecoration.prefixBytes,
                fakeDecoration.suffixBytes
            )
        )
    }

    @Test
    fun `M return null W lockAndReadNext() {no file}`() {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn null

        // When
        val result = testedReader.lockAndReadNext()

        // Then
        assertThat(result).isNull()
    }

    // endregion

    // region release

    @Test
    fun `M read batch twice W lockAndReadNext() + release() + lockAndReadNext()`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData

        // When
        val result1 = testedReader.lockAndReadNext()
        checkNotNull(result1)
        testedReader.release(result1)
        val result2 = testedReader.lockAndReadNext()

        // Then
        checkNotNull(result2)
        assertThat(result2.id).isEqualTo(file.name)
        assertThat(result2.data).isEqualTo(
            fakeData.map { it.data }.join(
                fakeDecoration.separatorBytes,
                fakeDecoration.prefixBytes,
                fakeDecoration.suffixBytes
            )
        )
        verify(mockFileMover, never()).delete(any())
    }

    @Test
    fun `M read batch twice W lockAndReadNext() + release() + lockAndReadNext() {multithreaded}`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData
        val countDownLatch = CountDownLatch(2)

        // When
        val result1 = testedReader.lockAndReadNext()
        checkNotNull(result1)
        var threadResult: Batch? = null
        Thread {
            Thread.sleep(100)
            threadResult = testedReader.lockAndReadNext()
            countDownLatch.countDown()
        }.start()
        Thread {
            testedReader.release(result1)
            countDownLatch.countDown()
        }.start()

        // Then
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        val result2 = threadResult
        checkNotNull(result2)
        assertThat(result2.id).isEqualTo(file.name)
        assertThat(result2.data).isEqualTo(
            fakeData.map { it.data }.join(
                fakeDecoration.separatorBytes,
                fakeDecoration.prefixBytes,
                fakeDecoration.suffixBytes
            )
        )
        verify(mockFileMover, never()).delete(any())
    }

    @Test
    fun `M read batch once W lockAndReadNext() + release() {diff} + lockAndReadNext()`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData

        // When
        val result1 = testedReader.lockAndReadNext()
        testedReader.release(Batch(file.name.reversed() + "0", ByteArray(0)))
        val result2 = testedReader.lockAndReadNext()

        // Then
        checkNotNull(result1)
        assertThat(result1.id).isEqualTo(file.name)
        assertThat(result1.data).isEqualTo(
            fakeData.map { it.data }.join(
                fakeDecoration.separatorBytes,
                fakeDecoration.prefixBytes,
                fakeDecoration.suffixBytes
            )
        )
        assertThat(result2).isNull()
        verify(mockFileMover, never()).delete(any())
    }

    @Test
    fun `M read and release multiple batches W lockAndReadNext() + release() { multithreaded }`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file1: File,
        @Forgery file2: File,
        @Forgery file3: File,
        @Forgery file4: File
    ) {
        // Given
        val files = listOf(file1, file2, file3, file4)
        val expectedIds = files.map { it.name }
        whenever(mockOrchestrator.getReadableFile(any())) doAnswer { invocation ->
            val set = invocation.getArgument<Set<String>>(0)
            files.first { it.name !in set }
        }
        whenever(mockFileReader.readData(any())) doReturn fakeData
        val countDownLatch = CountDownLatch(4)

        // When
        val results = mutableListOf<Batch?>()
        repeat(4) {
            Thread {
                val result = testedReader.lockAndReadNext()
                results.add(result)
                if (result != null) {
                    testedReader.release(result)
                }
                countDownLatch.countDown()
            }.start()
        }

        // Then
        countDownLatch.await(500, TimeUnit.MILLISECONDS)
        assertThat(results)
            .hasSize(4)
            .doesNotContainNull()
            .allMatch { it!!.id in expectedIds }
        verify(mockFileMover, never()).delete(any())
    }

    @Test
    fun `M warn W release() unknown file`(
        @StringForgery fileName: String
    ) {
        // Given
        val data = Batch(fileName, ByteArray(0))

        // When
        testedReader.release(data)

        // Then
        mockInternalLogger.verifyLog(
            level = InternalLogger.Level.WARN,
            target = InternalLogger.Target.MAINTAINER,
            BatchFileDataReader.WARNING_UNKNOWN_BATCH_ID.format(Locale.US, fileName)
        )
    }

    // endregion

    // region drop

    @Test
    fun `M delete underlying file+meta W lockAndReadNext() + dropBatch()`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File,
        forge: Forge
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        val metaFileMock = mock<File>().apply {
            whenever(exists()) doReturn true
            whenever(path) doReturn "${file.path}_${forge.anAlphabeticalString()}"
        }
        whenever(mockOrchestrator.getMetadataFile(file)) doReturn metaFileMock
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData
        whenever(mockFileMover.delete(file)) doReturn true

        // Then
        val result = testedReader.lockAndReadNext()
        checkNotNull(result)
        testedReader.drop(result)

        // Then
        verify(mockFileMover).delete(file)
        verify(mockFileMover).delete(metaFileMock)
    }

    @Test
    fun `M warn W lockAndReadNext() + dropBatch() {delete fails}`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData
        whenever(mockFileMover.delete(file)) doReturn false

        // Then
        val result = testedReader.lockAndReadNext()
        checkNotNull(result)
        testedReader.drop(result)

        // Then
        verify(mockFileMover).delete(file)
        mockInternalLogger.verifyLog(
            level = InternalLogger.Level.WARN,
            target = InternalLogger.Target.MAINTAINER,
            BatchFileDataReader.WARNING_DELETE_FAILED.format(Locale.US, file.path)
        )
    }

    @Test
    fun `M warn W drop() unknown file`(
        @StringForgery fileName: String
    ) {
        // Given
        val data = Batch(fileName, ByteArray(0))

        // When
        testedReader.drop(data)

        // Then
        mockInternalLogger.verifyLog(
            level = InternalLogger.Level.WARN,
            target = InternalLogger.Target.MAINTAINER,
            BatchFileDataReader.WARNING_UNKNOWN_BATCH_ID.format(Locale.US, fileName)
        )
    }

    // endregion

    // region dropAll

    @Test
    fun `M delete underlying file W lockAndReadNext() + dropAll()`(
        @Forgery fakeData: List<RawBatchEvent>,
        @Forgery file: File,
        forge: Forge
    ) {
        // Given
        whenever(mockOrchestrator.getReadableFile(emptySet())) doReturn file
        val metaFileMock = mock<File>().apply {
            whenever(exists()) doReturn true
            whenever(path) doReturn "${file.path}_${forge.anAlphabeticalString()}"
        }
        whenever(mockOrchestrator.getMetadataFile(file)) doReturn metaFileMock
        whenever(mockOrchestrator.getAllFiles()) doReturn emptyList()
        whenever(
            mockFileReader.readData(file)
        ) doReturn fakeData
        whenever(mockFileMover.delete(file)) doReturn true

        // Then
        val result = testedReader.lockAndReadNext()
        checkNotNull(result)
        testedReader.dropAll()

        // Then
        verify(mockFileMover).delete(file)
        verify(mockFileMover).delete(metaFileMock)
    }

    @Test
    fun `M delete all files+meta W lockAndReadNext() + dropAll()`(
        @Forgery file1: File,
        @Forgery file2: File,
        @Forgery file3: File,
        @Forgery file4: File,
        forge: Forge
    ) {
        // Given
        val files = listOf(file1, file2, file3, file4)
        whenever(mockOrchestrator.getAllFiles()) doReturn files
        whenever(mockFileMover.delete(any())) doReturn true

        val metaFileMocks = files.map {
            mock<File>().apply {
                whenever(exists()) doReturn true
                whenever(path) doReturn "${it.path}_${forge.anAlphabeticalString()}"
            }
        }

        whenever(mockOrchestrator.getMetadataFile(any())) doReturnConsecutively metaFileMocks

        // Then
        testedReader.dropAll()

        // Then
        verify(mockFileMover).delete(file1)
        verify(mockFileMover).delete(metaFileMocks[0])
        verify(mockFileMover).delete(file2)
        verify(mockFileMover).delete(metaFileMocks[1])
        verify(mockFileMover).delete(file3)
        verify(mockFileMover).delete(metaFileMocks[2])
        verify(mockFileMover).delete(file4)
        verify(mockFileMover).delete(metaFileMocks[3])
    }

    // endregion

    // region private

    private fun List<ByteArray>.join(
        separator: ByteArray,
        prefix: ByteArray,
        suffix: ByteArray
    ): ByteArray {
        return prefix + this.reduce { acc, bytes ->
            acc + separator + bytes
        } + suffix
    }

    // endregion
}
