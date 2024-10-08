/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

import com.datadog.gradle.plugin.apisurface.ApiSurfacePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val generateTelemetryModelsTaskName = "generateTelemetryModelsFromJson"

tasks.register(
    generateTelemetryModelsTaskName,
    com.datadog.gradle.plugin.jsonschema.GenerateJsonSchemaTask::class.java
) {
    inputDirPath = "src/main/json/telemetry"
    targetPackageName = "com.datadog.android.telemetry.model"
    ignoredFiles = arrayOf(
        "_common-schema.json"
    )
    inputNameMapping = mapOf(
        "debug-schema.json" to "TelemetryDebugEvent",
        "error-schema.json" to "TelemetryErrorEvent"
    )
}

afterEvaluate {
    tasks.findByName(ApiSurfacePlugin.TASK_GEN_KOTLIN_API_SURFACE)
        ?.dependsOn(generateTelemetryModelsTaskName)
    tasks.withType(KotlinCompile::class.java).configureEach {
        dependsOn(generateTelemetryModelsTaskName)
    }
}
