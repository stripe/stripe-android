package com.stripe.android.networking

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

/**
 * Factory for [AnalyticsRequest] objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentAnalyticsRequestFactory @VisibleForTesting internal constructor(
    packageManager: PackageManager?,
    packageInfo: PackageInfo?,
    packageName: String,
    publishableKeyProvider: Provider<String>,
    networkTypeProvider: Provider<String?>,
    internal val defaultProductUsageTokens: Set<String> = emptySet(),
) : AnalyticsRequestFactory(
    packageManager,
    packageInfo,
    packageName,
    publishableKeyProvider,
    networkTypeProvider,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        context: Context,
        publishableKey: String,
        defaultProductUsageTokens: Set<String> = emptySet()
    ) : this(
        context,
        { publishableKey },
        defaultProductUsageTokens
    )

    internal constructor(
        context: Context,
        publishableKeyProvider: Provider<String>
    ) : this(
        packageManager = context.applicationContext.packageManager,
        packageInfo = context.applicationContext.packageInfo,
        packageName = context.applicationContext.packageName.orEmpty(),
        publishableKeyProvider = publishableKeyProvider,
        networkTypeProvider = NetworkTypeDetector(context)::invoke,
    )

    @Inject
    internal constructor(
        context: Context,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(PRODUCT_USAGE) defaultProductUsageTokens: Set<String>
    ) : this(
        packageManager = context.applicationContext.packageManager,
        packageInfo = context.applicationContext.packageInfo,
        packageName = context.applicationContext.packageName.orEmpty(),
        publishableKeyProvider = publishableKeyProvider,
        networkTypeProvider = NetworkTypeDetector(context)::invoke,
        defaultProductUsageTokens = defaultProductUsageTokens,
    )

    @JvmSynthetic
    internal fun create3ds2Challenge(
        event: PaymentAnalyticsEvent,
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
        tokenType: Token.Type
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.TokenCreate,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    @JvmSynthetic
    internal fun createPaymentMethodCreation(
        paymentMethodCode: PaymentMethodCode?,
        productUsageTokens: Set<String>
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.PaymentMethodCreate,
            sourceType = paymentMethodCode,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createSourceCreation(
        @Source.SourceType sourceType: String,
        productUsageTokens: Set<String> = emptySet()
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.SourceCreate,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createAddSource(
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.CustomerAddSource,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createDeleteSource(
        productUsageTokens: Set<String>
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.CustomerDeleteSource,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createAttachPaymentMethod(
        productUsageTokens: Set<String>
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.CustomerAttachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createDetachPaymentMethod(
        productUsageTokens: Set<String>
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.CustomerDetachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentConfirmation(
        paymentMethodType: String? = null
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.PaymentIntentConfirm,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentConfirmation(
        paymentMethodType: String?
    ): AnalyticsRequest {
        return createRequest(
            PaymentAnalyticsEvent.SetupIntentConfirm,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createRequest(
        event: PaymentAnalyticsEvent,
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        threeDS2UiType: ThreeDS2UiType? = null
    ): AnalyticsRequest {
        return createRequest(
            event,
            additionalParams(
                productUsageTokens = productUsageTokens,
                sourceType = sourceType,
                tokenType = tokenType,
                threeDS2UiType = threeDS2UiType
            )
        )
    }

    private fun additionalParams(
        productUsageTokens: Set<String> = emptySet(),
        @Source.SourceType sourceType: String? = null,
        tokenType: Token.Type? = null,
        threeDS2UiType: ThreeDS2UiType? = null
    ): Map<String, Any> {
        return defaultProductUsageTokens
            .plus(productUsageTokens)
            .takeUnless { it.isEmpty() }?.let { mapOf(FIELD_PRODUCT_USAGE to it.toList()) }
            .orEmpty()
            .plus(sourceType?.let { mapOf(FIELD_SOURCE_TYPE to it) }.orEmpty())
            .plus(createTokenTypeParam(sourceType, tokenType))
            .plus(threeDS2UiType?.let { mapOf(FIELD_3DS2_UI_TYPE to it.toString()) }.orEmpty())
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

        @Keep
        override fun toString(): String = typeName

        companion object {
            fun fromUiTypeCode(uiTypeCode: String?) = values().firstOrNull {
                it.code == uiTypeCode
            } ?: None
        }
    }

    internal companion object {
        internal const val FIELD_TOKEN_TYPE = "token_type"
        internal const val FIELD_PRODUCT_USAGE = "product_usage"
        internal const val FIELD_SOURCE_TYPE = "source_type"
        internal const val FIELD_3DS2_UI_TYPE = "3ds2_ui_type"
    }
}
