/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.callback

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.Window
import com.datadog.android.api.InternalLogger
import com.datadog.android.sessionreplay.ImagePrivacy
import com.datadog.android.sessionreplay.TextAndInputPrivacy
import com.datadog.android.sessionreplay.forge.ForgeConfigurator
import com.datadog.android.sessionreplay.internal.TouchPrivacyManager
import com.datadog.android.sessionreplay.internal.async.RecordedDataQueueHandler
import com.datadog.android.sessionreplay.internal.async.TouchEventRecordedDataQueueItem
import com.datadog.android.sessionreplay.internal.recorder.ViewOnDrawInterceptor
import com.datadog.android.sessionreplay.internal.recorder.WindowInspector
import com.datadog.android.sessionreplay.internal.utils.TimeProvider
import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.model.MobileSegment.MobileIncrementalData
import com.datadog.android.sessionreplay.model.MobileSegment.MobileRecord
import com.datadog.android.utils.verifyLog
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.LongForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Intrinsics
import kotlin.math.pow

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class RecorderWindowCallbackTest {

    private lateinit var testedWindowCallback: RecorderWindowCallback

    @Mock
    lateinit var mockRecordedDataQueueHandler: RecordedDataQueueHandler

    @Mock
    lateinit var mockWrappedCallback: Window.Callback

    @Mock
    lateinit var mockViewOnDrawInterceptor: ViewOnDrawInterceptor

    @Mock
    lateinit var mockTouchPrivacyManager: TouchPrivacyManager

    @Mock
    lateinit var mockTimeProvider: TimeProvider

    @Mock
    lateinit var mockWindowInspector: WindowInspector

    @Mock
    lateinit var mockContext: Context

    @Forgery
    lateinit var fakeTouchEventRecordedDataQueueItem: TouchEventRecordedDataQueueItem

    @Mock
    lateinit var mockInternalLogger: InternalLogger

    @LongForgery(min = 0)
    var fakeTimestamp: Long = 0L

    // we will use ints here to avoid flakiness when converting to longs. It can happen
    // that the expected position to be 1 value higher due to the decimals conversion to longs when
    // normalizing with the device density.
    // This will not affect the tests validity in any way.
    @IntForgery(min = 1, max = 10)
    var fakeDensity: Int = 1

    @Mock
    lateinit var mockEventUtils: MotionEventUtils

    @Forgery
    lateinit var fakeTextAndInputPrivacy: TextAndInputPrivacy

    @BeforeEach
    fun `set up`() {
        val mockResources = mock<Resources> {
            val displayMetrics = DisplayMetrics().apply { density = fakeDensity.toFloat() }
            whenever(it.displayMetrics).thenReturn(displayMetrics)
        }
        whenever(mockRecordedDataQueueHandler.addTouchEventItem(any()))
            .thenReturn(fakeTouchEventRecordedDataQueueItem)
        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockTimeProvider.getDeviceTimestamp()).thenReturn(fakeTimestamp)
        whenever(mockTouchPrivacyManager.shouldRecordTouch(any()))
            .thenReturn(true)
        testedWindowCallback = RecorderWindowCallback(
            appContext = mockContext,
            recordedDataQueueHandler = mockRecordedDataQueueHandler,
            wrappedCallback = mockWrappedCallback,
            timeProvider = mockTimeProvider,
            viewOnDrawInterceptor = mockViewOnDrawInterceptor,
            internalLogger = mockInternalLogger,
            imagePrivacy = ImagePrivacy.MASK_NONE,
            touchPrivacyManager = mockTouchPrivacyManager,
            privacy = fakeTextAndInputPrivacy,
            copyEvent = { it },
            motionEventUtils = mockEventUtils,
            motionUpdateThresholdInNs = TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS,
            flushPositionBufferThresholdInNs = TEST_FLUSH_BUFFER_THRESHOLD_NS,
            windowInspector = mockWindowInspector
        )
    }

    // region Touch Events

    @Test
    fun `M delegate to the wrappedCallback W onTouchEvent`(forge: Forge) {
        // Given
        val fakeReturnedValue = forge.aBool()
        val mockEvent: MotionEvent = mock()
        whenever(mockWrappedCallback.dispatchTouchEvent(mockEvent)).thenReturn(fakeReturnedValue)

        // When
        val eventConsumed = testedWindowCallback.dispatchTouchEvent(mockEvent)

        // Then
        verify(mockWrappedCallback).dispatchTouchEvent(mockEvent)
        assertThat(eventConsumed).isEqualTo(fakeReturnedValue)
    }

    @Test
    fun `M recycle the copy after using it W onTouchEvent`(forge: Forge) {
        // Given
        val fakeReturnedValue = forge.aBool()
        val mockEvent: MotionEvent = mock()
        whenever(mockWrappedCallback.dispatchTouchEvent(mockEvent)).thenReturn(fakeReturnedValue)

        // When
        testedWindowCallback.dispatchTouchEvent(mockEvent)

        // Then
        verify(mockWrappedCallback).dispatchTouchEvent(mockEvent)
        verify(mockEvent).recycle()
    }

    @Test
    fun `M log the exception W wrappedCallback throws { null parameter }`() {
        // Given
        val mockEvent: MotionEvent = mock()
        whenever(mockWrappedCallback.dispatchTouchEvent(mockEvent)).thenAnswer {
            Intrinsics.checkNotNullParameter(
                null,
                "event"
            )
        }

        // When
        testedWindowCallback.dispatchTouchEvent(mockEvent)

        // Then
        verify(mockWrappedCallback).dispatchTouchEvent(mockEvent)
        mockInternalLogger.verifyLog(
            InternalLogger.Level.ERROR,
            InternalLogger.Target.MAINTAINER,
            RecorderWindowCallback.FAIL_TO_PROCESS_MOTION_EVENT_ERROR_MESSAGE,
            throwableClass = NullPointerException::class.java
        )
    }

    @Test
    fun `M propagate the exception W wrappedCallback throws`(@Forgery fakeException: Exception) {
        // Given
        val mockEvent: MotionEvent = mock()
        whenever(mockWrappedCallback.dispatchTouchEvent(mockEvent)).thenThrow(fakeException)

        // When + Then
        val exceptionCaught =
            assertThrows<Exception> { testedWindowCallback.dispatchTouchEvent(mockEvent) }
        assertThat(exceptionCaught).isSameAs(fakeException)
    }

    @Test
    fun `M do nothing W onTouchEvent { event is null }`() {
        // When
        testedWindowCallback.dispatchTouchEvent(null)

        // Then
        assertThat(testedWindowCallback.pointerInteractions).isEmpty()
        mockInternalLogger.verifyLog(
            InternalLogger.Level.ERROR,
            InternalLogger.Target.USER,
            RecorderWindowCallback.MOTION_EVENT_WAS_NULL_ERROR_MESSAGE
        )
    }

    @Test
    fun `M update the positions W onTouchEvent() { ActionDown }`(forge: Forge) {
        // Given
        val fakeRecords = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val relatedMotionEvent = fakeRecords.asMotionEvent()

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent)

        // Then
        assertThat(testedWindowCallback.pointerInteractions).isEqualTo(fakeRecords)
    }

    @Test
    fun `M update the positions and flush them W onTouchEvent() { ActionUp }`(forge: Forge) {
        // Given
        val fakeDownEvent = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val downMotionEvent = fakeDownEvent.asMotionEvent()
        testedWindowCallback.dispatchTouchEvent(downMotionEvent)

        val fakeRecords = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val relatedMotionEvent = fakeRecords.asMotionEvent()

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent)

        // Then
        assertThat(testedWindowCallback.pointerInteractions).isEmpty()
        verify(mockRecordedDataQueueHandler).addTouchEventItem(fakeDownEvent + fakeRecords)
        verify(mockRecordedDataQueueHandler).tryToConsumeItems()
    }

    @Test
    fun `M debounce the positions update W onTouchEvent() {one gesture cycle}`(forge: Forge) {
        // Given
        val fakeEvent1Records = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val relatedMotionEvent1 = fakeEvent1Records.asMotionEvent()
        val fakeEvent2Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent2 = fakeEvent2Records.asMotionEvent()
        val fakeEvent3Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent3 = fakeEvent3Records.asMotionEvent()
        val fakeEvent4Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent4 = fakeEvent4Records.asMotionEvent()
        val fakeEvent5Records = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val relatedMotionEvent5 = fakeEvent5Records.asMotionEvent()
        val expectedRecords = fakeEvent1Records +
            fakeEvent2Records +
            fakeEvent4Records +
            fakeEvent5Records

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent1)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent2)
        // must skip 3 as the motion update delay was not reached
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent3)
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS))
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent4)

        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent5)

        // Then
        verify(mockRecordedDataQueueHandler).addTouchEventItem(expectedRecords)
        verify(mockRecordedDataQueueHandler).tryToConsumeItems()
    }

    @Test
    fun `M perform intermediary flush W onTouchEvent() {one long gesture cycle}`(forge: Forge) {
        // Given
        val fakeDownEventRecords = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val fakeDownEvent = fakeDownEventRecords.asMotionEvent()
        val fakeMoveRecordsBeforeFlush = forge.aList(size = forge.anInt(min = 1, max = 5)) {
            touchRecords(MobileSegment.PointerEventType.MOVE)
        }
        val fakeMoveEventsBeforeFlush = fakeMoveRecordsBeforeFlush.map { it.asMotionEvent() }
        val fakeMoveRecordsAfterFlush = forge.aList(size = forge.anInt(min = 1, max = 5)) {
            touchRecords(MobileSegment.PointerEventType.MOVE)
        }
        val fakeMoveEventsAfterFlush = fakeMoveRecordsAfterFlush.map { it.asMotionEvent() }
        val fakeUpEventRecords = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val fakeUpEvent = fakeUpEventRecords.asMotionEvent()
        val expectedRecords1 = fakeDownEventRecords + fakeMoveRecordsBeforeFlush.flatten()
        val expectedRecords2 = fakeMoveRecordsAfterFlush.flatten() + fakeUpEventRecords

        // When
        testedWindowCallback.dispatchTouchEvent(fakeDownEvent)

        fakeMoveEventsBeforeFlush.forEachIndexed { index, event ->
            if (index == fakeMoveEventsBeforeFlush.size - 1) {
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_FLUSH_BUFFER_THRESHOLD_NS))
            } else {
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS))
            }
            testedWindowCallback.dispatchTouchEvent(event)
        }
        fakeMoveEventsAfterFlush.forEach {
            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS))
            testedWindowCallback.dispatchTouchEvent(it)
        }
        testedWindowCallback.dispatchTouchEvent(fakeUpEvent)

        // Then
        val argumentCaptor = argumentCaptor<List<MobileRecord>>()
        verify(mockRecordedDataQueueHandler, times(2)).addTouchEventItem(argumentCaptor.capture())
        verify(mockRecordedDataQueueHandler, times(2)).tryToConsumeItems()
        assertThat(argumentCaptor.firstValue).isEqualTo(expectedRecords1)
        assertThat(argumentCaptor.lastValue).isEqualTo(expectedRecords2)
    }

    @Test
    fun `M always collect the first move event after down W onTouchEvent()`(forge: Forge) {
        // Given
        val fakeGesture1DownRecords = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val fakeGesture1DownEvent = fakeGesture1DownRecords.asMotionEvent()
        val fakeGesture1MoveRecords = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val fakeGesture1MoveEvent = fakeGesture1MoveRecords.asMotionEvent()
        val fakeGesture1UpRecords = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val fakeGesture1UpEvent = fakeGesture1UpRecords.asMotionEvent()

        val fakeGesture2DownRecords = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val fakeGesture2DownEvent = fakeGesture2DownRecords.asMotionEvent()
        val fakeGesture2MoveRecords = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val fakeGesture2MoveEvent = fakeGesture2MoveRecords.asMotionEvent()
        val fakeGesture2UpRecords = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val fakeGesture2UpEvent = fakeGesture2UpRecords.asMotionEvent()
        val expectedTouchRecords1 =
            fakeGesture1DownRecords +
                fakeGesture1MoveRecords +
                fakeGesture1UpRecords
        val expectedTouchRecords2 =
            fakeGesture2DownRecords +
                fakeGesture2MoveRecords +
                fakeGesture2UpRecords

        // When
        testedWindowCallback.dispatchTouchEvent(fakeGesture1DownEvent)
        testedWindowCallback.dispatchTouchEvent(fakeGesture1MoveEvent)
        testedWindowCallback.dispatchTouchEvent(fakeGesture1UpEvent)
        testedWindowCallback.dispatchTouchEvent(fakeGesture2DownEvent)
        testedWindowCallback.dispatchTouchEvent(fakeGesture2MoveEvent)
        testedWindowCallback.dispatchTouchEvent(fakeGesture2UpEvent)

        // Then
        val argumentCaptor = argumentCaptor<List<MobileRecord>>()
        verify(mockRecordedDataQueueHandler, times(2)).addTouchEventItem(argumentCaptor.capture())
        verify(mockRecordedDataQueueHandler, times(2)).tryToConsumeItems()
        assertThat(argumentCaptor.firstValue).isEqualTo(expectedTouchRecords1)
        assertThat(argumentCaptor.lastValue).isEqualTo(expectedTouchRecords2)
    }

    @Test
    fun `M flush the intermediary positions W onTouchEvent() {move event longer than threshold}`(
        forge: Forge
    ) {
        // Given
        val fakeDownRecords = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val fakeDownEvent = fakeDownRecords.asMotionEvent()
        val fakeEvent1MoveRecords = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val fakeMotion1Event = fakeEvent1MoveRecords.asMotionEvent()
        val fakeEvent2MoveRecords = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val fakeMotion2Event = fakeEvent2MoveRecords.asMotionEvent()
        val expectedTouchRecords1 = fakeDownRecords + fakeEvent1MoveRecords

        // When
        testedWindowCallback.dispatchTouchEvent(fakeDownEvent)
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_FLUSH_BUFFER_THRESHOLD_NS))
        testedWindowCallback.dispatchTouchEvent(fakeMotion1Event)
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TEST_FLUSH_BUFFER_THRESHOLD_NS))
        testedWindowCallback.dispatchTouchEvent(fakeMotion2Event)

        // Then
        val argumentCaptor = argumentCaptor<List<MobileRecord>>()
        verify(mockRecordedDataQueueHandler, times(2)).addTouchEventItem(argumentCaptor.capture())
        verify(mockRecordedDataQueueHandler, times(2)).tryToConsumeItems()
        assertThat(argumentCaptor.firstValue).isEqualTo(expectedTouchRecords1)
        assertThat(argumentCaptor.lastValue).isEqualTo(fakeEvent2MoveRecords)
    }

    @Test
    fun `M flush the positions W onTouchEvent() { full gesture lifecycle }`(forge: Forge) {
        // Given
        val fakeEvent1Records = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val relatedMotionEvent1 = fakeEvent1Records.asMotionEvent()
        val fakeEvent2Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent2 = fakeEvent2Records.asMotionEvent()
        val fakeEvent3Records = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val relatedMotionEvent3 = fakeEvent3Records.asMotionEvent()

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent1)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent2)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent3)

        // Then
        val expectedRecords = fakeEvent1Records + fakeEvent2Records + fakeEvent3Records
        verify(mockRecordedDataQueueHandler).addTouchEventItem(expectedRecords)
        verify(mockRecordedDataQueueHandler).tryToConsumeItems()
        assertThat(testedWindowCallback.pointerInteractions).isEmpty()
    }

    // endregion

    // region Window focus change

    @Test
    fun `M intercept the onDraw for the new decorViews W window focus changed`(forge: Forge) {
        // Given
        val fakeDecorViews: List<View> = forge.aList { mock() }
        whenever(mockWindowInspector.getGlobalWindowViews(mockInternalLogger))
            .thenReturn(fakeDecorViews)

        // When
        testedWindowCallback.onWindowFocusChanged(forge.aBool())

        // Then
        inOrder(mockViewOnDrawInterceptor) {
            verify(mockViewOnDrawInterceptor).stopIntercepting()
            verify(mockViewOnDrawInterceptor).intercept(fakeDecorViews, fakeTextAndInputPrivacy, ImagePrivacy.MASK_NONE)
        }
    }

    @Test
    fun `M do nothing W window focus changed {decorViews could not be fetched}`(forge: Forge) {
        // Given
        whenever(mockWindowInspector.getGlobalWindowViews(mockInternalLogger))
            .thenReturn(emptyList())

        // When
        testedWindowCallback.onWindowFocusChanged(forge.aBool())

        // Then
        verifyNoInteractions(mockViewOnDrawInterceptor)
    }

    // endregion

    // region touchPrivacy

    @Test
    fun `M capture touch events W onTouchEvent { TouchPrivacy SHOW }`(
        forge: Forge
    ) {
        // Given
        val fakeEvent1Records = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val relatedMotionEvent1 = fakeEvent1Records.asMotionEvent()
        val fakeEvent2Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent2 = fakeEvent2Records.asMotionEvent()
        val fakeEvent3Records = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val relatedMotionEvent3 = fakeEvent3Records.asMotionEvent()

        testedWindowCallback = RecorderWindowCallback(
            appContext = mockContext,
            recordedDataQueueHandler = mockRecordedDataQueueHandler,
            wrappedCallback = mockWrappedCallback,
            timeProvider = mockTimeProvider,
            viewOnDrawInterceptor = mockViewOnDrawInterceptor,
            internalLogger = mockInternalLogger,
            privacy = fakeTextAndInputPrivacy,
            imagePrivacy = ImagePrivacy.MASK_NONE,
            touchPrivacyManager = mockTouchPrivacyManager,
            copyEvent = { it },
            motionEventUtils = mockEventUtils,
            motionUpdateThresholdInNs = TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS,
            flushPositionBufferThresholdInNs = TEST_FLUSH_BUFFER_THRESHOLD_NS,
            windowInspector = mockWindowInspector
        )

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent1)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent2)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent3)

        // Then
        val expectedRecords = fakeEvent1Records + fakeEvent2Records + fakeEvent3Records
        verify(mockRecordedDataQueueHandler).addTouchEventItem(expectedRecords)
        verify(mockRecordedDataQueueHandler).tryToConsumeItems()
        assertThat(testedWindowCallback.pointerInteractions).isEmpty()
    }

    @Test
    fun `M not capture touch events W onTouchEvent { TouchPrivacy HIDE }`(
        forge: Forge
    ) {
        // Given
        val fakeEvent1Records = forge.touchRecords(MobileSegment.PointerEventType.DOWN)
        val relatedMotionEvent1 = fakeEvent1Records.asMotionEvent()
        val fakeEvent2Records = forge.touchRecords(MobileSegment.PointerEventType.MOVE)
        val relatedMotionEvent2 = fakeEvent2Records.asMotionEvent()
        val fakeEvent3Records = forge.touchRecords(MobileSegment.PointerEventType.UP)
        val relatedMotionEvent3 = fakeEvent3Records.asMotionEvent()
        whenever(mockTouchPrivacyManager.shouldRecordTouch(any()))
            .thenReturn(false)

        testedWindowCallback = RecorderWindowCallback(
            appContext = mockContext,
            recordedDataQueueHandler = mockRecordedDataQueueHandler,
            wrappedCallback = mockWrappedCallback,
            timeProvider = mockTimeProvider,
            viewOnDrawInterceptor = mockViewOnDrawInterceptor,
            internalLogger = mockInternalLogger,
            privacy = fakeTextAndInputPrivacy,
            imagePrivacy = ImagePrivacy.MASK_NONE,
            touchPrivacyManager = mockTouchPrivacyManager,
            copyEvent = { it },
            motionEventUtils = mockEventUtils,
            motionUpdateThresholdInNs = TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS,
            flushPositionBufferThresholdInNs = TEST_FLUSH_BUFFER_THRESHOLD_NS,
            windowInspector = mockWindowInspector
        )

        // When
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent1)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent2)
        testedWindowCallback.dispatchTouchEvent(relatedMotionEvent3)

        // Then
        verifyNoInteractions(mockRecordedDataQueueHandler)
        assertThat(testedWindowCallback.pointerInteractions).isEmpty()
    }

    // endregion

    // region Internal

    private fun Forge.touchRecords(
        eventType: MobileSegment.PointerEventType
    ): List<MobileRecord.MobileIncrementalSnapshotRecord> {
        val pointerIds = aList { anInt(min = 1) }
        val positionMaxValue = (FLOAT_MAX_INT_VALUE / fakeDensity).toLong()
        return pointerIds
            .map {
                MobileIncrementalData.PointerInteractionData(
                    eventType,
                    MobileSegment.PointerType.TOUCH,
                    it.toLong(),
                    aLong(min = 0, max = positionMaxValue),
                    aLong(min = 0, max = positionMaxValue)
                )
            }
            .map {
                MobileRecord.MobileIncrementalSnapshotRecord(
                    timestamp = fakeTimestamp,
                    data = it
                )
            }
    }

    private fun List<MobileRecord.MobileIncrementalSnapshotRecord>.asMotionEvent(): MotionEvent {
        val mockMotionEvent: MotionEvent = mock { event ->
            whenever(event.pointerCount).thenReturn(this@asMotionEvent.size)
        }
        this.forEachIndexed { index, record ->
            val pointerInteractionData =
                record.data as MobileIncrementalData.PointerInteractionData
            val pointerId = pointerInteractionData.pointerId.toInt()
            whenever(mockMotionEvent.getPointerId(index)).thenReturn(pointerId)
            val motionEventAction = pointerInteractionData.pointerEventType.asMotionEventAction()
            whenever(mockMotionEvent.action).thenReturn(motionEventAction)
            val expectedXPos = (pointerInteractionData.x.toInt() * fakeDensity).toFloat()
            val expectedYPos = (pointerInteractionData.y.toInt() * fakeDensity).toFloat()
            whenever(mockEventUtils.getPointerAbsoluteX(mockMotionEvent, index))
                .thenReturn(expectedXPos)
            whenever(mockEventUtils.getPointerAbsoluteY(mockMotionEvent, index))
                .thenReturn(expectedYPos)
        }

        return mockMotionEvent
    }

    private fun MobileSegment.PointerEventType.asMotionEventAction(): Int {
        return when (this) {
            MobileSegment.PointerEventType.DOWN -> MotionEvent.ACTION_DOWN
            MobileSegment.PointerEventType.UP -> MotionEvent.ACTION_UP
            MobileSegment.PointerEventType.MOVE -> MotionEvent.ACTION_MOVE
        }
    }

    // endregion

    companion object {
        private val FLOAT_MAX_INT_VALUE = 2.0.pow(23.0).toFloat()

        // We need to test with higher threshold values in order to avoid flakiness
        private val TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS: Long =
            TimeUnit.MILLISECONDS.toNanos(100)

        private val TEST_FLUSH_BUFFER_THRESHOLD_NS: Long =
            TEST_MOTION_UPDATE_DELAY_THRESHOLD_NS * 10
    }
}
