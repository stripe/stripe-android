package com.stripe.android.networking

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.paymentsheet.analytics.DeviceId
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.utils.ContextUtils.packageInfo

/**
 * Factory for [AnalyticsRequest] objects.
 */
internal class AnalyticsRequestFactory @VisibleForTesting internal constructor(
    private val packageManager: PackageManager?,
    private val packageInfo: PackageInfo?,
    private val packageName: String,
    private val publishableKeySupplier: () -> String
) {
    internal constructor(
        context: Context,
        publishableKey: String
    ) : this(
        context,
        { publishableKey }
    )

    internal constructor(
        context: Context,
        publishableKeySupplier: () -> String
    ) : this(
        context.applicationContext.packageManager,
        context.applicationContext.packageInfo,
        context.applicationContext.packageName.orEmpty(),
        publishableKeySupplier
    )

    @JvmSynthetic
    internal fun createAuth(
        event: AnalyticsEvent,
        intentId: String,
        requestId: RequestId? = null,
        extraParams: Map<String, Any> = emptyMap()
    ): AnalyticsRequest {
        return createRequest(
            event,
            requestId = requestId,
            extraParams = extraParams.plus(
                createIntentParam(intentId)
            )
        )
    }

    @JvmSynthetic
    internal fun createAuthSource(
        event: AnalyticsEvent,
        sourceId: String?
    ): AnalyticsRequest {
        return createRequest(
            event,
            requestId = null,
            extraParams = sourceId?.let { mapOf(FIELD_SOURCE_ID to it) }
        )
    }

    @JvmSynthetic
    internal fun create3ds2Challenge(
        event: AnalyticsEvent,
        intentId: String,
        uiTypeCode: String
    ): AnalyticsRequest {
        return createRequest(
            event,
            requestId = null,
            extraParams = createIntentParam(intentId)
                .plus(
                    FIELD_3DS2_UI_TYPE to
                        ThreeDS2UiType.fromUiTypeCode(uiTypeCode).toString()
                )
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeError(
        intentId: String,
        runtimeErrorEvent: RuntimeErrorEvent
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            requestId = null,
            extraParams = createIntentParam(intentId)
                .plus(
                    FIELD_ERROR_DATA to mapOf(
                        "type" to "runtime_error_event",
                        "error_code" to runtimeErrorEvent.errorCode,
                        "error_message" to runtimeErrorEvent.errorMessage
                    )
                )
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        protocolErrorEvent: ProtocolErrorEvent
    ): AnalyticsRequest {
        val errorMessage = protocolErrorEvent.errorMessage
        val errorData = mapOf(
            "type" to "protocol_error_event",
            "sdk_trans_id" to protocolErrorEvent.sdkTransactionId?.value,
            "error_code" to errorMessage.errorCode,
            "error_description" to errorMessage.errorDescription,
            "error_details" to errorMessage.errorDetails,
            "trans_id" to errorMessage.transactionId
        )

        return createRequest(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            requestId = null,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_ERROR_DATA to errorData)
        )
    }

    @JvmSynthetic
    internal fun createTokenCreation(
        productUsageTokens: Set<String>?,
        tokenType: Token.Type,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.TokenCreate,
            requestId = requestId,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    @JvmSynthetic
    internal fun createPaymentMethodCreation(
        paymentMethodType: PaymentMethodCreateParams.Type?,
        productUsageTokens: Set<String>?,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.PaymentMethodCreate,
            requestId = requestId,
            sourceType = paymentMethodType?.code,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createSourceCreation(
        @Source.SourceType sourceType: String,
        productUsageTokens: Set<String>? = null,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SourceCreate,
            requestId = requestId,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createSourceRetrieve(
        sourceId: String,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SourceRetrieve,
            requestId = requestId,
            extraParams = mapOf(FIELD_SOURCE_ID to sourceId)
        )
    }

    @JvmSynthetic
    internal fun createAddSource(
        productUsageTokens: Set<String>? = null,
        @Source.SourceType sourceType: String,
        requestId: RequestId?,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerAddSource,
            requestId = requestId,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createDeleteSource(
        productUsageTokens: Set<String>?,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerDeleteSource,
            requestId = requestId,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createAttachPaymentMethod(
        productUsageTokens: Set<String>?,
        requestId: RequestId?,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerAttachPaymentMethod,
            requestId = requestId,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createDetachPaymentMethod(
        productUsageTokens: Set<String>?,
        requestId: RequestId?,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerDetachPaymentMethod,
            requestId = requestId,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentConfirmation(
        paymentMethodType: String? = null,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.PaymentIntentConfirm,
            requestId = requestId,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentRetrieve(
        intentId: String,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.PaymentIntentRetrieve,
            requestId = requestId,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentConfirmation(
        paymentMethodType: String?,
        intentId: String,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SetupIntentConfirm,
            requestId = requestId,
            sourceType = paymentMethodType,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentRetrieveParams(
        intentId: String,
        requestId: RequestId?
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SetupIntentRetrieve,
            requestId = requestId,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createRequest(
        event: AnalyticsEvent
    ) = createRequest(
        event,
        requestId = null
    )

    @JvmSynthetic
    internal fun createRequest(
        event: PaymentSheetEvent,
        sessionId: SessionId?,
        deviceId: DeviceId
    ): AnalyticsRequest {
        return AnalyticsRequest(
            createParams(
                event.toString(),
                requestId = null
            ).plus(
                FIELD_DEVICE_ID to deviceId.value
            ).plus(
                sessionId?.let {
                    mapOf(FIELD_SESSION_ID to sessionId.value)
                }.orEmpty()
            )
        )
    }

    @JvmSynthetic
    internal fun createRequest(
        event: AnalyticsEvent,
        requestId: RequestId? = null,
        productUsageTokens: Set<String>? = null,
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        extraParams: Map<String, Any>? = null
    ): AnalyticsRequest {
        return AnalyticsRequest(
            createParams(
                event.toString(),
                requestId,
                productUsageTokens,
                sourceType,
                tokenType,
                extraParams
            )
        )
    }

    private fun createParams(
        event: String,
        requestId: RequestId?,
        productUsageTokens: Set<String>? = null,
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        extraParams: Map<String, Any>? = null
    ): Map<String, Any> {
        return createStandardParams(event)
            .plus(createAppDataParams())
            .plus(
                productUsageTokens.takeUnless { it.isNullOrEmpty() }?.let {
                    mapOf(FIELD_PRODUCT_USAGE to it.toList())
                }.orEmpty()
            )
            .plus(sourceType?.let { mapOf(FIELD_SOURCE_TYPE to it) }.orEmpty())
            .plus(createTokenTypeParam(sourceType, tokenType))
            .plus(
                requestId?.let {
                    mapOf(FIELD_REQUEST_ID to it.value)
                }.orEmpty()
            )
            .plus(extraParams.orEmpty())
    }

    private fun createTokenTypeParam(
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null
    ): Map<String, String> {
        val value = when {
            tokenType != null -> tokenType.code
            // This is not a source event, so to match iOS we log a token without type
            // as type "unknown"
            sourceType == null -> "unknown"
            else -> null
        }

        return value?.let {
            mapOf(FIELD_TOKEN_TYPE to it)
        }.orEmpty()
    }

    private fun createStandardParams(
        event: String
    ): Map<String, Any> {
        return mapOf(
            FIELD_ANALYTICS_UA to ANALYTICS_UA,
            FIELD_EVENT to event,
            FIELD_PUBLISHABLE_KEY to runCatching {
                publishableKeySupplier()
            }.getOrDefault(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY),
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to DEVICE_TYPE,
            FIELD_BINDINGS_VERSION to Stripe.VERSION_NAME
        )
    }

    internal fun createAppDataParams(): Map<String, Any> {
        return when {
            packageManager != null && packageInfo != null -> {
                mapOf(
                    FIELD_APP_NAME to getAppName(packageInfo, packageManager),
                    FIELD_APP_VERSION to packageInfo.versionCode
                )
            }
            else -> emptyMap()
        }
    }

    private fun getAppName(
        packageInfo: PackageInfo?,
        packageManager: PackageManager
    ): CharSequence {
        return packageInfo?.applicationInfo?.loadLabel(packageManager).takeUnless {
            it.isNullOrBlank()
        } ?: packageName
    }

    private enum class ThreeDS2UiType(
        private val code: String?,
        private val typeName: String
    ) {
        None(null, "none"),
        Text("01", "text"),
        SingleSelect("02", "single_select"),
        MultiSelect("03", "multi_select"),
        Oob("04", "oob"),
        Html("05", "html");

        override fun toString(): String = typeName

        companion object {
            fun fromUiTypeCode(uiTypeCode: String?) = values().firstOrNull {
                it.code == uiTypeCode
            } ?: None
        }
    }

    internal companion object {
        internal const val FIELD_PRODUCT_USAGE = "product_usage"
        internal const val FIELD_ANALYTICS_UA = "analytics_ua"
        internal const val FIELD_APP_NAME = "app_name"
        internal const val FIELD_APP_VERSION = "app_version"
        internal const val FIELD_BINDINGS_VERSION = "bindings_version"
        internal const val FIELD_DEVICE_ID = "device_id"
        internal const val FIELD_DEVICE_TYPE = "device_type"
        internal const val FIELD_EVENT = "event"
        internal const val FIELD_ERROR_DATA = "error"
        internal const val FIELD_INTENT_ID = "intent_id"
        internal const val FIELD_OS_NAME = "os_name"
        internal const val FIELD_OS_RELEASE = "os_release"
        internal const val FIELD_OS_VERSION = "os_version"
        internal const val FIELD_PUBLISHABLE_KEY = "publishable_key"
        internal const val FIELD_REQUEST_ID = "request_id"
        internal const val FIELD_SESSION_ID = "session_id"
        internal const val FIELD_SOURCE_ID = "source_id"
        internal const val FIELD_SOURCE_TYPE = "source_type"
        internal const val FIELD_3DS2_UI_TYPE = "3ds2_ui_type"
        internal const val FIELD_TOKEN_TYPE = "token_type"

        @JvmSynthetic
        internal val VALID_PARAM_FIELDS: Set<String> = setOf(
            FIELD_ANALYTICS_UA, FIELD_APP_NAME, FIELD_APP_VERSION, FIELD_BINDINGS_VERSION,
            FIELD_DEVICE_TYPE, FIELD_EVENT, FIELD_OS_VERSION, FIELD_OS_NAME, FIELD_OS_RELEASE,
            FIELD_PRODUCT_USAGE, FIELD_PUBLISHABLE_KEY, FIELD_REQUEST_ID,
            FIELD_SOURCE_TYPE, FIELD_TOKEN_TYPE
        )

        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        internal const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"

        private fun createIntentParam(intentId: String): Map<String, String> {
            return mapOf(
                FIELD_INTENT_ID to intentId
            )
        }
    }
}
