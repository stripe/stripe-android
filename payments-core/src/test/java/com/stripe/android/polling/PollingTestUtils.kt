package com.stripe.android.polling

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.CoroutineDispatcher

internal fun createIntentStatusPoller(
    enqueuedStatuses: List<StripeIntent.Status>,
    dispatcher: CoroutineDispatcher,
): DefaultIntentStatusPoller {
    return DefaultIntentStatusPoller(
        stripeRepository = FakeStripeRepository(enqueuedStatuses),
        paymentConfigProvider = {
            PaymentConfiguration(
                publishableKey = "key",
                stripeAccountId = "account_id",
            )
        },
        config = IntentStatusPoller.Config(
            clientSecret = "secret",
        ),
        dispatcher = dispatcher,
    )
}

private class FakeStripeRepository(
    enqueuedStatuses: List<StripeIntent.Status>
) : AbsFakeStripeRepository() {

    private val queue = enqueuedStatuses.toMutableList()

    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        val intentStatus = queue.removeAt(0)
        val paymentIntent = PaymentIntentFactory.create(status = intentStatus)
        return Result.success(paymentIntent)
    }
}
