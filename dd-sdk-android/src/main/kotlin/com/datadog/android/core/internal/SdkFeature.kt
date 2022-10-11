/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

@file:Suppress("DEPRECATION")

package com.datadog.android.core.internal

import android.content.Context
import com.datadog.android.core.internal.data.upload.NoOpUploadScheduler
import com.datadog.android.core.internal.data.upload.UploadScheduler
import com.datadog.android.core.internal.persistence.file.FileMover
import com.datadog.android.core.internal.persistence.file.FileOrchestrator
import com.datadog.android.core.internal.persistence.file.FileReaderWriter
import com.datadog.android.core.internal.persistence.file.NoOpFileOrchestrator
import com.datadog.android.core.internal.persistence.file.advanced.FeatureFileOrchestrator
import com.datadog.android.core.internal.persistence.file.batch.BatchFileReaderWriter
import com.datadog.android.core.internal.privacy.ConsentProvider
import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.plugin.DatadogPlugin
import com.datadog.android.plugin.DatadogPluginConfig
import com.datadog.android.v2.api.EventBatchWriter
import com.datadog.android.v2.api.FeatureScope
import com.datadog.android.v2.api.FeatureStorageConfiguration
import com.datadog.android.v2.api.FeatureUploadConfiguration
import com.datadog.android.v2.api.NoOpInternalLogger
import com.datadog.android.v2.api.context.DatadogContext
import com.datadog.android.v2.core.internal.NoOpContextProvider
import com.datadog.android.v2.core.internal.data.upload.DataFlusher
import com.datadog.android.v2.core.internal.data.upload.DataUploadScheduler
import com.datadog.android.v2.core.internal.net.DataOkHttpUploader
import com.datadog.android.v2.core.internal.net.DataUploader
import com.datadog.android.v2.core.internal.net.NoOpDataUploader
import com.datadog.android.v2.core.internal.storage.ConsentAwareStorage
import com.datadog.android.v2.core.internal.storage.NoOpStorage
import com.datadog.android.v2.core.internal.storage.Storage
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
internal class SdkFeature(
    internal val coreFeature: CoreFeature,
    private val featureName: String,
    private val storageConfiguration: FeatureStorageConfiguration,
    private val uploadConfiguration: FeatureUploadConfiguration
) : FeatureScope {

    internal val initialized = AtomicBoolean(false)

    internal var storage: Storage = NoOpStorage()
    internal var uploader: DataUploader = NoOpDataUploader()
    internal var uploadScheduler: UploadScheduler = NoOpUploadScheduler()
    internal var fileOrchestrator: FileOrchestrator = NoOpFileOrchestrator()
    private val featurePlugins: MutableList<DatadogPlugin> = mutableListOf()

    // region SDK Feature

    fun initialize(context: Context, plugins: List<DatadogPlugin>) {
        if (initialized.get()) {
            return
        }

        storage = createStorage(featureName, storageConfiguration)

        setupUploader()

        registerPlugins(
            plugins,
            DatadogPluginConfig(
                context = context,
                storageDir = coreFeature.storageDir,
                envName = coreFeature.envName,
                serviceName = coreFeature.serviceName,
                trackingConsent = coreFeature.trackingConsentProvider.getConsent()
            ),
            coreFeature.trackingConsentProvider
        )

        onInitialize()

        initialized.set(true)

        onPostInitialized()
    }

    fun isInitialized(): Boolean {
        return initialized.get()
    }

    fun clearAllData() {
        @Suppress("ThreadSafety") // TODO RUMM-1503 delegate to another thread
        storage.dropAll()
    }

    fun stop() {
        if (initialized.get()) {
            unregisterPlugins()
            uploadScheduler.stopScheduling()
            uploadScheduler = NoOpUploadScheduler()
            storage = NoOpStorage()
            uploader = NoOpDataUploader()
            fileOrchestrator = NoOpFileOrchestrator()

            onStop()

            initialized.set(false)
            onPostStopped()
        }
    }

    @Deprecated("Plugins won't work that way in SDK v2")
    fun getPlugins(): List<DatadogPlugin> {
        return featurePlugins
    }

    // endregion

    // region FeatureScope

    // TODO RUMM-0000 there is no thread switch here, it stays the same.
    // Need to clarify the threading. We either switch thread here or in Storage.
    // Or give the ability to specify the executor to the caller.
    @Suppress("ThreadSafety")
    override fun withWriteContext(callback: (DatadogContext, EventBatchWriter) -> Unit) {
        val contextProvider = coreFeature.contextProvider
        if (contextProvider is NoOpContextProvider) return
        val context = contextProvider.context
        storage.writeCurrentBatch(context) {
            callback(context, it)
        }
    }

    // endregion

    // region Lifecycle

    // TODO RUMM-0000 Should be moved out, to the public API of feature registration
    @Suppress("EmptyFunctionBlock")
    fun onInitialize() {}

    @Suppress("EmptyFunctionBlock")
    fun onPostInitialized() {}

    @Suppress("EmptyFunctionBlock")
    fun onStop() {}

    @Suppress("EmptyFunctionBlock")
    fun onPostStopped() {}

    // endregion

    // region Internal

    private fun registerPlugins(
        plugins: List<DatadogPlugin>,
        config: DatadogPluginConfig,
        trackingConsentProvider: ConsentProvider
    ) {
        plugins.forEach {
            featurePlugins.add(it)
            it.register(config)
            trackingConsentProvider.registerCallback(it)
        }
    }

    private fun unregisterPlugins() {
        featurePlugins.forEach {
            it.unregister()
        }
        featurePlugins.clear()
    }

    private fun setupUploader() {
        uploadScheduler = if (coreFeature.isMainProcess) {
            uploader = createUploader(uploadConfiguration)
            DataUploadScheduler(
                storage,
                uploader,
                coreFeature.contextProvider,
                coreFeature.networkInfoProvider,
                coreFeature.systemInfoProvider,
                coreFeature.uploadFrequency,
                coreFeature.uploadExecutorService
            )
        } else {
            NoOpUploadScheduler()
        }
        uploadScheduler.startScheduling()
    }

    // region Feature setup

    private fun createStorage(
        featureName: String,
        storageConfiguration: FeatureStorageConfiguration
    ): Storage {
        val fileOrchestrator = FeatureFileOrchestrator(
            consentProvider = coreFeature.trackingConsentProvider,
            storageDir = coreFeature.storageDir,
            featureName = featureName,
            executorService = coreFeature.persistenceExecutorService,
            internalLogger = sdkLogger
        )
        this.fileOrchestrator = fileOrchestrator

        return ConsentAwareStorage(
            grantedOrchestrator = fileOrchestrator.grantedOrchestrator,
            pendingOrchestrator = fileOrchestrator.pendingOrchestrator,
            batchEventsReaderWriter = BatchFileReaderWriter.create(
                internalLogger = sdkLogger,
                encryption = coreFeature.localDataEncryption
            ),
            batchMetadataReaderWriter = FileReaderWriter.create(
                internalLogger = sdkLogger,
                encryption = coreFeature.localDataEncryption
            ),
            fileMover = FileMover(sdkLogger),
            // TODO RUMM-0000 create internal logger
            internalLogger = NoOpInternalLogger(),
            filePersistenceConfig = coreFeature.buildFilePersistenceConfig().copy(
                maxBatchSize = storageConfiguration.maxBatchSize,
                maxItemSize = storageConfiguration.maxItemSize,
                maxItemsPerBatch = storageConfiguration.maxItemsPerBatch,
                oldFileThreshold = storageConfiguration.oldBatchThreshold
            )
        )
    }

    private fun createUploader(uploadConfiguration: FeatureUploadConfiguration): DataUploader {
        return DataOkHttpUploader(
            requestFactory = uploadConfiguration.requestFactory,
            internalLogger = sdkLogger,
            callFactory = coreFeature.okHttpClient,
            sdkVersion = coreFeature.sdkVersion,
            androidInfoProvider = coreFeature.androidInfoProvider
        )
    }

    // endregion

    // Used for nightly tests only
    internal fun flushStoredData() {
        // TODO RUMM-0000 should it just accept storage?
        val flusher = DataFlusher(
            coreFeature.contextProvider,
            fileOrchestrator,
            BatchFileReaderWriter.create(sdkLogger, coreFeature.localDataEncryption),
            FileReaderWriter.create(sdkLogger, coreFeature.localDataEncryption),
            FileMover(sdkLogger)
        )
        @Suppress("ThreadSafety")
        flusher.flush(uploader)
    }

    // endregion
}
