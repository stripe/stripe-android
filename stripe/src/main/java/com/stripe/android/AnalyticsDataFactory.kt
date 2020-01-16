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

    internal constructor(context: Context) : this(
        context.applicationContext.packageManager,
        context.applicationContext.packageName
    )

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

    @JvmSynthetic
    internal fun createAuthParams(
        event: AnalyticsEvent,
        intentId: String,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            event,
            publishableKey,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeParams(
        event: AnalyticsEvent,
        intentId: String,
        uiTypeCode: String,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            event,
            publishableKey,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_3DS2_UI_TYPE to get3ds2UiType(uiTypeCode))
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        runtimeErrorEvent: RuntimeErrorEvent,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            publishableKey,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_ERROR_DATA to mapOf(
                    "type" to "runtime_error_event",
                    "error_code" to runtimeErrorEvent.errorCode,
                    "error_message" to runtimeErrorEvent.errorMessage
                ))
        )
    }

    @JvmSynthetic
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

        return createParams(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            publishableKey,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_ERROR_DATA to errorData)
        )
    }

    @JvmSynthetic
    internal fun createTokenCreationParams(
        productUsageTokens: Collection<String>?,
        publishableKey: String,
        @Token.TokenType tokenType: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.TokenCreate,
            publishableKey,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    @JvmSynthetic
    internal fun createPaymentMethodCreationParams(
        publishableKey: String,
        paymentMethodId: String?
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentMethodCreate,
            publishableKey,
            extraParams = paymentMethodId?.let {
                mapOf(FIELD_PAYMENT_METHOD_ID to it)
            }
        )
    }

    @JvmSynthetic
    internal fun createSourceCreationParams(
        publishableKey: String,
        @Source.SourceType sourceType: String,
        productUsageTokens: Collection<String>? = null
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SourceCreate,
            publishableKey,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createAddSourceParams(
        productUsageTokens: Collection<String>? = null,
        publishableKey: String,
        @Source.SourceType sourceType: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerAddSource,
            publishableKey,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createDeleteSourceParams(
        productUsageTokens: Collection<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerDeleteSource,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createAttachPaymentMethodParams(
        productUsageTokens: Collection<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerAttachPaymentMethod,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createDetachPaymentMethodParams(
        productUsageTokens: Collection<String>?,
        publishableKey: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerDetachPaymentMethod,
            publishableKey,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentConfirmationParams(
        publishableKey: String,
        paymentMethodType: String? = null
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentIntentConfirm,
            publishableKey,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentRetrieveParams(
        publishableKey: String,
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentIntentRetrieve,
            publishableKey,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentConfirmationParams(
        publishableKey: String,
        paymentMethodType: String?,
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SetupIntentConfirm,
            publishableKey,
            extraParams = createIntentParam(intentId)
                .plus(
                    paymentMethodType?.let {
                        mapOf(FIELD_PAYMENT_METHOD_TYPE to it)
                    }.orEmpty()
                )
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentRetrieveParams(
        publishableKey: String,
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SetupIntentRetrieve,
            publishableKey,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createParams(
        event: AnalyticsEvent,
        publishableKey: String,
        productUsageTokens: Collection<String>? = null,
        @Source.SourceType sourceType: String? = null,
        @Token.TokenType tokenType: String? = null,
        extraParams: Map<String, Any>? = null
    ): Map<String, Any> {
        return createStandardParams(event, publishableKey)
            .plus(createNameAndVersionParams())
            .plus(
                productUsageTokens?.let {
                    mapOf(FIELD_PRODUCT_USAGE to it.toList())
                }.orEmpty()
            )
            .plus(sourceType?.let { mapOf(FIELD_SOURCE_TYPE to it) }.orEmpty())
            .plus(createTokenTypeParam(sourceType, tokenType))
            .plus(extraParams.orEmpty())
    }

    private fun createTokenTypeParam(
        @Source.SourceType sourceType: String? = null,
        @Token.TokenType tokenType: String? = null
    ): Map<String, String> {
        val value = when {
            tokenType != null -> tokenType
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
        event: AnalyticsEvent,
        publishableKey: String
    ): Map<String, Any> {
        return mapOf(
            FIELD_ANALYTICS_UA to ANALYTICS_UA,
            FIELD_EVENT to event.toString(),
            FIELD_PUBLISHABLE_KEY to publishableKey,
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to DEVICE_TYPE,
            FIELD_BINDINGS_VERSION to BuildConfig.VERSION_NAME
        )
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

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        internal const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"

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

        private fun createIntentParam(intentId: String): Map<String, String> {
            return mapOf(
                FIELD_INTENT_ID to intentId
            )
        }
    }
}
