package com.stripe.android.networking

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.BuildConfig
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.utils.ContextUtils.packageInfo
import javax.inject.Provider

/**
 * Factory for [AnalyticsRequest] objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsRequestFactory @VisibleForTesting internal constructor(
    private val packageManager: PackageManager?,
    private val packageInfo: PackageInfo?,
    private val packageName: String,
    private val publishableKeyProvider: Provider<String>,
    internal val defaultProductUsageTokens: Set<String> = emptySet()
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        context: Context,
        publishableKey: String,
        defaultProductUsageTokens: Set<String> = emptySet(),
    ) : this(
        context,
        { publishableKey },
        defaultProductUsageTokens,
    )

    internal constructor(
        context: Context,
        publishableKeyProvider: Provider<String>,
        defaultProductUsageTokens: Set<String> = emptySet(),
    ) : this(
        context.applicationContext.packageManager,
        context.applicationContext.packageInfo,
        context.applicationContext.packageName.orEmpty(),
        publishableKeyProvider,
        defaultProductUsageTokens
    )

    @JvmSynthetic
    internal fun create3ds2Challenge(
        event: AnalyticsEvent,
        uiTypeCode: String?
    ): AnalyticsRequest {
        return createRequest(
            event,
            threeDS2UiType = ThreeDS2UiType.fromUiTypeCode(uiTypeCode)
        )
    }

    @JvmSynthetic
    internal fun createTokenCreation(
        productUsageTokens: Set<String>,
        tokenType: Token.Type,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.TokenCreate,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    @JvmSynthetic
    internal fun createPaymentMethodCreation(
        paymentMethodType: PaymentMethod.Type?,
        productUsageTokens: Set<String>,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.PaymentMethodCreate,
            sourceType = paymentMethodType?.code,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createSourceCreation(
        @Source.SourceType sourceType: String,
        productUsageTokens: Set<String> = emptySet(),
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SourceCreate,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createAddSource(
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerAddSource,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createDeleteSource(
        productUsageTokens: Set<String>,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerDeleteSource,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createAttachPaymentMethod(
        productUsageTokens: Set<String>,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerAttachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createDetachPaymentMethod(
        productUsageTokens: Set<String>
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.CustomerDetachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentConfirmation(
        paymentMethodType: String? = null,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.PaymentIntentConfirm,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentConfirmation(
        paymentMethodType: String?,
    ): AnalyticsRequest {
        return createRequest(
            AnalyticsEvent.SetupIntentConfirm,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    fun createRequest(
        event: String,
        deviceId: String
    ): AnalyticsRequest {
        return AnalyticsRequest(
            createParams(
                event
            ).plus(
                FIELD_DEVICE_ID to deviceId
            )
        )
    }

    @JvmSynthetic
    internal fun createRequest(
        event: AnalyticsEvent,
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        threeDS2UiType: ThreeDS2UiType? = null
    ): AnalyticsRequest {
        return AnalyticsRequest(
            createParams(
                event.toString(),
                productUsageTokens,
                sourceType,
                tokenType,
                threeDS2UiType
            )
        )
    }

    private fun createParams(
        event: String,
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        threeDS2UiType: ThreeDS2UiType? = null
    ): Map<String, Any> {
        return createStandardParams(event)
            .plus(createAppDataParams())
            .plus(
                defaultProductUsageTokens.plus(productUsageTokens)
                    .takeUnless { it.isEmpty() }?.let {
                        mapOf(FIELD_PRODUCT_USAGE to it.toList())
                    }.orEmpty()
            )
            .plus(sourceType?.let { mapOf(FIELD_SOURCE_TYPE to it) }.orEmpty())
            .plus(createTokenTypeParam(sourceType, tokenType))
            .plus(
                threeDS2UiType?.let {
                    mapOf(
                        FIELD_3DS2_UI_TYPE to it.toString()
                    )
                }.orEmpty()
            )
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
                publishableKeyProvider.get()
            }.getOrDefault(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY),
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to DEVICE_TYPE,
            FIELD_BINDINGS_VERSION to Stripe.VERSION_NAME,
            FIELD_IS_DEVELOPMENT to BuildConfig.DEBUG
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

    internal enum class ThreeDS2UiType(
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
        internal const val FIELD_IS_DEVELOPMENT = "is_development"
        internal const val FIELD_DEVICE_ID = "device_id"
        internal const val FIELD_DEVICE_TYPE = "device_type"
        internal const val FIELD_EVENT = "event"
        internal const val FIELD_OS_NAME = "os_name"
        internal const val FIELD_OS_RELEASE = "os_release"
        internal const val FIELD_OS_VERSION = "os_version"
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
    }
}
