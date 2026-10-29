package com.stripe.android.polling

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration

internal fun createIntentStatusPoller(
    enqueuedStatuses: List<StripeIntent.Status>,
    dispatcher: CoroutineDispatcher,
    pollingInterval: Duration,
    isSetupIntent: Boolean = false,
): DefaultIntentStatusPoller {
    return DefaultIntentStatusPoller(
        stripeRepository = FakeStripeRepository(enqueuedStatuses, isSetupIntent),
        requestOptions = RequestCredentials(
            apiKey = "key",
            stripeAccount = "acct_123",
        ),
        config = IntentStatusPoller.Config(
            clientSecret = "secret",
            pollingInterval = pollingInterval,
        ),
        dispatcher = dispatcher,
        apiConfiguration = ApiConfiguration("pk_test_123").build(),
    )
}

private class FakeStripeRepository(
    enqueuedStatuses: List<StripeIntent.Status>,
    private val isSetupIntent: Boolean,
) : AbsFakeStripeRepository() {

    private val queue = enqueuedStatuses.toMutableList()

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<StripeIntent> {
        val intentStatus = queue.removeAt(0)
        val stripeIntent = if (isSetupIntent) {
            SetupIntentFactory.create(status = intentStatus)
        } else {
            PaymentIntentFactory.create(status = intentStatus)
        }
        return Result.success(stripeIntent)
    }
}
