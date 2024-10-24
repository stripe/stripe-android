package com.stripe.android.core.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SentryEnvelopeHeader(
    @SerialName("event_id") val eventId: String
)

@Serializable
data class SentryUser(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val ipAddress: String? = null
)

@Serializable
data class SentryItemHeader(
    val type: String
)

@Serializable
data class SentryEvent(
    @SerialName("event_id") val eventId: String,
    val timestamp: Double,
    val platform: String,
    val release: String,
    val exception: SentryException,
    val tags: Map<String, String>,
    val contexts: SentryContexts,
    val user: SentryUser
)

@Serializable
data class SentryException(
    val values: List<SentryExceptionValue>
)

@Serializable
data class SentryExceptionValue(
    val type: String,
    val value: String,
    val stacktrace: SentryStacktrace
)

@Serializable
data class SentryStacktrace(
    val frames: List<SentryFrame>
)

@Serializable
data class SentryFrame(
    val lineno: Int,
    val filename: String,
    val function: String
)

@Serializable
data class SentryContexts(
    val app: SentryAppContext,
    val os: SentryOsContext,
    val device: SentryDeviceContext
)

@Serializable
data class SentryAppContext(
    @SerialName("app_identifier") val appIdentifier: String,
    @SerialName("app_name") val appName: String?,
    @SerialName("app_version") val appVersion: String
)

@Serializable
data class SentryOsContext(
    val name: String,
    val version: String,
    val type: String,
    val build: String
)

@Serializable
data class SentryDeviceContext(
    @SerialName("model_id") val modelId: String,
    val model: String,
    val manufacturer: String,
    val type: String,
    val archs: List<String>
)
