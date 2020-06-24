/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.domain.scope

import com.datadog.android.core.internal.data.Writer
import com.datadog.android.core.internal.utils.loggableStackTrace
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.internal.RumFeature
import com.datadog.android.rum.internal.domain.RumContext
import com.datadog.android.rum.internal.domain.event.RumEvent
import com.datadog.android.rum.internal.domain.model.ErrorEvent
import com.datadog.android.rum.internal.domain.model.ViewEvent
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.UUID

internal class RumViewScope(
    internal val parentScope: RumScope,
    key: Any,
    internal val name: String,
    initialAttributes: Map<String, Any?>
) : RumScope {

    internal val urlName = name.replace('.', '/')

    internal val keyRef: Reference<Any> = WeakReference(key)
    internal val attributes: MutableMap<String, Any?> = initialAttributes.toMutableMap()

    internal val viewId: String = UUID.randomUUID().toString()
    internal val startedNanos: Long = System.nanoTime()

    internal val eventTimestamp = RumFeature.timeProvider.getDeviceTimestamp()

    internal var activeActionScope: RumScope? = null
    internal val activeResourceScopes = mutableMapOf<String, RumScope>()

    internal var resourceCount: Long = 0
    internal var actionCount: Long = 0
    internal var errorCount: Long = 0
    internal var version: Long = 1

    internal var stopped: Boolean = false

    init {
        GlobalRum.updateRumContext(getRumContext())
        attributes.putAll(GlobalRum.globalAttributes)
    }

    // region RumScope

    override fun handleEvent(
        event: RumRawEvent,
        writer: Writer<RumEvent>
    ): RumScope? {

        when (event) {
            is RumRawEvent.SentError -> {
                errorCount++
                sendViewUpdate(writer)
            }
            is RumRawEvent.SentResource -> {
                resourceCount++
                sendViewUpdate(writer)
            }
            is RumRawEvent.SentAction -> {
                actionCount++
                sendViewUpdate(writer)
            }
            is RumRawEvent.StartView -> onStartView(event, writer)
            is RumRawEvent.StopView -> onStopView(event, writer)
            is RumRawEvent.StartAction -> onStartAction(event, writer)
            is RumRawEvent.StartResource -> onStartResource(event, writer)
            is RumRawEvent.AddError -> onAddError(event, writer)
            is RumRawEvent.KeepAlive -> onKeepAlive(event, writer)
            else -> delegateEventToChildren(event, writer)
        }

        return if (stopped && activeResourceScopes.isEmpty()) {
            null
        } else {
            this
        }
    }

    override fun getRumContext(): RumContext {
        return parentScope.getRumContext()
            .copy(
                viewId = viewId,
                viewUrl = urlName,
                actionId = (activeActionScope as? RumActionScope)?.actionId
            )
    }

    // endregion

    // region Internal

    private fun onStartView(
        event: RumRawEvent.StartView,
        writer: Writer<RumEvent>
    ) {
        if (!stopped) {
            sendViewUpdate(writer)
            delegateEventToChildren(event, writer)
            stopped = true
        }
    }

    private fun onStopView(
        event: RumRawEvent.StopView,
        writer: Writer<RumEvent>
    ) {
        delegateEventToChildren(event, writer)
        val startedKey = keyRef.get()
        val shouldStop = (event.key == startedKey) || (startedKey == null)
        if (shouldStop && !stopped) {
            attributes.putAll(event.attributes)
            sendViewUpdate(writer)
            stopped = true
        }
    }

    private fun onStartAction(
        event: RumRawEvent.StartAction,
        writer: Writer<RumEvent>
    ) {
        delegateEventToChildren(event, writer)

        if (stopped || activeActionScope != null) return

        activeActionScope = RumActionScope.fromEvent(this, event)
    }

    private fun onStartResource(
        event: RumRawEvent.StartResource,
        writer: Writer<RumEvent>
    ) {
        delegateEventToChildren(event, writer)
        if (stopped) return

        val updatedEvent = event.copy(
            attributes = addExtraAttributes(event.attributes)
        )
        activeResourceScopes[event.key] = RumResourceScope.fromEvent(this, updatedEvent)
    }

    private fun onAddError(
        event: RumRawEvent.AddError,
        writer: Writer<RumEvent>
    ) {
        delegateEventToChildren(event, writer)
        if (stopped) return

        val context = getRumContext()
        val updatedAttributes = addExtraAttributes(event.attributes)

        val errorEvent = ErrorEvent(
            date = eventTimestamp,
            error = ErrorEvent.Error(
                message = event.message,
                source = event.source.toSchemaSource(),
                stack = event.throwable?.loggableStackTrace()
            ),
            action = context.actionId?.let { ErrorEvent.Action(it) },
            view = ErrorEvent.View(
                id = context.viewId.orEmpty(),
                url = context.viewUrl.orEmpty()
            ),
            application = ErrorEvent.Application(context.applicationId),
            session = ErrorEvent.Session(id = context.sessionId, type = ErrorEvent.Type.USER),
            dd = ErrorEvent.Dd()
        )
        val rumEvent = RumEvent(
            event = errorEvent,
            attributes = updatedAttributes,
            userInfo = RumFeature.userInfoProvider.getUserInfo(),
            networkInfo = RumFeature.networkInfoProvider.getLatestNetworkInfo()
        )
        writer.write(rumEvent)
        errorCount++
        sendViewUpdate(writer)
    }

    private fun onKeepAlive(
        event: RumRawEvent.KeepAlive,
        writer: Writer<RumEvent>
    ) {
        delegateEventToChildren(event, writer)
        if (stopped) return

        sendViewUpdate(writer)
    }

    private fun delegateEventToChildren(
        event: RumRawEvent,
        writer: Writer<RumEvent>
    ) {
        delegateEventToResources(event, writer)
        delegateEventToAction(event, writer)
    }

    private fun delegateEventToAction(
        event: RumRawEvent,
        writer: Writer<RumEvent>
    ) {
        val currentAction = activeActionScope
        if (currentAction != null) {
            val updatedAction = currentAction.handleEvent(event, writer)
            if (updatedAction == null) {
                activeActionScope = null
            }
        }
    }

    private fun delegateEventToResources(
        event: RumRawEvent,
        writer: Writer<RumEvent>
    ) {
        val iterator = activeResourceScopes.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val scope = entry.value.handleEvent(event, writer)
            if (scope == null) {
                iterator.remove()
            }
        }
    }

    private fun sendViewUpdate(writer: Writer<RumEvent>) {
        attributes.putAll(GlobalRum.globalAttributes)
        version++
        val updatedDurationNs = System.nanoTime() - startedNanos
        val context = getRumContext()

        val viewEvent = ViewEvent(
            date = RumFeature.timeProvider.getDeviceTimestamp(),
            view = ViewEvent.View(
                id = context.viewId.orEmpty(),
                url = context.viewUrl.orEmpty(),
                timeSpent = updatedDurationNs,
                action = ViewEvent.Action(actionCount),
                resource = ViewEvent.Resource(resourceCount),
                error = ViewEvent.Error(errorCount)
            ),
            application = ViewEvent.Application(context.applicationId),
            session = ViewEvent.Session(id = context.sessionId, type = ViewEvent.Type.USER),
            dd = ViewEvent.Dd(documentVersion = version)
        )

        val rumEvent = RumEvent(
            event = viewEvent,
            attributes = attributes,
            userInfo = RumFeature.userInfoProvider.getUserInfo()
        )
        writer.write(rumEvent)
    }

    private fun addExtraAttributes(
        attributes: Map<String, Any?>
    ): MutableMap<String, Any?> {
        return attributes.toMutableMap()
            .apply { putAll(GlobalRum.globalAttributes) }
    }

    // endregion

    companion object {

        internal fun fromEvent(
            parentScope: RumScope,
            event: RumRawEvent.StartView
        ): RumViewScope {
            return RumViewScope(parentScope, event.key, event.name, event.attributes)
        }
    }
}