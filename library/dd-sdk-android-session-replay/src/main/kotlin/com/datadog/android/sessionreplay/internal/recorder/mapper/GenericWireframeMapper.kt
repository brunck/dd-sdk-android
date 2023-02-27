/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.model.MobileSegment

internal abstract class GenericWireframeMapper(
    private val viewWireframeMapper: ViewWireframeMapper,
    internal val imageMapper: ViewScreenshotWireframeMapper,
    private val textMapper: TextWireframeMapper,
    private val buttonMapper: ButtonMapper,
    private val editTextViewMapper: EditTextViewMapper,
    private val checkedTextViewMapper: CheckedTextViewMapper,
    private val decorViewMapper: DecorViewMapper,
    private val checkBoxMapper: CheckBoxMapper,
    private val radioButtonMapper: RadioButtonMapper = RadioButtonMapper(textMapper)
) : WireframeMapper<View, MobileSegment.Wireframe> {

    override fun map(view: View, systemInformation: SystemInformation):
        List<MobileSegment.Wireframe> {
        return when {
            RadioButton::class.java.isAssignableFrom(view::class.java) -> {
                radioButtonMapper.map(view as RadioButton, systemInformation)
            }
            CheckBox::class.java.isAssignableFrom(view::class.java) -> {
                checkBoxMapper.map(view as CheckBox, systemInformation)
            }
            CheckedTextView::class.java.isAssignableFrom(view::class.java) -> {
                checkedTextViewMapper.map(
                    view as CheckedTextView,
                    systemInformation
                )
            }
            Button::class.java.isAssignableFrom(view::class.java) -> {
                buttonMapper.map(view as Button, systemInformation)
            }
            EditText::class.java.isAssignableFrom(view::class.java) -> {
                editTextViewMapper.map(view as EditText, systemInformation)
            }
            TextView::class.java.isAssignableFrom(view::class.java) -> {
                textMapper.map(view as TextView, systemInformation)
            }
            ImageView::class.java.isAssignableFrom(view::class.java) -> {
                imageMapper.map(view as ImageView, systemInformation)
            }
            else -> {
                val viewParent = view.parent
                if (viewParent == null ||
                    !View::class.java.isAssignableFrom(viewParent::class.java)
                ) {
                    decorViewMapper.map(view, systemInformation)
                } else {
                    viewWireframeMapper.map(view, systemInformation)
                }
            }
        }
    }
}