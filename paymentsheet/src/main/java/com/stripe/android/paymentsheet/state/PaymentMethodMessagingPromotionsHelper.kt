package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface PaymentMethodMessagePromotionsHelper {
    fun fetchPromotionsAsync(intent: StripeIntent)

    fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): PaymentMethodMessagePromotion?

    fun getPromotions(): List<PaymentMethodMessagePromotion>?

    fun getPromotionProvider(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): (() -> PaymentMethodMessagePromotion?)?
}

@Singleton
internal class DefaultPaymentMethodMessagePromotionsHelper @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : PaymentMethodMessagePromotionsHelper {
    private var promotionsDeferred: Deferred<Result<PaymentMethodMessagePromotionList>>? = null

    override fun fetchPromotionsAsync(intent: StripeIntent) {
        if (!FeatureFlags.paymentMethodMessagePromotions.isEnabled) return

        promotionsDeferred?.cancel()
        promotionsDeferred = null
        promotionsDeferred = viewModelScope.async(workContext) {
            stripeRepository.retrievePaymentMethodMessagePromotionsForPaymentSheet(
                amount = intent.amount?.toInt() ?: 0,
                currency = intent.currency ?: "usd",
                country = intent.countryCode,
                locale = Locale.getDefault().language,
                requestOptions = ApiRequest.Options(
                    apiKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccount = lazyPaymentConfig.get().stripeAccountId
                )
            )
        }
    }

    override fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
    ): PaymentMethodMessagePromotion? {
        return if (FeatureFlags.paymentMethodMessagePromotions.isEnabled) {
            val variant = metadata.experimentsData?.experimentAssignments[
                ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
            ] ?: return null

            val promotion = if (variant == "treatment") {
                if (promotionsDeferred?.isCompleted == true) {
                    promotionsDeferred?.getCompleted()?.getOrNull()?.promotions?.find {
                        it.paymentMethodType.lowercase() == code
                    }
                } else {
                    null
                }
            } else {
                null
            }

            promotion
        } else {
            null
        }
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return if (FeatureFlags.paymentMethodMessagePromotions.isEnabled) {
            if (promotionsDeferred?.isCompleted == true) {
                promotionsDeferred?.getCompleted()?.getOrNull()?.promotions
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun getPromotionProvider(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): (() -> PaymentMethodMessagePromotion?)? {
        return getPromotionProviderInternal(
            code = code,
            metadata = metadata,
        )
    }
}

internal class PrefetchedPaymentMethodMessagePromotionsHelper(
    private val promotions: List<PaymentMethodMessagePromotion>?,
) : PaymentMethodMessagePromotionsHelper {
    override fun fetchPromotionsAsync(intent: StripeIntent) {
        // NO-OP
    }

    override fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
    ): PaymentMethodMessagePromotion? {
        return if (FeatureFlags.paymentMethodMessagePromotions.isEnabled) {
            val variant = metadata.experimentsData?.experimentAssignments[
                ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
            ]

            val promotion = if (variant == "treatment") {
                promotions?.find {
                    it.paymentMethodType.lowercase() == code
                }
            } else {
                null
            }

            promotion
        } else {
            null
        }
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return if (FeatureFlags.paymentMethodMessagePromotions.isEnabled) {
            promotions
        } else {
            null
        }
    }

    override fun getPromotionProvider(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): (() -> PaymentMethodMessagePromotion?)? {
        return getPromotionProviderInternal(code, metadata)
    }
}

internal class NoOpPromotionsHelper @Inject constructor() : PaymentMethodMessagePromotionsHelper {
    override fun fetchPromotionsAsync(intent: StripeIntent) {
        // NO-OP
    }

    override fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
    ): PaymentMethodMessagePromotion? {
        return null
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return null
    }

    override fun getPromotionProvider(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): (() -> PaymentMethodMessagePromotion?)? {
        return null
    }
}

internal object PromotionSupportedPaymentMethods {
    val supportedPaymentMethods = setOf(
        PaymentMethod.Type.Klarna.code,
        PaymentMethod.Type.Affirm.code,
        PaymentMethod.Type.AfterpayClearpay.code
    )
}

private fun PaymentMethodMessagePromotionsHelper.getPromotionProviderInternal(
    code: PaymentMethodCode,
    metadata: PaymentMethodMetadata,
): (() -> PaymentMethodMessagePromotion?)? {
    if (!PromotionSupportedPaymentMethods.supportedPaymentMethods.contains(code)) return null

    val variant = metadata.experimentsData?.experimentAssignments[
        ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
    ]

    return if (FeatureFlags.paymentMethodMessagePromotions.isEnabled && variant == "treatment") {
        { getPromotionIfAvailableForCode(code, metadata) }
    } else {
        null
    }
}

@Module
internal interface PaymentMethodMessagePromotionsHelperModule {
    @Binds
    fun bindsPromotionsHelper(
        impl: DefaultPaymentMethodMessagePromotionsHelper
    ): PaymentMethodMessagePromotionsHelper
}

@Module
internal interface NoOpPaymentMethodMessagingPromotionHelperModule {
    @Binds
    fun bindsPromotionsHelper(impl: NoOpPromotionsHelper): PaymentMethodMessagePromotionsHelper
}
