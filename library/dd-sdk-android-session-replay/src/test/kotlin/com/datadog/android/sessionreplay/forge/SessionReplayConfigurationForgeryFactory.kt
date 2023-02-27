/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.forge

import com.datadog.android.sessionreplay.SessionReplayConfiguration
import com.datadog.android.sessionreplay.SessionReplayPrivacy
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class SessionReplayConfigurationForgeryFactory :
    ForgeryFactory<SessionReplayConfiguration> {
    override fun getForgery(forge: Forge): SessionReplayConfiguration {
        return SessionReplayConfiguration(
            endpointUrl = forge.aStringMatching("http(s?)://[a-z]+\\.com/\\w+"),
            privacy = forge.aValueFrom(SessionReplayPrivacy::class.java)
        )
    }
}