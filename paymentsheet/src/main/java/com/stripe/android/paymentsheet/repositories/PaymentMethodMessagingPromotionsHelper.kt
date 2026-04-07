package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
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

internal interface PaymentMethodMessagingPromotionsHelper {
    fun fetchPromotionsAsync(intent: StripeIntent)

    fun getPromotionIfAvailableForCode(code: PaymentMethodCode): PaymentMethodMessagePromotion?
}

@Singleton
internal class DefaultPaymentMethodMessagingPromotionsHelper @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    @IOContext private val workContext: CoroutineContext,
) : PaymentMethodMessagingPromotionsHelper {
    private var promotionsDeferred: Deferred<Result<PaymentMethodMessagePromotionList>>? = null

    override fun fetchPromotionsAsync(intent: StripeIntent) {
        promotionsDeferred = CoroutineScope(workContext).async {
            stripeRepository.retrievePaymentMethodMessageForPaymentSheet(
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

    override fun getPromotionIfAvailableForCode(code: PaymentMethodCode): PaymentMethodMessagePromotion? {
        return promotionsDeferred?.takeIf { it.isCompleted }?.getCompleted()?.getOrNull()?.promotions?.find {
            it.paymentMethodType.lowercase() == code
        }
    }

    fun clear() {
        promotionsDeferred = null
    }
}

@Module
internal interface PaymentMethodMessagingPromotionsHelperModule {
    @Binds
    fun bindsPaymentMethodMessagingPromotionsHelper(
        impl: DefaultPaymentMethodMessagingPromotionsHelper
    ): PaymentMethodMessagingPromotionsHelper
}
