package com.stripe.android.payments.samsungpay

import android.content.Context
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Internal abstraction for Stripe API calls needed by Samsung Pay.
 * This exists because [com.stripe.android.networking.StripeApiRepository] has an
 * internal constructor and cannot be instantiated from external modules.
 */
internal interface StripeIntentRepository {
    suspend fun retrievePaymentIntent(clientSecret: String): PaymentIntent
    suspend fun confirmPaymentIntent(params: ConfirmPaymentIntentParams): PaymentIntent
    suspend fun confirmSetupIntent(params: ConfirmSetupIntentParams): SetupIntent
}

internal class DefaultStripeIntentRepository(
    context: Context,
) : StripeIntentRepository {

    private val stripe = Stripe(
        context = context,
        publishableKey = com.stripe.android.PaymentConfiguration.getInstance(context).publishableKey,
        stripeAccountId = com.stripe.android.PaymentConfiguration.getInstance(context).stripeAccountId,
    )

    override suspend fun retrievePaymentIntent(clientSecret: String): PaymentIntent {
        return withContext(Dispatchers.IO) {
            stripe.retrievePaymentIntentSynchronous(clientSecret)
        }
    }

    override suspend fun confirmPaymentIntent(params: ConfirmPaymentIntentParams): PaymentIntent {
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            stripe.confirmPaymentIntentSynchronous(params)
        }
    }

    override suspend fun confirmSetupIntent(params: ConfirmSetupIntentParams): SetupIntent {
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            stripe.confirmSetupIntentSynchronous(params)
        }
    }
}
