/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.legacy.trace.api.interceptor;

import com.datadog.legacy.trace.api.DDTags;

import java.util.Map;

public interface MutableSpan {

  /** @return Start time with nanosecond scale, but millisecond resolution. */
  long getStartTime();

  /** @return Duration with nanosecond scale. */
  long getDurationNano();

  String getOperationName();

  MutableSpan setOperationName(final String serviceName);

  String getServiceName();

  MutableSpan setServiceName(final String serviceName);

  String getResourceName();

  MutableSpan setResourceName(final String resourceName);

  Integer getSamplingPriority();

  /**
   * @deprecated Use {@link io.opentracing.Span#setTag(String, boolean)} instead using either tag
   *     names {@link com.datadog.legacy.trace.api.DDTags#MANUAL_KEEP} or {@link
   *     DDTags#MANUAL_DROP}.
   * @param newPriority
   * @return
   */
  @Deprecated
  MutableSpan setSamplingPriority(final int newPriority);

  String getSpanType();

  MutableSpan setSpanType(final String type);

  Map<String, Object> getTags();

  MutableSpan setTag(final String tag, final String value);

  MutableSpan setTag(final String tag, final boolean value);

  MutableSpan setTag(final String tag, final Number value);

  Boolean isError();

  MutableSpan setError(boolean value);

  /** @deprecated Use {@link #getLocalRootSpan()} instead. */
  @Deprecated
  MutableSpan getRootSpan();

  /**
   * Returns the root span for current the trace fragment. In the context of distributed tracing
   * this method returns the root span only for the fragment generated by the currently traced
   * application.
   *
   * @return The root span for the current trace fragment.
   */
  MutableSpan getLocalRootSpan();

  /**
   * By calling this method the span will be removed from the current active Trace without
   * actually being persisted.
   *
   * Note: This method is meant for internal SDK usage. Be aware that if used this Span will
   * be removed from the Trace and lost.
   */
  public void drop();
}