package com.stripe.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import java.util.HashMap

/**
 * Util class to create logging items, which are fed as [Map][java.util.Map] objects in
 * query parameters to our server.
 */
internal class AnalyticsDataFactory @VisibleForTesting internal constructor(
    private val packageManager: PackageManager?,
    private val packageName: String?
) {
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(EventName.TOKEN_CREATION, EventName.CREATE_PAYMENT_METHOD,
        EventName.ATTACH_PAYMENT_METHOD, EventName.DETACH_PAYMENT_METHOD, EventName.SOURCE_CREATION,
        EventName.ADD_SOURCE, EventName.DEFAULT_SOURCE, EventName.DELETE_SOURCE,
        EventName.SET_SHIPPING_INFO, EventName.CONFIRM_PAYMENT_INTENT,
        EventName.RETRIEVE_PAYMENT_INTENT, EventName.CONFIRM_SETUP_INTENT,
        EventName.RETRIEVE_SETUP_INTENT, EventName.AUTH_3DS1_SDK,
        EventName.AUTH_3DS2_FINGERPRINT, EventName.AUTH_3DS2_START,
        EventName.AUTH_3DS2_FRICTIONLESS, EventName.AUTH_3DS2_CHALLENGE_PRESENTED,
        EventName.AUTH_3DS2_CHALLENGE_CANCELED, EventName.AUTH_3DS2_CHALLENGE_COMPLETED,
        EventName.AUTH_3DS2_CHALLENGE_ERRORED, EventName.AUTH_3DS2_CHALLENGE_TIMEDOUT,
        EventName.AUTH_3DS2_FALLBACK, EventName.AUTH_REDIRECT, EventName.AUTH_ERROR)
    internal annotation class EventName {
        companion object {
            internal const val TOKEN_CREATION = "token_creation"
            internal const val CREATE_PAYMENT_METHOD = "payment_method_creation"
            internal const val ATTACH_PAYMENT_METHOD = "attach_payment_method"
            internal const val DETACH_PAYMENT_METHOD = "detach_payment_method"
            internal const val SOURCE_CREATION = "source_creation"
            internal const val ADD_SOURCE = "add_source"
            internal const val DEFAULT_SOURCE = "default_source"
            internal const val DELETE_SOURCE = "delete_source"
            internal const val SET_SHIPPING_INFO = "set_shipping_info"
            internal const val CONFIRM_PAYMENT_INTENT = "payment_intent_confirmation"
            internal const val RETRIEVE_PAYMENT_INTENT = "payment_intent_retrieval"
            internal const val CONFIRM_SETUP_INTENT = "setup_intent_confirmation"
            internal const val RETRIEVE_SETUP_INTENT = "setup_intent_retrieval"

            internal const val AUTH_3DS1_SDK = "3ds1_sdk"

            internal const val AUTH_3DS2_FINGERPRINT = "3ds2_fingerprint"
            internal const val AUTH_3DS2_START = "3ds2_authenticate"
            internal const val AUTH_3DS2_FRICTIONLESS = "3ds2_frictionless_flow"
            internal const val AUTH_3DS2_CHALLENGE_PRESENTED = "3ds2_challenge_flow_presented"
            internal const val AUTH_3DS2_CHALLENGE_CANCELED = "3ds2_challenge_flow_canceled"
            internal const val AUTH_3DS2_CHALLENGE_COMPLETED = "3ds2_challenge_flow_completed"
            internal const val AUTH_3DS2_CHALLENGE_ERRORED = "3ds2_challenge_flow_errored"
            internal const val AUTH_3DS2_CHALLENGE_TIMEDOUT = "3ds2_challenge_flow_timed_out"
            internal const val AUTH_3DS2_FALLBACK = "3ds2_fallback"

            internal const val AUTH_REDIRECT = "url_redirect_next_action"
            internal const val AUTH_ERROR = "auth_error"
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ThreeDS2UiType.NONE, ThreeDS2UiType.TEXT, ThreeDS2UiType.SINGLE_SELECT,
        ThreeDS2UiType.MULTI_SELECT, ThreeDS2UiType.OOB, ThreeDS2UiType.HTML)
    private annotation class ThreeDS2UiType {
        companion object {
            const val NONE = "none"
            const val TEXT = "text"
            const val SINGLE_SELECT = "single_select"
            const val MULTI_SELECT = "multi_select"
            const val OOB = "oob"
            const val HTML = "html"
        }
    }

    internal fun createAuthParams(
        @EventName eventName: String,
        intentId: String,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(eventName, publishableKey)
            .plus(FIELD_INTENT_ID to intentId)
    }

    internal fun create3ds2ChallengeParams(
        @EventName eventName: String,
        intentId: String,
        uiTypeCode: String,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(eventName, publishableKey)
            .plus(FIELD_INTENT_ID to intentId)
            .plus(FIELD_3DS2_UI_TYPE to get3ds2UiType(uiTypeCode))
    }

    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        runtimeErrorEvent: RuntimeErrorEvent,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.AUTH_3DS2_CHALLENGE_ERRORED,
            publishableKey
        )
            .plus(FIELD_INTENT_ID to intentId)
            .plus(FIELD_ERROR_DATA to
                mapOf(
                    "type" to "runtime_error_event",
                    "error_code" to runtimeErrorEvent.errorCode,
                    "error_message" to runtimeErrorEvent.errorMessage
                ))
    }

    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        protocolErrorEvent: ProtocolErrorEvent,
        publishableKey: String
    ): Map<String, Any> {
        val errorMessage = protocolErrorEvent.errorMessage
        val errorData = mapOf(
            "type" to "protocol_error_event",
            "sdk_trans_id" to protocolErrorEvent.sdkTransactionId,
            "error_code" to errorMessage.errorCode,
            "error_description" to errorMessage.errorDescription,
            "error_details" to errorMessage.errorDetails,
            "trans_id" to errorMessage.transactionId
        )

        return getEventLoggingParams(
            EventName.AUTH_3DS2_CHALLENGE_ERRORED,
            publishableKey
        )
            .plus(FIELD_INTENT_ID to intentId)
            .plus(FIELD_ERROR_DATA to errorData)
    }

    internal fun getTokenCreationParams(
        productUsageTokens: List<String>?,
        publishableKey: String,
        tokenType: String?
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.TOKEN_CREATION,
            publishableKey,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    internal fun createPaymentMethodCreationParams(
        publishableKey: String,
        paymentMethodId: String?
    ): Map<String, Any> {
        val params =
            getEventLoggingParams(
                EventName.CREATE_PAYMENT_METHOD,
                publishableKey
            )
        return if (paymentMethodId != null) {
            params.plus(FIELD_PAYMENT_METHOD_ID to paymentMethodId)
        } else {
            params
        }
    }

    internal fun getSourceCreationParams(
        productUsageTokens: List<String>?,
        publishableKey: String,
        @Source.SourceType sourceType: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.SOURCE_CREATION,
            publishableKey,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    internal fun getAddSourceParams(
        productUsageTokens: List<String>?,
        publishableKey: String,
        @Source.SourceType sourceType: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.ADD_SOURCE,
            publishableKey,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    internal fun getDeleteSourceParams(
        productUsageTokens: List<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.DELETE_SOURCE,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    internal fun getAttachPaymentMethodParams(
        productUsageTokens: List<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.ATTACH_PAYMENT_METHOD,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    internal fun getDetachPaymentMethodParams(
        productUsageTokens: List<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.DETACH_PAYMENT_METHOD,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    internal fun getPaymentIntentConfirmationParams(
        productUsageTokens: List<String>?,
        publishableKey: String,
        paymentMethodType: String?
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.CONFIRM_PAYMENT_INTENT,
            publishableKey,
            productUsageTokens = productUsageTokens,
            sourceType = paymentMethodType
        )
    }

    internal fun getPaymentIntentRetrieveParams(
        productUsageTokens: List<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return getEventLoggingParams(
            EventName.RETRIEVE_PAYMENT_INTENT,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    internal fun getSetupIntentConfirmationParams(
        publishableKey: String,
        paymentMethodType: String?
    ): Map<String, Any> {
        val params =
            getEventLoggingParams(EventName.CONFIRM_SETUP_INTENT, publishableKey)
        return if (paymentMethodType != null) {
            params.plus(FIELD_PAYMENT_METHOD_TYPE to paymentMethodType)
        } else {
            params
        }
    }

    internal fun getSetupIntentRetrieveParams(publishableKey: String): Map<String, Any> {
        return getEventLoggingParams(EventName.RETRIEVE_SETUP_INTENT, publishableKey)
    }

    internal fun getEventLoggingParams(
        @EventName eventName: String,
        publishableKey: String,
        productUsageTokens: List<String>? = null,
        @Source.SourceType sourceType: String? = null,
        @Token.TokenType tokenType: String? = null
    ): Map<String, Any> {
        val paramsObject = mapOf(
            FIELD_ANALYTICS_UA to analyticsUa,
            FIELD_EVENT to getEventParamName(eventName),
            FIELD_PUBLISHABLE_KEY to publishableKey,
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to deviceLoggingString,
            FIELD_BINDINGS_VERSION to BuildConfig.VERSION_NAME
        )
            .plus(createNameAndVersionParams())
            .toMutableMap()

        if (productUsageTokens != null) {
            paramsObject[FIELD_PRODUCT_USAGE] = productUsageTokens
        }

        if (sourceType != null) {
            paramsObject[FIELD_SOURCE_TYPE] = sourceType
        }

        if (tokenType != null) {
            paramsObject[FIELD_TOKEN_TYPE] = tokenType
        } else if (sourceType == null) {
            // This is not a source event, so to match iOS we log a token without type
            // as type "unknown"
            paramsObject[FIELD_TOKEN_TYPE] = "unknown"
        }

        return paramsObject
    }

    internal fun createNameAndVersionParams(): Map<String, Any> {
        return if (packageManager != null) {
            try {
                createNameAndVersionParams(packageManager)
            } catch (nameNotFound: PackageManager.NameNotFoundException) {
                mapOf(
                    FIELD_APP_NAME to UNKNOWN,
                    FIELD_APP_VERSION to UNKNOWN
                )
            }
        } else {
            mapOf(
                FIELD_APP_NAME to NO_CONTEXT,
                FIELD_APP_VERSION to NO_CONTEXT
            )
        }
    }

    private fun createNameAndVersionParams(packageManager: PackageManager): Map<String, Any> {
        val paramsObject = HashMap<String, Any>(2)
        val info = packageManager.getPackageInfo(packageName, 0)

        val nameString: String?
        if (info.applicationInfo != null) {
            val name = info.applicationInfo.loadLabel(packageManager)
            nameString = name.toString()
            paramsObject[FIELD_APP_NAME] = nameString
        } else {
            nameString = null
        }

        if (nameString.isNullOrBlank()) {
            paramsObject[FIELD_APP_NAME] = info.packageName
        }

        return paramsObject
            .plus(FIELD_APP_VERSION to info.versionCode)
    }

    internal companion object {
        internal const val UNKNOWN = "unknown"
        internal const val NO_CONTEXT = "no_context"

        internal const val FIELD_PRODUCT_USAGE = "product_usage"
        internal const val FIELD_ANALYTICS_UA = "analytics_ua"
        internal const val FIELD_APP_NAME = "app_name"
        internal const val FIELD_APP_VERSION = "app_version"
        internal const val FIELD_BINDINGS_VERSION = "bindings_version"
        internal const val FIELD_DEVICE_TYPE = "device_type"
        internal const val FIELD_EVENT = "event"
        internal const val FIELD_ERROR_DATA = "error"
        internal const val FIELD_INTENT_ID = "intent_id"
        internal const val FIELD_OS_NAME = "os_name"
        internal const val FIELD_OS_RELEASE = "os_release"
        internal const val FIELD_OS_VERSION = "os_version"
        internal const val FIELD_PAYMENT_METHOD_ID = "payment_method_id"
        internal const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        internal const val FIELD_PUBLISHABLE_KEY = "publishable_key"
        internal const val FIELD_SOURCE_TYPE = "source_type"
        internal const val FIELD_3DS2_UI_TYPE = "3ds2_ui_type"
        internal const val FIELD_TOKEN_TYPE = "token_type"

        @JvmSynthetic
        internal val VALID_PARAM_FIELDS: Set<String> = setOf(
            FIELD_ANALYTICS_UA, FIELD_APP_NAME, FIELD_APP_VERSION, FIELD_BINDINGS_VERSION,
            FIELD_DEVICE_TYPE, FIELD_EVENT, FIELD_OS_VERSION, FIELD_OS_NAME, FIELD_OS_RELEASE,
            FIELD_PRODUCT_USAGE, FIELD_PUBLISHABLE_KEY, FIELD_SOURCE_TYPE, FIELD_TOKEN_TYPE
        )

        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val deviceLoggingString: String
            get() = Build.MANUFACTURER + '_'.toString() + Build.BRAND + '_'.toString() + Build.MODEL

        internal val analyticsUa: String
            @JvmSynthetic
            get() = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"

        @JvmSynthetic
        internal fun getEventParamName(@EventName eventName: String): String {
            return "$ANALYTICS_NAME.$eventName"
        }

        @ThreeDS2UiType
        private fun get3ds2UiType(uiTypeCode: String): String {
            return when (uiTypeCode) {
                "01" -> ThreeDS2UiType.TEXT
                "02" -> ThreeDS2UiType.SINGLE_SELECT
                "03" -> ThreeDS2UiType.MULTI_SELECT
                "04" -> ThreeDS2UiType.OOB
                "05" -> ThreeDS2UiType.HTML
                else -> ThreeDS2UiType.NONE
            }
        }

        @JvmSynthetic
        internal fun create(context: Context): AnalyticsDataFactory {
            return AnalyticsDataFactory(
                context.applicationContext.packageManager,
                context.applicationContext.packageName
            )
        }
    }
}
