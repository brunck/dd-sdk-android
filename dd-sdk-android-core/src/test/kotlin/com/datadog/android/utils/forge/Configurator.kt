/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.utils.forge

import com.datadog.android.internal.tests.elmyr.InternalTelemetryApiUsageForgeryFactory
import com.datadog.android.internal.tests.elmyr.TracingHeaderTypesSetForgeryFactory
import com.datadog.android.test.elmyr.PersistenceStrategyBatchForgeryFactory
import com.datadog.android.tests.elmyr.useCoreFactories
import com.datadog.tools.unit.forge.BaseConfigurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.jvm.useJvmFactories

internal class Configurator :
    BaseConfigurator() {
    override fun configure(forge: Forge) {
        super.configure(forge)
        forge.useCoreFactories()

        // Datadog Core
        forge.addFactory(CustomAttributesForgeryFactory())
        forge.addFactory(ConfigurationForgeryFactory())
        forge.addFactory(ConfigurationCoreForgeryFactory())
        forge.addFactory(ConfigurationForgeryFactory())
        forge.addFactory(FilePersistenceConfigForgeryFactory())
        forge.addFactory(AndroidInfoProviderForgeryFactory())
        forge.addFactory(FeatureStorageConfigurationForgeryFactory())
        forge.addFactory(BatchDataForgeryFactory())
        forge.addFactory(BatchIdForgeryFactory())

        // IO
        forge.addFactory(BatchForgeryFactory())
        forge.addFactory(PayloadDecorationForgeryFactory())
        forge.addFactory(WorkerParametersForgeryFactory())

        // NDK Crash
        forge.addFactory(NdkCrashLogForgeryFactory())

        // MISC
        forge.addFactory(BigIntegerFactory())
        forge.addFactory(CharsetForgeryFactory())

        // Datadog SDK v2
        forge.addFactory(DataUploadConfigurationForgeryFactory())

        // UploadStatus
        forge.addFactory(DNSErrorStatusForgeryFactory())
        forge.addFactory(HttpClientErrorForgeryFactory())
        forge.addFactory(HttpClientRateLimitingStatusForgeryFactory())
        forge.addFactory(HttpRedirectStatusForgeryFactory())
        forge.addFactory(HttpServerErrorForgeryFactory())
        forge.addFactory(InvalidTokenErrorStatusForgeryFactory())
        forge.addFactory(NetworkErrorStatusForgeryFactory())
        forge.addFactory(RequestCreationErrorStatusForgeryFactory())
        forge.addFactory(SuccessStatusForgeryFactory())
        forge.addFactory(UnknownExceptionStatusForgeryFactory())
        forge.addFactory(UnknownHttpErrorStatusForgeryFactory())
        forge.addFactory(UnknownStatusForgeryFactory())

        // RemovalReason
        forge.addFactory(RemovalReasonFlushedForgeryFactory())
        forge.addFactory(RemovalReasonPurgedForgeryFactory())
        forge.addFactory(RemovalReasonInvalidForgeryFactory())
        forge.addFactory(RemovalReasonObsoleteForgeryFactory())
        forge.addFactory(RemovalReasonIntakeCodeForgeryFactory())
        forge.addFactory(RemovalReasonForgeryFactory())

        forge.addFactory(BatchClosedMetadataForgeryFactory())

        forge.addFactory(PersistenceStrategyBatchForgeryFactory())

        forge.useJvmFactories()

        // telemetry
        forge.addFactory(InternalTelemetryApiUsageForgeryFactory())
        forge.addFactory(TracingHeaderTypesSetForgeryFactory())
    }
}
