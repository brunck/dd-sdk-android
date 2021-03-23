/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.persistence.file.advanced

import com.datadog.android.core.internal.persistence.file.FileHandler
import com.datadog.android.core.internal.utils.retryWithDelay
import com.datadog.android.log.Logger
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A [DataMigrationOperation] that moves all the files in the `fromDir` directory
 * to the `toDir` directory.
 */
internal class MoveDataMigrationOperation(
    internal val fromDir: File?,
    internal val toDir: File?,
    internal val fileHandler: FileHandler,
    internal val internalLogger: Logger
) : DataMigrationOperation {

    override fun run() {
        if (fromDir == null) {
            internalLogger.w(WARN_NULL_SOURCE_DIR)
        } else if (toDir == null) {
            internalLogger.w(WARN_NULL_DEST_DIR)
        } else {
            retryWithDelay(MAX_RETRY, RETRY_DELAY_NS) {
                fileHandler.moveFiles(fromDir, toDir)
            }
        }
    }

    companion object {
        internal const val WARN_NULL_SOURCE_DIR = "Can't move data from a null directory"
        internal const val WARN_NULL_DEST_DIR = "Can't move data to a null directory"

        private const val MAX_RETRY = 3
        private val RETRY_DELAY_NS = TimeUnit.MILLISECONDS.toNanos(500)
    }
}
