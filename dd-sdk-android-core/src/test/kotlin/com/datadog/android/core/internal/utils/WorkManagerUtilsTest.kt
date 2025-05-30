/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.utils

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.impl.WorkManagerImpl
import com.datadog.android.api.InternalLogger
import com.datadog.android.core.UploadWorker
import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.utils.config.ApplicationContextTestConfiguration
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.datadog.tools.unit.setStaticValue
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class WorkManagerUtilsTest {

    @Mock
    lateinit var mockWorkManager: WorkManagerImpl

    @Mock
    lateinit var mockInternalLogger: InternalLogger

    @StringForgery
    lateinit var fakeInstanceName: String

    @BeforeEach
    fun `set up`() {
        CoreFeature.disableKronosBackgroundSync = true

        whenever(mockWorkManager.cancelAllWorkByTag(anyString())) doReturn mock()
        whenever(
            mockWorkManager.enqueueUniqueWork(
                anyString(),
                any(),
                any<OneTimeWorkRequest>()
            )
        ) doReturn mock()
    }

    @AfterEach
    fun `tear down`() {
        WorkManagerImpl::class.java.setStaticValue("sDefaultInstance", null)
    }

    @Test
    fun `it will cancel the worker if WorkManager was correctly instantiated`() {
        // Given
        WorkManagerImpl::class.java.setStaticValue("sDefaultInstance", mockWorkManager)

        // When
        cancelUploadWorker(appContext.mockInstance, fakeInstanceName, mockInternalLogger)

        // Then
        verify(mockWorkManager).cancelAllWorkByTag("$TAG_DATADOG_UPLOAD/$fakeInstanceName")
    }

    @Test
    fun `it will handle the cancel exception if WorkManager was not correctly instantiated`() {
        // When
        cancelUploadWorker(appContext.mockInstance, fakeInstanceName, mockInternalLogger)

        // Then
        verifyNoInteractions(mockWorkManager)
    }

    @Test
    fun `it will schedule the worker if WorkManager was correctly instantiated`() {
        // Given
        WorkManagerImpl::class.java.setStaticValue("sDefaultInstance", mockWorkManager)

        // When
        triggerUploadWorker(appContext.mockInstance, fakeInstanceName, mockInternalLogger)

        // Then
        argumentCaptor<OneTimeWorkRequest> {
            verify(mockWorkManager).enqueueUniqueWork(
                eq(UPLOAD_WORKER_NAME),
                eq(ExistingWorkPolicy.REPLACE),
                capture()
            )
            val workSpec = lastValue.workSpec
            assertThat(workSpec.workerClassName).isEqualTo(UploadWorker::class.java.canonicalName)
            assertThat(workSpec.input.getString(UploadWorker.DATADOG_INSTANCE_NAME)).isEqualTo(fakeInstanceName)
            assertThat(lastValue.tags).contains("$TAG_DATADOG_UPLOAD/$fakeInstanceName")
        }
    }

    @Test
    fun `it will handle the trigger exception if WorkManager was not correctly instantiated`() {
        // When
        triggerUploadWorker(appContext.mockInstance, fakeInstanceName, mockInternalLogger)

        // Then
        verifyNoInteractions(mockWorkManager)
    }

    companion object {
        val appContext = ApplicationContextTestConfiguration(Application::class.java)

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(appContext)
        }
    }
}
