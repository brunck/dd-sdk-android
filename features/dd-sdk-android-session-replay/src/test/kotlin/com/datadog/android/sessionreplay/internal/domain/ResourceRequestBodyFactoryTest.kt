/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.domain

import com.datadog.android.api.storage.RawBatchEvent
import com.datadog.android.sessionreplay.forge.ForgeConfigurator
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.APPLICATION_ID_KEY
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.CONTENT_TYPE_IMAGE
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.FILENAME_BLOB
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.NAME_IMAGE
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.NAME_RESOURCE
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.TYPE_KEY
import com.datadog.android.sessionreplay.internal.domain.ResourceRequestBodyFactory.Companion.TYPE_RESOURCE
import com.google.gson.JsonObject
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class ResourceRequestBodyFactoryTest {
    private lateinit var testedRequestBodyFactory: ResourceRequestBodyFactory

    @StringForgery
    private lateinit var fakeApplicationId: String

    @StringForgery
    private lateinit var fakeFilename: String

    @StringForgery
    private lateinit var fakeImageRepresentation: String

    @BeforeEach
    fun `set up`() {
        testedRequestBodyFactory = ResourceRequestBodyFactory()
    }

    @Test
    fun `M return valid requestBody W create()`() {
        // Given
        val fakeRawBatchEvent = RawBatchEvent(
            data = fakeImageRepresentation.toByteArray(),
            metadata = fakeFilename.toByteArray()
        )
        val fakeListResources = listOf(fakeRawBatchEvent)

        // When
        val requestBody = testedRequestBodyFactory.create(fakeApplicationId, fakeListResources)

        // Then
        assertThat(requestBody).isInstanceOf(MultipartBody::class.java)
        assertThat(requestBody.contentType()?.type).isEqualTo(MultipartBody.FORM.type)
        assertThat(requestBody.contentType()?.subtype).isEqualTo(MultipartBody.FORM.subtype)

        val body = requestBody as MultipartBody
        val parts = body.parts

        val applicationIdJson = JsonObject()
        applicationIdJson.addProperty(APPLICATION_ID_KEY, fakeApplicationId)
        applicationIdJson.addProperty(TYPE_KEY, TYPE_RESOURCE)

        val applicationIdPart = MultipartBody.Part.createFormData(
            NAME_RESOURCE,
            FILENAME_BLOB,
            applicationIdJson.toString().toRequestBody(ResourceRequestBodyFactory.CONTENT_TYPE_APPLICATION)
        )

        val resourcesPart = MultipartBody.Part.createFormData(
            NAME_IMAGE,
            fakeFilename,
            fakeImageRepresentation.toByteArray().toRequestBody(CONTENT_TYPE_IMAGE)
        )

        assertThat(parts)
            .usingElementComparator { first, second ->
                val headersEval = first.headers == second.headers
                val bodyEval = first.body.toByteArray().contentEquals(second.body.toByteArray())
                if (headersEval && bodyEval) {
                    0
                } else {
                    -1
                }
            }
            .containsExactlyInAnyOrder(
                resourcesPart,
                applicationIdPart
            )
    }

    private fun RequestBody.toByteArray(): ByteArray {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readByteArray()
    }
}
