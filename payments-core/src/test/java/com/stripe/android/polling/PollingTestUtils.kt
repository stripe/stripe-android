package com.stripe.android.polling

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal fun exponentialDelayProvider(): () -> Long {
    var attempt = 1
    return {
        // Add a minor offset so that we run after the delay has finished,
        // not when it's just about to finish.
        val offset = if (attempt == 1) 1 else 0
        calculateDelay(attempts = attempt++).inWholeMilliseconds + offset
    }
}

internal fun createIntentStatusPoller(
    enqueuedStatuses: List<StripeIntent.Status?>,
    dispatcher: CoroutineDispatcher,
    maxAttempts: Int = 10,
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
            maxAttempts = maxAttempts,
        ),
        dispatcher = dispatcher,
    )
}

private class FakeStripeRepository(
    enqueuedStatuses: List<StripeIntent.Status?>
) : AbsFakeStripeRepository() {

    private val queue = enqueuedStatuses.toMutableList()

    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent {
        val intentStatus = queue.removeFirst()
        return mock {
            on { status } doReturn intentStatus
        }
    }
}
