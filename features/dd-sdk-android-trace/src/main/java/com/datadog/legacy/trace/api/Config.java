/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.legacy.trace.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Config reads values with the following priority: 1) system properties, 2) environment variables,
 * 3) optional configuration file. It also includes default values to ensure a valid config.
 *
 * <p>
 *
 * <p>System properties are {@link Config#PREFIX}'ed. Environment variables are the same as the
 * system property, but uppercased with '.' -> '_'.
 */
public class Config {
    /**
     * Config keys below
     */
    private static final String PREFIX = "dd.";

    public static final String PROFILING_URL_TEMPLATE = "https://intake.profile.%s/v1/input";

    private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");

    public static final String CONFIGURATION_FILE = "trace.config";
    public static final String API_KEY = "api-key";
    public static final String API_KEY_FILE = "api-key-file";
    public static final String SITE = "site";
    public static final String SERVICE_NAME = "service.name";
    public static final String TRACE_ENABLED = "trace.enabled";
    public static final String INTEGRATIONS_ENABLED = "integrations.enabled";
    public static final String WRITER_TYPE = "writer.type";
    public static final String AGENT_HOST = "agent.host";
    public static final String TRACE_AGENT_PORT = "trace.agent.port";
    public static final String AGENT_PORT_LEGACY = "agent.port";
    public static final String AGENT_UNIX_DOMAIN_SOCKET = "trace.agent.unix.domain.socket";
    public static final String PRIORITY_SAMPLING = "priority.sampling";
    public static final String TRACE_RESOLVER_ENABLED = "trace.resolver.enabled";
    public static final String SERVICE_MAPPING = "service.mapping";

    private static final String ENV = "env";
    private static final String VERSION = "version";
    public static final String TAGS = "tags";
    @Deprecated // Use dd.tags instead
    public static final String GLOBAL_TAGS = "trace.global.tags";
    public static final String SPAN_TAGS = "trace.span.tags";
    public static final String JMX_TAGS = "trace.jmx.tags";
    public static final String TRACE_ANALYTICS_ENABLED = "trace.analytics.enabled";
    public static final String TRACE_ANNOTATIONS = "trace.annotations";
    public static final String TRACE_EXECUTORS_ALL = "trace.executors.all";
    public static final String TRACE_EXECUTORS = "trace.executors";
    public static final String TRACE_METHODS = "trace.methods";
    public static final String TRACE_CLASSES_EXCLUDE = "trace.classes.exclude";
    public static final String TRACE_SAMPLING_SERVICE_RULES = "trace.sampling.service.rules";
    public static final String TRACE_SAMPLING_OPERATION_RULES = "trace.sampling.operation.rules";
    public static final String TRACE_SAMPLE_RATE = "trace.sample.rate";
    public static final String TRACE_RATE_LIMIT = "trace.rate.limit";
    public static final String TRACE_REPORT_HOSTNAME = "trace.report-hostname";
    public static final String HEADER_TAGS = "trace.header.tags";
    public static final String HTTP_SERVER_ERROR_STATUSES = "http.server.error.statuses";
    public static final String HTTP_CLIENT_ERROR_STATUSES = "http.client.error.statuses";
    public static final String HTTP_SERVER_TAG_QUERY_STRING = "http.server.tag.query-string";
    public static final String HTTP_CLIENT_TAG_QUERY_STRING = "http.client.tag.query-string";
    public static final String HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN = "trace.http.client.split-by-domain";
    public static final String DB_CLIENT_HOST_SPLIT_BY_INSTANCE = "trace.db.client.split-by-instance";
    public static final String SPLIT_BY_TAGS = "trace.split-by-tags";
    public static final String SCOPE_DEPTH_LIMIT = "trace.scope.depth.limit";
    public static final String PARTIAL_FLUSH_MIN_SPANS = "trace.partial.flush.min.spans";
    public static final String RUNTIME_CONTEXT_FIELD_INJECTION =
            "trace.runtime.context.field.injection";
    public static final String PROPAGATION_STYLE_EXTRACT = "propagation.style.extract";
    public static final String PROPAGATION_STYLE_INJECT = "propagation.style.inject";

    public static final String JMX_FETCH_ENABLED = "jmxfetch.enabled";
    public static final String JMX_FETCH_CONFIG_DIR = "jmxfetch.config.dir";
    public static final String JMX_FETCH_CONFIG = "jmxfetch.config";
    @Deprecated
    public static final String JMX_FETCH_METRICS_CONFIGS = "jmxfetch.metrics-configs";
    public static final String JMX_FETCH_CHECK_PERIOD = "jmxfetch.check-period";
    public static final String JMX_FETCH_REFRESH_BEANS_PERIOD = "jmxfetch.refresh-beans-period";
    public static final String JMX_FETCH_STATSD_HOST = "jmxfetch.statsd.host";
    public static final String JMX_FETCH_STATSD_PORT = "jmxfetch.statsd.port";

    public static final String HEALTH_METRICS_ENABLED = "trace.health.metrics.enabled";
    public static final String HEALTH_METRICS_STATSD_HOST = "trace.health.metrics.statsd.host";
    public static final String HEALTH_METRICS_STATSD_PORT = "trace.health.metrics.statsd.port";

    public static final String LOGS_INJECTION_ENABLED = "logs.injection";

    public static final String PROFILING_ENABLED = "profiling.enabled";
    @Deprecated // Use dd.site instead
    public static final String PROFILING_URL = "profiling.url";
    @Deprecated // Use dd.api-key instead
    public static final String PROFILING_API_KEY_OLD = "profiling.api-key";
    @Deprecated // Use dd.api-key-file instead
    public static final String PROFILING_API_KEY_FILE_OLD = "profiling.api-key-file";
    @Deprecated // Use dd.api-key instead
    public static final String PROFILING_API_KEY_VERY_OLD = "profiling.apikey";
    @Deprecated // Use dd.api-key-file instead
    public static final String PROFILING_API_KEY_FILE_VERY_OLD = "profiling.apikey.file";
    public static final String PROFILING_TAGS = "profiling.tags";
    public static final String PROFILING_START_DELAY = "profiling.start-delay";
    // DANGEROUS! May lead on sigsegv on JVMs before 14
    // Not intended for production use
    public static final String PROFILING_START_FORCE_FIRST =
            "profiling.experimental.start-force-first";
    public static final String PROFILING_UPLOAD_PERIOD = "profiling.upload.period";
    public static final String PROFILING_TEMPLATE_OVERRIDE_FILE =
            "profiling.jfr-template-override-file";
    public static final String PROFILING_UPLOAD_TIMEOUT = "profiling.upload.timeout";
    public static final String PROFILING_UPLOAD_COMPRESSION = "profiling.upload.compression";
    public static final String PROFILING_PROXY_HOST = "profiling.proxy.host";
    public static final String PROFILING_PROXY_PORT = "profiling.proxy.port";
    public static final String PROFILING_PROXY_USERNAME = "profiling.proxy.username";
    public static final String PROFILING_PROXY_PASSWORD = "profiling.proxy.password";
    public static final String PROFILING_EXCEPTION_SAMPLE_LIMIT = "profiling.exception.sample.limit";
    public static final String PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS =
            "profiling.exception.histogram.top-items";
    public static final String PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE =
            "profiling.exception.histogram.max-collection-size";

    public static final String RUNTIME_ID_TAG = "runtime-id";
    public static final String SERVICE = "service";
    public static final String SERVICE_TAG = SERVICE;
    public static final String HOST_TAG = "host";
    public static final String LANGUAGE_TAG_KEY = "language";
    public static final String LANGUAGE_TAG_VALUE = "jvm";

    public static final String DEFAULT_SITE = "datadoghq.com";
    public static final String DEFAULT_SERVICE_NAME = "unnamed-java-app";

    private static final boolean DEFAULT_TRACE_ENABLED = true;
    public static final boolean DEFAULT_INTEGRATIONS_ENABLED = true;
    public static final String DD_AGENT_WRITER_TYPE = "DDAgentWriter";
    public static final String LOGGING_WRITER_TYPE = "LoggingWriter";
    private static final String DEFAULT_AGENT_WRITER_TYPE = DD_AGENT_WRITER_TYPE;

    public static final String DEFAULT_AGENT_HOST = "localhost";
    public static final int DEFAULT_TRACE_AGENT_PORT = 8126;
    public static final String DEFAULT_AGENT_UNIX_DOMAIN_SOCKET = null;

    private static final boolean DEFAULT_RUNTIME_CONTEXT_FIELD_INJECTION = true;

    private static final boolean DEFAULT_PRIORITY_SAMPLING_ENABLED = true;
    private static final boolean DEFAULT_TRACE_RESOLVER_ENABLED = true;
    private static final Set<Integer> DEFAULT_HTTP_SERVER_ERROR_STATUSES =
            parseIntegerRangeSet("500-599", "default");
    private static final Set<Integer> DEFAULT_HTTP_CLIENT_ERROR_STATUSES =
            parseIntegerRangeSet("400-499", "default");
    private static final boolean DEFAULT_HTTP_SERVER_TAG_QUERY_STRING = false;
    private static final boolean DEFAULT_HTTP_CLIENT_TAG_QUERY_STRING = false;
    private static final boolean DEFAULT_HTTP_CLIENT_SPLIT_BY_DOMAIN = false;
    private static final boolean DEFAULT_DB_CLIENT_HOST_SPLIT_BY_INSTANCE = false;
    private static final String DEFAULT_SPLIT_BY_TAGS = "";
    private static final int DEFAULT_SCOPE_DEPTH_LIMIT = 100;
    private static final int DEFAULT_PARTIAL_FLUSH_MIN_SPANS = 1000;
    private static final String DEFAULT_PROPAGATION_STYLE_EXTRACT = PropagationStyle.DATADOG.name();
    private static final String DEFAULT_PROPAGATION_STYLE_INJECT = PropagationStyle.DATADOG.name();
    private static final boolean DEFAULT_JMX_FETCH_ENABLED = true;

    public static final int DEFAULT_JMX_FETCH_STATSD_PORT = 8125;

    public static final boolean DEFAULT_METRICS_ENABLED = false;
    // No default constants for metrics statsd support -- falls back to jmxfetch values

    public static final boolean DEFAULT_LOGS_INJECTION_ENABLED = false;

    public static final boolean DEFAULT_PROFILING_ENABLED = false;
    public static final int DEFAULT_PROFILING_START_DELAY = 10;
    public static final boolean DEFAULT_PROFILING_START_FORCE_FIRST = false;
    public static final int DEFAULT_PROFILING_UPLOAD_PERIOD = 60; // 1 min
    public static final int DEFAULT_PROFILING_UPLOAD_TIMEOUT = 30; // seconds
    public static final String DEFAULT_PROFILING_UPLOAD_COMPRESSION = "on";
    public static final int DEFAULT_PROFILING_PROXY_PORT = 8080;
    public static final int DEFAULT_PROFILING_EXCEPTION_SAMPLE_LIMIT = 10_000;
    public static final int DEFAULT_PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS = 50;
    public static final int DEFAULT_PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE = 10000;

    private static final String SPLIT_BY_SPACE_OR_COMMA_REGEX = "[,\\s]+";

    private static final boolean DEFAULT_TRACE_REPORT_HOSTNAME = false;
    private static final String DEFAULT_TRACE_ANNOTATIONS = null;
    private static final boolean DEFAULT_TRACE_EXECUTORS_ALL = false;
    private static final String DEFAULT_TRACE_EXECUTORS = "";
    private static final String DEFAULT_TRACE_METHODS = null;
    public static final boolean DEFAULT_TRACE_ANALYTICS_ENABLED = false;
    public static final float DEFAULT_ANALYTICS_SAMPLE_RATE = 1.0f;
    public static final double DEFAULT_TRACE_RATE_LIMIT = 100;

    public enum PropagationStyle {
        DATADOG,
        B3,
        B3MULTI,
        TRACECONTEXT,
        HAYSTACK
    }

    /**
     * A tag intended for internal use only, hence not added to the public api DDTags class.
     */
    private static final String INTERNAL_HOST_NAME = "_dd.hostname";

    /**
     * this is a random UUID that gets generated on JVM start up and is attached to every root span
     * and every JMX metric that is sent out.
     */
    private final String runtimeId;

    /**
     * Note: this has effect only on profiling site. Traces are sent to Datadog agent and are not
     * affected by this setting.
     */
    private final String site;

    private final String serviceName;
    private final boolean traceEnabled;
    private final boolean integrationsEnabled;
    private final String writerType;
    private final String agentHost;
    private final int agentPort;
    private final String agentUnixDomainSocket;
    private final boolean prioritySamplingEnabled;
    private final boolean traceResolverEnabled;
    private final Map<String, String> serviceMapping;
    private final Map<String, String> tags;
    private final Map<String, String> spanTags;
    private final Map<String, String> jmxTags;
    private final List<String> excludedClasses;
    private final Map<String, String> headerTags;
    private final Set<Integer> httpServerErrorStatuses;
    private final Set<Integer> httpClientErrorStatuses;
    private final boolean httpServerTagQueryString;
    private final boolean httpClientTagQueryString;
    private final boolean httpClientSplitByDomain;
    private final boolean dbClientSplitByInstance;
    private final Set<String> splitByTags;
    private final Integer scopeDepthLimit;
    private final Integer partialFlushMinSpans;
    private final boolean runtimeContextFieldInjection;
    private final Set<PropagationStyle> propagationStylesToExtract;
    private final Set<PropagationStyle> propagationStylesToInject;

    private final boolean jmxFetchEnabled;
    private final String jmxFetchConfigDir;
    private final List<String> jmxFetchConfigs;
    @Deprecated
    private final List<String> jmxFetchMetricsConfigs;
    private final Integer jmxFetchCheckPeriod;
    private final Integer jmxFetchRefreshBeansPeriod;
    private final String jmxFetchStatsdHost;
    private final Integer jmxFetchStatsdPort;

    // These values are default-ed to those of jmx fetch values as needed
    private final boolean healthMetricsEnabled;
    private final String healthMetricsStatsdHost;
    private final Integer healthMetricsStatsdPort;

    private final boolean logsInjectionEnabled;
    private final boolean reportHostName;

    private final String traceAnnotations;

    private final String traceMethods;

    private final boolean traceExecutorsAll;
    private final List<String> traceExecutors;

    private final boolean traceAnalyticsEnabled;

    private final Map<String, String> traceSamplingServiceRules;
    private final Map<String, String> traceSamplingOperationRules;
    private final Double traceSampleRate;
    private final Double traceRateLimit;

    private final boolean profilingEnabled;
    @Deprecated
    private final String profilingUrl;
    private final Map<String, String> profilingTags;
    private final int profilingStartDelay;
    private final boolean profilingStartForceFirst;
    private final int profilingUploadPeriod;
    private final String profilingTemplateOverrideFile;
    private final int profilingUploadTimeout;
    private final String profilingUploadCompression;
    private final String profilingProxyHost;
    private final int profilingProxyPort;
    private final String profilingProxyUsername;
    private final String profilingProxyPassword;
    private final int profilingExceptionSampleLimit;
    private final int profilingExceptionHistogramTopItems;
    private final int profilingExceptionHistogramMaxCollectionSize;

    // Values from an optionally provided properties file
    private static Properties propertiesFromConfigFile;

    // Read order: System Properties -> Env Variables, [-> properties file], [-> default value]
    // Visible for testing
    Config() {
        propertiesFromConfigFile = loadConfigurationFile();

        runtimeId = UUID.randomUUID().toString();

        site = getSettingFromEnvironment(SITE, DEFAULT_SITE);
        serviceName =
                getSettingFromEnvironment(
                        SERVICE, getSettingFromEnvironment(SERVICE_NAME, DEFAULT_SERVICE_NAME));

        traceEnabled = getBooleanSettingFromEnvironment(TRACE_ENABLED, DEFAULT_TRACE_ENABLED);
        integrationsEnabled =
                getBooleanSettingFromEnvironment(INTEGRATIONS_ENABLED, DEFAULT_INTEGRATIONS_ENABLED);
        writerType = getSettingFromEnvironment(WRITER_TYPE, DEFAULT_AGENT_WRITER_TYPE);
        agentHost = getSettingFromEnvironment(AGENT_HOST, DEFAULT_AGENT_HOST);
        agentPort =
                getIntegerSettingFromEnvironment(
                        TRACE_AGENT_PORT,
                        getIntegerSettingFromEnvironment(AGENT_PORT_LEGACY, DEFAULT_TRACE_AGENT_PORT));
        agentUnixDomainSocket =
                getSettingFromEnvironment(AGENT_UNIX_DOMAIN_SOCKET, DEFAULT_AGENT_UNIX_DOMAIN_SOCKET);
        prioritySamplingEnabled =
                getBooleanSettingFromEnvironment(PRIORITY_SAMPLING, DEFAULT_PRIORITY_SAMPLING_ENABLED);
        traceResolverEnabled =
                getBooleanSettingFromEnvironment(TRACE_RESOLVER_ENABLED, DEFAULT_TRACE_RESOLVER_ENABLED);
        serviceMapping = getMapSettingFromEnvironment(SERVICE_MAPPING, null);

        {
            final Map<String, String> tags =
                    new HashMap<>(getMapSettingFromEnvironment(GLOBAL_TAGS, null));
            tags.putAll(getMapSettingFromEnvironment(TAGS, null));
            this.tags = getMapWithPropertiesDefinedByEnvironment(tags, ENV, VERSION);
        }

        spanTags = getMapSettingFromEnvironment(SPAN_TAGS, null);
        jmxTags = getMapSettingFromEnvironment(JMX_TAGS, null);

        excludedClasses = getListSettingFromEnvironment(TRACE_CLASSES_EXCLUDE, null);
        headerTags = getMapSettingFromEnvironment(HEADER_TAGS, null);

        httpServerErrorStatuses =
                getIntegerRangeSettingFromEnvironment(
                        HTTP_SERVER_ERROR_STATUSES, DEFAULT_HTTP_SERVER_ERROR_STATUSES);

        httpClientErrorStatuses =
                getIntegerRangeSettingFromEnvironment(
                        HTTP_CLIENT_ERROR_STATUSES, DEFAULT_HTTP_CLIENT_ERROR_STATUSES);

        httpServerTagQueryString =
                getBooleanSettingFromEnvironment(
                        HTTP_SERVER_TAG_QUERY_STRING, DEFAULT_HTTP_SERVER_TAG_QUERY_STRING);

        httpClientTagQueryString =
                getBooleanSettingFromEnvironment(
                        HTTP_CLIENT_TAG_QUERY_STRING, DEFAULT_HTTP_CLIENT_TAG_QUERY_STRING);

        httpClientSplitByDomain =
                getBooleanSettingFromEnvironment(
                        HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN, DEFAULT_HTTP_CLIENT_SPLIT_BY_DOMAIN);

        dbClientSplitByInstance =
                getBooleanSettingFromEnvironment(
                        DB_CLIENT_HOST_SPLIT_BY_INSTANCE, DEFAULT_DB_CLIENT_HOST_SPLIT_BY_INSTANCE);

        splitByTags =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(
                                getListSettingFromEnvironment(SPLIT_BY_TAGS, DEFAULT_SPLIT_BY_TAGS)));

        scopeDepthLimit =
                getIntegerSettingFromEnvironment(SCOPE_DEPTH_LIMIT, DEFAULT_SCOPE_DEPTH_LIMIT);

        partialFlushMinSpans =
                getIntegerSettingFromEnvironment(PARTIAL_FLUSH_MIN_SPANS, DEFAULT_PARTIAL_FLUSH_MIN_SPANS);

        runtimeContextFieldInjection =
                getBooleanSettingFromEnvironment(
                        RUNTIME_CONTEXT_FIELD_INJECTION, DEFAULT_RUNTIME_CONTEXT_FIELD_INJECTION);

        propagationStylesToExtract =
                getPropagationStyleSetSettingFromEnvironmentOrDefault(
                        PROPAGATION_STYLE_EXTRACT, DEFAULT_PROPAGATION_STYLE_EXTRACT);
        propagationStylesToInject =
                getPropagationStyleSetSettingFromEnvironmentOrDefault(
                        PROPAGATION_STYLE_INJECT, DEFAULT_PROPAGATION_STYLE_INJECT);

        jmxFetchEnabled =
                getBooleanSettingFromEnvironment(JMX_FETCH_ENABLED, DEFAULT_JMX_FETCH_ENABLED);
        jmxFetchConfigDir = getSettingFromEnvironment(JMX_FETCH_CONFIG_DIR, null);
        jmxFetchConfigs = getListSettingFromEnvironment(JMX_FETCH_CONFIG, null);
        jmxFetchMetricsConfigs = getListSettingFromEnvironment(JMX_FETCH_METRICS_CONFIGS, null);
        jmxFetchCheckPeriod = getIntegerSettingFromEnvironment(JMX_FETCH_CHECK_PERIOD, null);
        jmxFetchRefreshBeansPeriod =
                getIntegerSettingFromEnvironment(JMX_FETCH_REFRESH_BEANS_PERIOD, null);
        jmxFetchStatsdHost = getSettingFromEnvironment(JMX_FETCH_STATSD_HOST, null);
        jmxFetchStatsdPort =
                getIntegerSettingFromEnvironment(JMX_FETCH_STATSD_PORT, DEFAULT_JMX_FETCH_STATSD_PORT);

        // Writer.Builder createMonitor will use the values of the JMX fetch & agent to fill-in defaults
        healthMetricsEnabled =
                getBooleanSettingFromEnvironment(HEALTH_METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        healthMetricsStatsdHost = getSettingFromEnvironment(HEALTH_METRICS_STATSD_HOST, null);
        healthMetricsStatsdPort = getIntegerSettingFromEnvironment(HEALTH_METRICS_STATSD_PORT, null);

        logsInjectionEnabled =
                getBooleanSettingFromEnvironment(LOGS_INJECTION_ENABLED, DEFAULT_LOGS_INJECTION_ENABLED);
        reportHostName =
                getBooleanSettingFromEnvironment(TRACE_REPORT_HOSTNAME, DEFAULT_TRACE_REPORT_HOSTNAME);

        traceAnnotations = getSettingFromEnvironment(TRACE_ANNOTATIONS, DEFAULT_TRACE_ANNOTATIONS);

        traceMethods = getSettingFromEnvironment(TRACE_METHODS, DEFAULT_TRACE_METHODS);

        traceExecutorsAll =
                getBooleanSettingFromEnvironment(TRACE_EXECUTORS_ALL, DEFAULT_TRACE_EXECUTORS_ALL);

        traceExecutors = getListSettingFromEnvironment(TRACE_EXECUTORS, DEFAULT_TRACE_EXECUTORS);

        traceAnalyticsEnabled =
                getBooleanSettingFromEnvironment(TRACE_ANALYTICS_ENABLED, DEFAULT_TRACE_ANALYTICS_ENABLED);

        traceSamplingServiceRules = getMapSettingFromEnvironment(TRACE_SAMPLING_SERVICE_RULES, null);
        traceSamplingOperationRules =
                getMapSettingFromEnvironment(TRACE_SAMPLING_OPERATION_RULES, null);
        traceSampleRate = getDoubleSettingFromEnvironment(TRACE_SAMPLE_RATE, null);
        traceRateLimit = getDoubleSettingFromEnvironment(TRACE_RATE_LIMIT, DEFAULT_TRACE_RATE_LIMIT);

        profilingEnabled =
                getBooleanSettingFromEnvironment(PROFILING_ENABLED, DEFAULT_PROFILING_ENABLED);
        profilingUrl = getSettingFromEnvironment(PROFILING_URL, null);

        profilingTags = getMapSettingFromEnvironment(PROFILING_TAGS, null);
        profilingStartDelay =
                getIntegerSettingFromEnvironment(PROFILING_START_DELAY, DEFAULT_PROFILING_START_DELAY);
        profilingStartForceFirst =
                getBooleanSettingFromEnvironment(
                        PROFILING_START_FORCE_FIRST, DEFAULT_PROFILING_START_FORCE_FIRST);
        profilingUploadPeriod =
                getIntegerSettingFromEnvironment(PROFILING_UPLOAD_PERIOD, DEFAULT_PROFILING_UPLOAD_PERIOD);
        profilingTemplateOverrideFile =
                getSettingFromEnvironment(PROFILING_TEMPLATE_OVERRIDE_FILE, null);
        profilingUploadTimeout =
                getIntegerSettingFromEnvironment(
                        PROFILING_UPLOAD_TIMEOUT, DEFAULT_PROFILING_UPLOAD_TIMEOUT);
        profilingUploadCompression =
                getSettingFromEnvironment(
                        PROFILING_UPLOAD_COMPRESSION, DEFAULT_PROFILING_UPLOAD_COMPRESSION);
        profilingProxyHost = getSettingFromEnvironment(PROFILING_PROXY_HOST, null);
        profilingProxyPort =
                getIntegerSettingFromEnvironment(PROFILING_PROXY_PORT, DEFAULT_PROFILING_PROXY_PORT);
        profilingProxyUsername = getSettingFromEnvironment(PROFILING_PROXY_USERNAME, null);
        profilingProxyPassword = getSettingFromEnvironment(PROFILING_PROXY_PASSWORD, null);

        profilingExceptionSampleLimit =
                getIntegerSettingFromEnvironment(
                        PROFILING_EXCEPTION_SAMPLE_LIMIT, DEFAULT_PROFILING_EXCEPTION_SAMPLE_LIMIT);
        profilingExceptionHistogramTopItems =
                getIntegerSettingFromEnvironment(
                        PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS,
                        DEFAULT_PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS);
        profilingExceptionHistogramMaxCollectionSize =
                getIntegerSettingFromEnvironment(
                        PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE,
                        DEFAULT_PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE);
    }

    // Read order: Properties -> Parent
    private Config(final Properties properties, final Config parent) {
        runtimeId = parent.runtimeId;

        site = properties.getProperty(SITE, parent.site);
        serviceName =
                properties.getProperty(SERVICE, properties.getProperty(SERVICE_NAME, parent.serviceName));

        traceEnabled = getPropertyBooleanValue(properties, TRACE_ENABLED, parent.traceEnabled);
        integrationsEnabled =
                getPropertyBooleanValue(properties, INTEGRATIONS_ENABLED, parent.integrationsEnabled);
        writerType = properties.getProperty(WRITER_TYPE, parent.writerType);
        agentHost = properties.getProperty(AGENT_HOST, parent.agentHost);
        agentPort =
                getPropertyIntegerValue(
                        properties,
                        TRACE_AGENT_PORT,
                        getPropertyIntegerValue(properties, AGENT_PORT_LEGACY, parent.agentPort));
        agentUnixDomainSocket =
                properties.getProperty(AGENT_UNIX_DOMAIN_SOCKET, parent.agentUnixDomainSocket);
        prioritySamplingEnabled =
                getPropertyBooleanValue(properties, PRIORITY_SAMPLING, parent.prioritySamplingEnabled);
        traceResolverEnabled =
                getPropertyBooleanValue(properties, TRACE_RESOLVER_ENABLED, parent.traceResolverEnabled);
        serviceMapping = getPropertyMapValue(properties, SERVICE_MAPPING, parent.serviceMapping);

        {
            final Map<String, String> preTags =
                    new HashMap<>(
                            getPropertyMapValue(properties, GLOBAL_TAGS, Collections.<String, String>emptyMap()));
            preTags.putAll(getPropertyMapValue(properties, TAGS, parent.tags));
            this.tags = overwriteKeysFromProperties(preTags, properties, ENV, VERSION);
        }
        spanTags = getPropertyMapValue(properties, SPAN_TAGS, parent.spanTags);
        jmxTags = getPropertyMapValue(properties, JMX_TAGS, parent.jmxTags);
        excludedClasses =
                getPropertyListValue(properties, TRACE_CLASSES_EXCLUDE, parent.excludedClasses);
        headerTags = getPropertyMapValue(properties, HEADER_TAGS, parent.headerTags);

        httpServerErrorStatuses =
                getPropertyIntegerRangeValue(
                        properties, HTTP_SERVER_ERROR_STATUSES, parent.httpServerErrorStatuses);

        httpClientErrorStatuses =
                getPropertyIntegerRangeValue(
                        properties, HTTP_CLIENT_ERROR_STATUSES, parent.httpClientErrorStatuses);

        httpServerTagQueryString =
                getPropertyBooleanValue(
                        properties, HTTP_SERVER_TAG_QUERY_STRING, parent.httpServerTagQueryString);

        httpClientTagQueryString =
                getPropertyBooleanValue(
                        properties, HTTP_CLIENT_TAG_QUERY_STRING, parent.httpClientTagQueryString);

        httpClientSplitByDomain =
                getPropertyBooleanValue(
                        properties, HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN, parent.httpClientSplitByDomain);

        dbClientSplitByInstance =
                getPropertyBooleanValue(
                        properties, DB_CLIENT_HOST_SPLIT_BY_INSTANCE, parent.dbClientSplitByInstance);

        splitByTags =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(
                                getPropertyListValue(
                                        properties, SPLIT_BY_TAGS, new ArrayList<>(parent.splitByTags))));

        scopeDepthLimit =
                getPropertyIntegerValue(properties, SCOPE_DEPTH_LIMIT, parent.scopeDepthLimit);

        partialFlushMinSpans =
                getPropertyIntegerValue(properties, PARTIAL_FLUSH_MIN_SPANS, parent.partialFlushMinSpans);

        runtimeContextFieldInjection =
                getPropertyBooleanValue(
                        properties, RUNTIME_CONTEXT_FIELD_INJECTION, parent.runtimeContextFieldInjection);

        final Set<PropagationStyle> parsedPropagationStylesToExtract =
                getPropagationStyleSetFromPropertyValue(properties, PROPAGATION_STYLE_EXTRACT);
        propagationStylesToExtract =
                parsedPropagationStylesToExtract == null
                        ? parent.propagationStylesToExtract
                        : parsedPropagationStylesToExtract;
        final Set<PropagationStyle> parsedPropagationStylesToInject =
                getPropagationStyleSetFromPropertyValue(properties, PROPAGATION_STYLE_INJECT);
        propagationStylesToInject =
                parsedPropagationStylesToInject == null
                        ? parent.propagationStylesToInject
                        : parsedPropagationStylesToInject;

        jmxFetchEnabled =
                getPropertyBooleanValue(properties, JMX_FETCH_ENABLED, parent.jmxFetchEnabled);
        jmxFetchConfigDir = properties.getProperty(JMX_FETCH_CONFIG_DIR, parent.jmxFetchConfigDir);
        jmxFetchConfigs = getPropertyListValue(properties, JMX_FETCH_CONFIG, parent.jmxFetchConfigs);
        jmxFetchMetricsConfigs =
                getPropertyListValue(properties, JMX_FETCH_METRICS_CONFIGS, parent.jmxFetchMetricsConfigs);
        jmxFetchCheckPeriod =
                getPropertyIntegerValue(properties, JMX_FETCH_CHECK_PERIOD, parent.jmxFetchCheckPeriod);
        jmxFetchRefreshBeansPeriod =
                getPropertyIntegerValue(
                        properties, JMX_FETCH_REFRESH_BEANS_PERIOD, parent.jmxFetchRefreshBeansPeriod);
        jmxFetchStatsdHost = properties.getProperty(JMX_FETCH_STATSD_HOST, parent.jmxFetchStatsdHost);
        jmxFetchStatsdPort =
                getPropertyIntegerValue(properties, JMX_FETCH_STATSD_PORT, parent.jmxFetchStatsdPort);

        healthMetricsEnabled =
                getPropertyBooleanValue(properties, HEALTH_METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
        healthMetricsStatsdHost =
                properties.getProperty(HEALTH_METRICS_STATSD_HOST, parent.healthMetricsStatsdHost);
        healthMetricsStatsdPort =
                getPropertyIntegerValue(
                        properties, HEALTH_METRICS_STATSD_PORT, parent.healthMetricsStatsdPort);

        logsInjectionEnabled =
                getBooleanSettingFromEnvironment(LOGS_INJECTION_ENABLED, DEFAULT_LOGS_INJECTION_ENABLED);
        reportHostName =
                getPropertyBooleanValue(properties, TRACE_REPORT_HOSTNAME, parent.reportHostName);

        traceAnnotations = properties.getProperty(TRACE_ANNOTATIONS, parent.traceAnnotations);

        traceMethods = properties.getProperty(TRACE_METHODS, parent.traceMethods);

        traceExecutorsAll =
                getPropertyBooleanValue(properties, TRACE_EXECUTORS_ALL, parent.traceExecutorsAll);
        traceExecutors = getPropertyListValue(properties, TRACE_EXECUTORS, parent.traceExecutors);

        traceAnalyticsEnabled =
                getPropertyBooleanValue(properties, TRACE_ANALYTICS_ENABLED, parent.traceAnalyticsEnabled);

        traceSamplingServiceRules =
                getPropertyMapValue(
                        properties, TRACE_SAMPLING_SERVICE_RULES, parent.traceSamplingServiceRules);
        traceSamplingOperationRules =
                getPropertyMapValue(
                        properties, TRACE_SAMPLING_OPERATION_RULES, parent.traceSamplingOperationRules);
        traceSampleRate = getPropertyDoubleValue(properties, TRACE_SAMPLE_RATE, parent.traceSampleRate);
        traceRateLimit = getPropertyDoubleValue(properties, TRACE_RATE_LIMIT, parent.traceRateLimit);

        profilingEnabled =
                getPropertyBooleanValue(properties, PROFILING_ENABLED, parent.profilingEnabled);
        profilingUrl = properties.getProperty(PROFILING_URL, parent.profilingUrl);
        profilingTags = getPropertyMapValue(properties, PROFILING_TAGS, parent.profilingTags);
        profilingStartDelay =
                getPropertyIntegerValue(properties, PROFILING_START_DELAY, parent.profilingStartDelay);
        profilingStartForceFirst =
                getPropertyBooleanValue(
                        properties, PROFILING_START_FORCE_FIRST, parent.profilingStartForceFirst);
        profilingUploadPeriod =
                getPropertyIntegerValue(properties, PROFILING_UPLOAD_PERIOD, parent.profilingUploadPeriod);
        profilingTemplateOverrideFile =
                properties.getProperty(
                        PROFILING_TEMPLATE_OVERRIDE_FILE, parent.profilingTemplateOverrideFile);
        profilingUploadTimeout =
                getPropertyIntegerValue(
                        properties, PROFILING_UPLOAD_TIMEOUT, parent.profilingUploadTimeout);
        profilingUploadCompression =
                properties.getProperty(PROFILING_UPLOAD_COMPRESSION, parent.profilingUploadCompression);
        profilingProxyHost = properties.getProperty(PROFILING_PROXY_HOST, parent.profilingProxyHost);
        profilingProxyPort =
                getPropertyIntegerValue(properties, PROFILING_PROXY_PORT, parent.profilingProxyPort);
        profilingProxyUsername =
                properties.getProperty(PROFILING_PROXY_USERNAME, parent.profilingProxyUsername);
        profilingProxyPassword =
                properties.getProperty(PROFILING_PROXY_PASSWORD, parent.profilingProxyPassword);

        profilingExceptionSampleLimit =
                getPropertyIntegerValue(
                        properties, PROFILING_EXCEPTION_SAMPLE_LIMIT, parent.profilingExceptionSampleLimit);

        profilingExceptionHistogramTopItems =
                getPropertyIntegerValue(
                        properties,
                        PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS,
                        parent.profilingExceptionHistogramTopItems);
        profilingExceptionHistogramMaxCollectionSize =
                getPropertyIntegerValue(
                        properties,
                        PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE,
                        parent.profilingExceptionHistogramMaxCollectionSize);

    }

    /**
     * @return A map of tags to be applied only to the local application root span.
     */
    public Map<String, String> getLocalRootSpanTags() {
        final Map<String, String> runtimeTags = getRuntimeTags();
        final Map<String, String> result = new HashMap<>(runtimeTags);
        result.put(LANGUAGE_TAG_KEY, LANGUAGE_TAG_VALUE);

        if (reportHostName) {
            final String hostName = getHostName();
            if (null != hostName && !hostName.isEmpty()) {
                result.put(INTERNAL_HOST_NAME, hostName);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedSpanTags() {
        // Do not include runtimeId into span tags: we only want that added to the root span
        final Map<String, String> result = newHashMap(getGlobalTags().size() + spanTags.size());
        result.putAll(getGlobalTags());
        result.putAll(spanTags);
        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedJmxTags() {
        final Map<String, String> runtimeTags = getRuntimeTags();
        final Map<String, String> result =
                newHashMap(
                        getGlobalTags().size() + jmxTags.size() + runtimeTags.size() + 1 /* for serviceName */);
        result.putAll(getGlobalTags());
        result.putAll(jmxTags);
        result.putAll(runtimeTags);
        // service name set here instead of getRuntimeTags because apm already manages the service tag
        // and may chose to override it.
        // Additionally, infra/JMX metrics require `service` rather than APM's `service.name` tag
        result.put(SERVICE_TAG, serviceName);
        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedProfilingTags() {
        final Map<String, String> runtimeTags = getRuntimeTags();
        final String host = getHostName();
        final Map<String, String> result =
                newHashMap(
                        getGlobalTags().size()
                                + profilingTags.size()
                                + runtimeTags.size()
                                + 3 /* for serviceName and host and language */);
        result.put(HOST_TAG, host); // Host goes first to allow to override it
        result.putAll(getGlobalTags());
        result.putAll(profilingTags);
        result.putAll(runtimeTags);
        // service name set here instead of getRuntimeTags because apm already manages the service tag
        // and may chose to override it.
        result.put(SERVICE_TAG, serviceName);
        result.put(LANGUAGE_TAG_KEY, LANGUAGE_TAG_VALUE);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the sample rate for the specified instrumentation or {@link
     * #DEFAULT_ANALYTICS_SAMPLE_RATE} if none specified.
     */
    public float getInstrumentationAnalyticsSampleRate(final String... aliases) {
        for (final String alias : aliases) {
            final Float rate = getFloatSettingFromEnvironment(alias + ".analytics.sample-rate", null);
            if (null != rate) {
                return rate;
            }
        }
        return DEFAULT_ANALYTICS_SAMPLE_RATE;
    }

    /**
     * Provide 'global' tags, i.e. tags set everywhere. We have to support old (dd.trace.global.tags)
     * version of this setting if new (dd.tags) version has not been specified.
     */
    private Map<String, String> getGlobalTags() {
        return tags;
    }

    /**
     * Return a map of tags required by the datadog backend to link runtime metrics (i.e. jmx) and
     * traces.
     *
     * <p>These tags must be applied to every runtime metrics and placed on the root span of every
     * trace.
     *
     * @return A map of tag-name -> tag-value
     */
    private Map<String, String> getRuntimeTags() {
        final Map<String, String> result = newHashMap(2);
        result.put(RUNTIME_ID_TAG, runtimeId);
        return Collections.unmodifiableMap(result);
    }

    public String getFinalProfilingUrl() {
        if (profilingUrl == null) {
            return String.format(Locale.US, PROFILING_URL_TEMPLATE, site);
        } else {
            return profilingUrl;
        }
    }

    public boolean isIntegrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        return integrationEnabled(integrationNames, defaultEnabled);
    }

    /**
     * @param integrationNames
     * @param defaultEnabled
     * @return
     * @deprecated This method should only be used internally. Use the instance getter instead {@link
     * #isIntegrationEnabled(SortedSet, boolean)}.
     */
    @Deprecated
    private static boolean integrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        // If default is enabled, we want to enable individually,
        // if default is disabled, we want to disable individually.
        boolean anyEnabled = defaultEnabled;
        for (final String name : integrationNames) {
            final boolean configEnabled =
                    getBooleanSettingFromEnvironment("integration." + name + ".enabled", defaultEnabled);
            if (defaultEnabled) {
                anyEnabled &= configEnabled;
            } else {
                anyEnabled |= configEnabled;
            }
        }
        return anyEnabled;
    }

    public boolean isJmxFetchIntegrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        return jmxFetchIntegrationEnabled(integrationNames, defaultEnabled);
    }

    public boolean isRuleEnabled(final String name) {
        return getBooleanSettingFromEnvironment("trace." + name + ".enabled", true)
                && getBooleanSettingFromEnvironment("trace." + name.toLowerCase(Locale.US) + ".enabled", true);
    }

    /**
     * @param integrationNames
     * @param defaultEnabled
     * @return
     * @deprecated This method should only be used internally. Use the instance getter instead {@link
     * #isJmxFetchIntegrationEnabled(SortedSet, boolean)}.
     */
    @Deprecated
    public static boolean jmxFetchIntegrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        // If default is enabled, we want to enable individually,
        // if default is disabled, we want to disable individually.
        boolean anyEnabled = defaultEnabled;
        for (final String name : integrationNames) {
            final boolean configEnabled =
                    getBooleanSettingFromEnvironment("jmxfetch." + name + ".enabled", defaultEnabled);
            if (defaultEnabled) {
                anyEnabled &= configEnabled;
            } else {
                anyEnabled |= configEnabled;
            }
        }
        return anyEnabled;
    }

    public boolean isTraceAnalyticsIntegrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        return traceAnalyticsIntegrationEnabled(integrationNames, defaultEnabled);
    }

    /**
     * @param integrationNames
     * @param defaultEnabled
     * @return
     * @deprecated This method should only be used internally. Use the instance getter instead {@link
     * #isTraceAnalyticsIntegrationEnabled(SortedSet, boolean)}.
     */
    @Deprecated
    public static boolean traceAnalyticsIntegrationEnabled(
            final SortedSet<String> integrationNames, final boolean defaultEnabled) {
        // If default is enabled, we want to enable individually,
        // if default is disabled, we want to disable individually.
        boolean anyEnabled = defaultEnabled;
        for (final String name : integrationNames) {
            final boolean configEnabled =
                    getBooleanSettingFromEnvironment(name + ".analytics.enabled", defaultEnabled);
            if (defaultEnabled) {
                anyEnabled &= configEnabled;
            } else {
                anyEnabled |= configEnabled;
            }
        }
        return anyEnabled;
    }

    /**
     * Helper method that takes the name, adds a "dd." prefix then checks for System Properties of
     * that name. If none found, the name is converted to an Environment Variable and used to check
     * the env. If none of the above returns a value, then an optional properties file if checked. If
     * setting is not configured in either location, <code>defaultValue</code> is returned.
     *
     * @param name
     * @param defaultValue
     * @return
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    public static String getSettingFromEnvironment(final String name, final String defaultValue) {
        String value;
        final String systemPropertyName = propertyNameToSystemPropertyName(name);

        // System properties and properties provided from command line have the highest precedence
        value = System.getProperties().getProperty(systemPropertyName);
        if (null != value) {
            return value;
        }

        // getting setting from env or from config file is not supported on Android

        return defaultValue;
    }

    /**
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    private static Map<String, String> getMapSettingFromEnvironment(
            final String name, final String defaultValue) {
        return parseMap(
                getSettingFromEnvironment(name, defaultValue), propertyNameToSystemPropertyName(name));
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a list by
     * splitting on `,`.
     *
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    private static List<String> getListSettingFromEnvironment(
            final String name, final String defaultValue) {
        return parseList(getSettingFromEnvironment(name, defaultValue));
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Boolean.
     *
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    public static Boolean getBooleanSettingFromEnvironment(
            final String name, final Boolean defaultValue) {
        return getSettingFromEnvironmentWithLog(name, Boolean.class, defaultValue);
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Float.
     *
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    public static Float getFloatSettingFromEnvironment(final String name, final Float defaultValue) {
        return getSettingFromEnvironmentWithLog(name, Float.class, defaultValue);
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Double.
     *
     * @deprecated This method should only be used internally. Use the explicit getter instead.
     */
    @Deprecated
    private static Double getDoubleSettingFromEnvironment(
            final String name, final Double defaultValue) {
        return getSettingFromEnvironmentWithLog(name, Double.class, defaultValue);
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Integer.
     */
    private static Integer getIntegerSettingFromEnvironment(
            final String name, final Integer defaultValue) {
        return getSettingFromEnvironmentWithLog(name, Integer.class, defaultValue);
    }

    private static <T> T getSettingFromEnvironmentWithLog(
            final String name, Class<T> tClass, final T defaultValue) {
        try {
            return valueOf(getSettingFromEnvironment(name, null), tClass, defaultValue);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a set of
     * strings splitting by space or comma.
     */
    private static Set<PropagationStyle> getPropagationStyleSetSettingFromEnvironmentOrDefault(
            final String name, final String defaultValue) {
        final String value = getSettingFromEnvironment(name, defaultValue);
        Set<PropagationStyle> result =
                convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(value));

        if (result.isEmpty()) {
            // Treat empty parsing result as no value and use default instead
            result =
                    convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(defaultValue));
        }

        return result;
    }

    private static Set<Integer> getIntegerRangeSettingFromEnvironment(
            final String name, final Set<Integer> defaultValue) {
        final String value = getSettingFromEnvironment(name, null);
        try {
            return value == null ? defaultValue : parseIntegerRangeSet(value, name);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts the property name, e.g. 'service.name' into a public environment variable name, e.g.
     * `DD_SERVICE_NAME`.
     *
     * @param setting The setting name, e.g. `service.name`
     * @return The public facing environment variable name
     */
    private static String propertyNameToEnvironmentVariableName(final String setting) {
        return ENV_REPLACEMENT
                .matcher(propertyNameToSystemPropertyName(setting).toUpperCase(Locale.US))
                .replaceAll("_");
    }

    /**
     * Converts the property name, e.g. 'service.name' into a public system property name, e.g.
     * `dd.service.name`.
     *
     * @param setting The setting name, e.g. `service.name`
     * @return The public facing system property name
     */
    private static String propertyNameToSystemPropertyName(final String setting) {
        return PREFIX + setting;
    }

    /**
     * @param value        to parse by tClass::valueOf
     * @param tClass       should contain static parsing method "T valueOf(String)"
     * @param defaultValue
     * @param <T>
     * @return value == null || value.trim().isEmpty() ? defaultValue : tClass.valueOf(value)
     * @throws NumberFormatException
     */
    private static <T> T valueOf(
            final String value, final Class<T> tClass, final T defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            Method method = tClass.getMethod("valueOf", String.class);
            return (T) method.invoke(null, value);
        } catch (NumberFormatException e) {
            throw e;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new NumberFormatException(e.toString());
        } catch (Throwable e) {
            throw new NumberFormatException(e.toString());
        }
    }

    private static Map<String, String> getPropertyMapValue(
            final Properties properties, final String name, final Map<String, String> defaultValue) {
        final String value = properties.getProperty(name);
        return value == null || value.trim().isEmpty() ? defaultValue : parseMap(value, name);
    }

    private static List<String> getPropertyListValue(
            final Properties properties, final String name, final List<String> defaultValue) {
        final String value = properties.getProperty(name);
        return value == null || value.trim().isEmpty() ? defaultValue : parseList(value);
    }

    private static Boolean getPropertyBooleanValue(
            final Properties properties, final String name, final Boolean defaultValue) {
        return valueOf(properties.getProperty(name), Boolean.class, defaultValue);
    }

    private static Integer getPropertyIntegerValue(
            final Properties properties, final String name, final Integer defaultValue) {
        return valueOf(properties.getProperty(name), Integer.class, defaultValue);
    }

    private static Double getPropertyDoubleValue(
            final Properties properties, final String name, final Double defaultValue) {
        return valueOf(properties.getProperty(name), Double.class, defaultValue);
    }

    private static Set<PropagationStyle> getPropagationStyleSetFromPropertyValue(
            final Properties properties, final String name) {
        final String value = properties.getProperty(name);
        if (value != null) {
            final Set<PropagationStyle> result =
                    convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(value));
            if (!result.isEmpty()) {
                return result;
            }
        }
        // null means parent value should be used
        return null;
    }

    private static Set<Integer> getPropertyIntegerRangeValue(
            final Properties properties, final String name, final Set<Integer> defaultValue) {
        final String value = properties.getProperty(name);
        try {
            return value == null ? defaultValue : parseIntegerRangeSet(value, name);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Map<String, String> parseMap(final String str, final String settingName) {
        // If we ever want to have default values besides an empty map, this will need to change.
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        if (!str.matches("(([^,:]+:[^,:]*,)*([^,:]+:[^,:]*),?)?")) {
            return Collections.emptyMap();
        }

        final String[] tokens = str.split(",", -1);
        final Map<String, String> map = newHashMap(tokens.length);

        for (final String token : tokens) {
            final String[] keyValue = token.split(":", -1);
            if (keyValue.length == 2) {
                final String key = keyValue[0].trim();
                final String value = keyValue[1].trim();
                if (value.length() <= 0) {
                    continue;
                }
                map.put(key, value);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Set<Integer> parseIntegerRangeSet(String str, final String settingName)
            throws NumberFormatException {
        str = str.replaceAll("\\s", "");
        if (!str.matches("\\d{3}(?:-\\d{3})?(?:,\\d{3}(?:-\\d{3})?)*")) {
            throw new NumberFormatException();
        }

        final String[] tokens = str.split(",", -1);
        final Set<Integer> set = new HashSet<>();

        for (final String token : tokens) {
            final String[] range = token.split("-", -1);
            if (range.length == 1) {
                set.add(Integer.parseInt(range[0]));
            } else if (range.length == 2) {
                final int left = Integer.parseInt(range[0]);
                final int right = Integer.parseInt(range[1]);
                final int min = Math.min(left, right);
                final int max = Math.max(left, right);
                for (int i = min; i <= max; i++) {
                    set.add(i);
                }
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private static Map<String, String> newHashMap(final int size) {
        return new HashMap<>(size + 1, 1f);
    }

    /**
     * @param map
     * @param propNames
     * @return new unmodifiable copy of {@param map} where properties are overwritten from environment
     */
    private static Map<String, String> getMapWithPropertiesDefinedByEnvironment(
            final Map<String, String> map, final String... propNames) {
        final Map<String, String> res = new HashMap<>(map);
        for (final String propName : propNames) {
            final String val = getSettingFromEnvironment(propName, null);
            if (val != null) {
                res.put(propName, val);
            }
        }
        return Collections.unmodifiableMap(res);
    }

    /**
     * same as {@link Config#getMapWithPropertiesDefinedByEnvironment(Map, String...)} but using
     * {@code properties} as source of values to overwrite inside map
     *
     * @param map
     * @param properties
     * @param keys
     * @return
     */
    private static Map<String, String> overwriteKeysFromProperties(
            final Map<String, String> map,
            final Properties properties,
            final String... keys) {
        final Map<String, String> res = new HashMap<>(map);
        for (final String propName : keys) {
            final String val = properties.getProperty(propName, null);
            if (val != null) {
                res.put(propName, val);
            }
        }
        return Collections.unmodifiableMap(res);
    }

    private static List<String> parseList(final String str) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final String[] tokens = str.split(",", -1);
        // Remove whitespace from each item.
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        return Collections.unmodifiableList(Arrays.asList(tokens));
    }

    private static Set<String> parseStringIntoSetOfNonEmptyStrings(final String str) {
        // Using LinkedHashSet to preserve original string order
        final Set<String> result = new LinkedHashSet<>();
        // Java returns single value when splitting an empty string. We do not need that value, so
        // we need to throw it out.
        for (final String value : str.split(SPLIT_BY_SPACE_OR_COMMA_REGEX)) {
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<PropagationStyle> convertStringSetToPropagationStyleSet(
            final Set<String> input) {
        // Using LinkedHashSet to preserve original string order
        final Set<PropagationStyle> result = new LinkedHashSet<>();
        for (final String value : input) {
            try {
                result.add(PropagationStyle.valueOf(value.toUpperCase(Locale.US)));
            } catch (final IllegalArgumentException e) {
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Loads the optional configuration properties file into the global {@link Properties} object.
     *
     * @return The {@link Properties} object. the returned instance might be empty of file does not
     * exist or if it is in a wrong format.
     */
    private static Properties loadConfigurationFile() {
        final Properties properties = new Properties();

        // Reading from system property first and from env after
        String configurationFilePath =
                System.getProperty(propertyNameToSystemPropertyName(CONFIGURATION_FILE));
        if (null == configurationFilePath) {
            configurationFilePath =
                    System.getenv(propertyNameToEnvironmentVariableName(CONFIGURATION_FILE));
        }
        if (null == configurationFilePath) {
            return properties;
        }

        // Normalizing tilde (~) paths for unix systems
        configurationFilePath =
                configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

        // Configuration properties file is optional
        final File configurationFile = new File(configurationFilePath);
        if (!configurationFile.exists()) {
            return properties;
        }

        try (final FileReader fileReader = new FileReader(configurationFile)) {
            properties.load(fileReader);
        } catch (final FileNotFoundException fnf) {
        } catch (final IOException ioe) {
        }

        return properties;
    }

    /**
     * Returns the detected hostname. First tries locally, then using DNS
     */
    private static String getHostName() {
        String possibleHostname;

        // Try environment variable.  This works in almost all environments
        if (System.getProperty("os.name").startsWith("Windows")) {
            possibleHostname = System.getenv("COMPUTERNAME");
        } else {
            possibleHostname = System.getenv("HOSTNAME");
        }

        if (possibleHostname != null && !possibleHostname.isEmpty()) {
            return possibleHostname.trim();
        }

        // Try hostname command
        try (final BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()))) {
            possibleHostname = reader.readLine();
        } catch (final Exception ignore) {
            // Ignore.  Hostname command is not always available
        }

        if (possibleHostname != null && !possibleHostname.isEmpty()) {
            return possibleHostname.trim();
        }

        // From DNS
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            // If we are not able to detect the hostname we do not throw an exception.
        }

        return null;
    }

    // This has to be placed after all other static fields to give them a chance to initialize
    private static final Config INSTANCE = new Config();

    public static Config get() {
        return INSTANCE;
    }

    public static Config get(final Properties properties) {
        if (properties == null || properties.isEmpty()) {
            return INSTANCE;
        } else {
            return new Config(properties, INSTANCE);
        }
    }

    // region GENERATED GETTERS

    public String getRuntimeId() {
        return runtimeId;
    }

    public String getSite() {
        return site;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public boolean isIntegrationsEnabled() {
        return integrationsEnabled;
    }

    public String getWriterType() {
        return writerType;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public String getAgentUnixDomainSocket() {
        return agentUnixDomainSocket;
    }

    public boolean isPrioritySamplingEnabled() {
        return prioritySamplingEnabled;
    }

    public boolean isTraceResolverEnabled() {
        return traceResolverEnabled;
    }

    public Map<String, String> getServiceMapping() {
        return serviceMapping;
    }

    public List<String> getExcludedClasses() {
        return excludedClasses;
    }

    public Map<String, String> getHeaderTags() {
        return headerTags;
    }

    public Set<Integer> getHttpServerErrorStatuses() {
        return httpServerErrorStatuses;
    }

    public Set<Integer> getHttpClientErrorStatuses() {
        return httpClientErrorStatuses;
    }

    public boolean isHttpServerTagQueryString() {
        return httpServerTagQueryString;
    }

    public boolean isHttpClientTagQueryString() {
        return httpClientTagQueryString;
    }

    public boolean isHttpClientSplitByDomain() {
        return httpClientSplitByDomain;
    }

    public boolean isDbClientSplitByInstance() {
        return dbClientSplitByInstance;
    }

    public Set<String> getSplitByTags() {
        return splitByTags;
    }

    public Integer getScopeDepthLimit() {
        return scopeDepthLimit;
    }

    public Integer getPartialFlushMinSpans() {
        return partialFlushMinSpans;
    }

    public boolean isRuntimeContextFieldInjection() {
        return runtimeContextFieldInjection;
    }

    public Set<Config.PropagationStyle> getPropagationStylesToExtract() {
        return propagationStylesToExtract;
    }

    public Set<PropagationStyle> getPropagationStylesToInject() {
        return propagationStylesToInject;
    }

    public boolean isJmxFetchEnabled() {
        return jmxFetchEnabled;
    }

    public String getJmxFetchConfigDir() {
        return jmxFetchConfigDir;
    }

    public List<String> getJmxFetchConfigs() {
        return jmxFetchConfigs;
    }

    public List<String> getJmxFetchMetricsConfigs() {
        return jmxFetchMetricsConfigs;
    }

    public Integer getJmxFetchCheckPeriod() {
        return jmxFetchCheckPeriod;
    }

    public Integer getJmxFetchRefreshBeansPeriod() {
        return jmxFetchRefreshBeansPeriod;
    }

    public String getJmxFetchStatsdHost() {
        return jmxFetchStatsdHost;
    }

    public Integer getJmxFetchStatsdPort() {
        return jmxFetchStatsdPort;
    }

    public boolean isHealthMetricsEnabled() {
        return healthMetricsEnabled;
    }

    public String getHealthMetricsStatsdHost() {
        return healthMetricsStatsdHost;
    }

    public Integer getHealthMetricsStatsdPort() {
        return healthMetricsStatsdPort;
    }

    public boolean isLogsInjectionEnabled() {
        return logsInjectionEnabled;
    }

    public boolean isReportHostName() {
        return reportHostName;
    }

    public String getTraceAnnotations() {
        return traceAnnotations;
    }

    public String getTraceMethods() {
        return traceMethods;
    }

    public boolean isTraceExecutorsAll() {
        return traceExecutorsAll;
    }

    public List<String> getTraceExecutors() {
        return traceExecutors;
    }

    public boolean isTraceAnalyticsEnabled() {
        return traceAnalyticsEnabled;
    }

    public Map<String, String> getTraceSamplingServiceRules() {
        return traceSamplingServiceRules;
    }

    public Map<String, String> getTraceSamplingOperationRules() {
        return traceSamplingOperationRules;
    }

    public Double getTraceSampleRate() {
        return traceSampleRate;
    }

    public Double getTraceRateLimit() {
        return traceRateLimit;
    }

    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }

    public int getProfilingStartDelay() {
        return profilingStartDelay;
    }

    public boolean isProfilingStartForceFirst() {
        return profilingStartForceFirst;
    }

    public int getProfilingUploadPeriod() {
        return profilingUploadPeriod;
    }

    public String getProfilingTemplateOverrideFile() {
        return profilingTemplateOverrideFile;
    }

    public int getProfilingUploadTimeout() {
        return profilingUploadTimeout;
    }

    public String getProfilingUploadCompression() {
        return profilingUploadCompression;
    }

    public String getProfilingProxyHost() {
        return profilingProxyHost;
    }

    public int getProfilingProxyPort() {
        return profilingProxyPort;
    }

    public String getProfilingProxyUsername() {
        return profilingProxyUsername;
    }

    public String getProfilingProxyPassword() {
        return profilingProxyPassword;
    }

    public int getProfilingExceptionSampleLimit() {
        return profilingExceptionSampleLimit;
    }

    public int getProfilingExceptionHistogramTopItems() {
        return profilingExceptionHistogramTopItems;
    }

    public int getProfilingExceptionHistogramMaxCollectionSize() {
        return profilingExceptionHistogramMaxCollectionSize;
    }

    // endregion

    // region GENERATED toString()

    @Override
    public String toString() {
        return "Config{" +
                "runtimeId='" + runtimeId + '\'' +
                ", site='" + site + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", traceEnabled=" + traceEnabled +
                ", integrationsEnabled=" + integrationsEnabled +
                ", writerType='" + writerType + '\'' +
                ", agentHost='" + agentHost + '\'' +
                ", agentPort=" + agentPort +
                ", agentUnixDomainSocket='" + agentUnixDomainSocket + '\'' +
                ", prioritySamplingEnabled=" + prioritySamplingEnabled +
                ", traceResolverEnabled=" + traceResolverEnabled +
                ", serviceMapping=" + serviceMapping +
                ", tags=" + tags +
                ", spanTags=" + spanTags +
                ", jmxTags=" + jmxTags +
                ", excludedClasses=" + excludedClasses +
                ", headerTags=" + headerTags +
                ", httpServerErrorStatuses=" + httpServerErrorStatuses +
                ", httpClientErrorStatuses=" + httpClientErrorStatuses +
                ", httpServerTagQueryString=" + httpServerTagQueryString +
                ", httpClientTagQueryString=" + httpClientTagQueryString +
                ", httpClientSplitByDomain=" + httpClientSplitByDomain +
                ", dbClientSplitByInstance=" + dbClientSplitByInstance +
                ", splitByTags=" + splitByTags +
                ", scopeDepthLimit=" + scopeDepthLimit +
                ", partialFlushMinSpans=" + partialFlushMinSpans +
                ", runtimeContextFieldInjection=" + runtimeContextFieldInjection +
                ", propagationStylesToExtract=" + propagationStylesToExtract +
                ", propagationStylesToInject=" + propagationStylesToInject +
                ", jmxFetchEnabled=" + jmxFetchEnabled +
                ", jmxFetchConfigDir='" + jmxFetchConfigDir + '\'' +
                ", jmxFetchConfigs=" + jmxFetchConfigs +
                ", jmxFetchMetricsConfigs=" + jmxFetchMetricsConfigs +
                ", jmxFetchCheckPeriod=" + jmxFetchCheckPeriod +
                ", jmxFetchRefreshBeansPeriod=" + jmxFetchRefreshBeansPeriod +
                ", jmxFetchStatsdHost='" + jmxFetchStatsdHost + '\'' +
                ", jmxFetchStatsdPort=" + jmxFetchStatsdPort +
                ", healthMetricsEnabled=" + healthMetricsEnabled +
                ", healthMetricsStatsdHost='" + healthMetricsStatsdHost + '\'' +
                ", healthMetricsStatsdPort=" + healthMetricsStatsdPort +
                ", logsInjectionEnabled=" + logsInjectionEnabled +
                ", reportHostName=" + reportHostName +
                ", traceAnnotations='" + traceAnnotations + '\'' +
                ", traceMethods='" + traceMethods + '\'' +
                ", traceExecutorsAll=" + traceExecutorsAll +
                ", traceExecutors=" + traceExecutors +
                ", traceAnalyticsEnabled=" + traceAnalyticsEnabled +
                ", traceSamplingServiceRules=" + traceSamplingServiceRules +
                ", traceSamplingOperationRules=" + traceSamplingOperationRules +
                ", traceSampleRate=" + traceSampleRate +
                ", traceRateLimit=" + traceRateLimit +
                ", profilingEnabled=" + profilingEnabled +
                ", profilingUrl='" + profilingUrl + '\'' +
                ", profilingTags=" + profilingTags +
                ", profilingStartDelay=" + profilingStartDelay +
                ", profilingStartForceFirst=" + profilingStartForceFirst +
                ", profilingUploadPeriod=" + profilingUploadPeriod +
                ", profilingTemplateOverrideFile='" + profilingTemplateOverrideFile + '\'' +
                ", profilingUploadTimeout=" + profilingUploadTimeout +
                ", profilingUploadCompression='" + profilingUploadCompression + '\'' +
                ", profilingProxyHost='" + profilingProxyHost + '\'' +
                ", profilingProxyPort=" + profilingProxyPort +
                ", profilingProxyUsername='" + profilingProxyUsername + '\'' +
                ", profilingProxyPassword='" + profilingProxyPassword + '\'' +
                ", profilingExceptionSampleLimit=" + profilingExceptionSampleLimit +
                ", profilingExceptionHistogramTopItems=" + profilingExceptionHistogramTopItems +
                ", profilingExceptionHistogramMaxCollectionSize=" + profilingExceptionHistogramMaxCollectionSize +
                '}';
    }


    // endregion

}
