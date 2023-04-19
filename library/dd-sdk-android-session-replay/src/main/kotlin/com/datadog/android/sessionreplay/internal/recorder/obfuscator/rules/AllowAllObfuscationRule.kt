/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.obfuscator.rules

import android.widget.TextView
import com.datadog.android.sessionreplay.internal.recorder.obfuscator.FixedLengthStringObfuscator

internal class AllowAllObfuscationRule(
    private val fixedLengthStringObfuscator: FixedLengthStringObfuscator =
        FixedLengthStringObfuscator(),
    private val textTypeResolver: TextTypeResolver = TextTypeResolver(),
    private val textValueResolver: TextValueResolver = TextValueResolver()
) : TextValueObfuscationRule {
    override fun resolveObfuscatedValue(textView: TextView): String {
        val textType = textTypeResolver.resolveTextType(textView)
        val textValue = textValueResolver.resolveTextValue(textView)
        return if (textType == TextType.SENSITIVE_TEXT) {
            fixedLengthStringObfuscator.obfuscate(textValue)
        } else {
            textValue
        }
    }
}
