package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
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

    fun reportPromotionDisplayed(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    )
}

@Singleton
internal class DefaultPaymentMethodMessagePromotionsHelper @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
    private val eventReporter: EventReporter
) : PaymentMethodMessagePromotionsHelper {
    private var promotionsDeferred: Deferred<Result<PaymentMethodMessagePromotionList>>? = null

    override fun fetchPromotionsAsync(intent: StripeIntent) {
        eventReporter.onPaymentMethodMessagePromotionsFetchBegin()
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
        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
        ] ?: return null

        if (variant != "treatment") return null

        return if (promotionsDeferred?.isCompleted == true) {
            promotionsDeferred?.getCompleted()?.getOrNull()?.promotions?.find {
                it.paymentMethodType.lowercase() == code
            }
        } else {
            null
        }
    }

    override fun reportPromotionDisplayed(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ) {
        reportPromotionDisplayedInternal(code, metadata, eventReporter)
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return if (promotionsDeferred?.isCompleted == true) {
            promotionsDeferred?.getCompleted()?.getOrNull()?.promotions
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
    private val eventReporter: EventReporter
) : PaymentMethodMessagePromotionsHelper {
    override fun fetchPromotionsAsync(intent: StripeIntent) {
        // NO-OP
    }

    override fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): PaymentMethodMessagePromotion? {
        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
        ] ?: return null

        if (variant != "treatment") return null

        return promotions?.find {
            it.paymentMethodType.lowercase() == code
        }
    }

    override fun reportPromotionDisplayed(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ) {
        reportPromotionDisplayedInternal(code, metadata, eventReporter)
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? = promotions

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

internal class NoOpPromotionsHelper @Inject constructor() : PaymentMethodMessagePromotionsHelper {
    override fun fetchPromotionsAsync(intent: StripeIntent) {
        // NO-OP
    }

    override fun getPromotionIfAvailableForCode(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ): PaymentMethodMessagePromotion? {
        return null
    }

    override fun reportPromotionDisplayed(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata
    ) {
        // NO-OP
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

    return if (variant == "treatment") {
        { getPromotionIfAvailableForCode(code, metadata) }
    } else {
        null
    }
}

private fun PaymentMethodMessagePromotionsHelper.reportPromotionDisplayedInternal(
    code: PaymentMethodCode,
    metadata: PaymentMethodMetadata,
    eventReporter: EventReporter
) {
    if (!PromotionSupportedPaymentMethods.supportedPaymentMethods.contains(code)) return

    val variant = metadata.experimentsData?.experimentAssignments[
        ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
    ] ?: return

    if (variant != "treatment") return

    val displayedSuccessfully = getPromotionIfAvailableForCode(code, metadata) != null
    eventReporter.onPaymentMethodMessagePromotionDisplayed(displayedSuccessfully)
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
