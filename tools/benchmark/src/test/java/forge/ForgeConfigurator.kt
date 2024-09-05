/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package forge

import com.datadog.tools.unit.forge.BaseConfigurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.jvm.useJvmFactories

internal class ForgeConfigurator : BaseConfigurator() {

    override fun configure(forge: Forge) {
        super.configure(forge)

        // Session Replay
        forge.addFactory(BenchmarkContextForgeryFactory())
        forge.addFactory(MetricDataForgeryFactory())
        forge.addFactory(GaugeDataForgeryFactory())
        forge.addFactory(PointDataForgeryFactory())
        forge.addFactory(DatadogExporterConfigurationForgeryFactory())
        forge.addFactory(SpanEventForgeryFactory())
        forge.addFactory(NetworkInfoForgeryFactory())
        forge.addFactory(UserInfoForgeryFactory())
        forge.useJvmFactories()
    }
}
