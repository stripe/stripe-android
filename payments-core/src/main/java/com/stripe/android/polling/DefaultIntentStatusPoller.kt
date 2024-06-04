package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentStatusPoller @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfigProvider: Provider<PaymentConfiguration>,
    private val config: IntentStatusPoller.Config,
    private val dispatcher: CoroutineDispatcher,
) : IntentStatusPoller {

    private var attempts: Int = 0
    private var pollingJob: Job? = null

    private val _state = MutableStateFlow<StripeIntent.Status?>(null)
    override val state: StateFlow<StripeIntent.Status?> = _state

    override fun startPolling(scope: CoroutineScope) {
        pollingJob = scope.launch(dispatcher) {
            performPoll()
        }
    }

    private suspend fun performPoll(force: Boolean = false) {
        if (force || attempts < config.maxAttempts) {
            attempts += 1

            _state.value = fetchIntentStatus()

            val canTryAgain = attempts < config.maxAttempts
            if (canTryAgain) {
                delay(calculateDelay(attempts))
                performPoll()
            }
        }
    }

    private suspend fun fetchIntentStatus(): StripeIntent.Status? {
        val paymentConfig = paymentConfigProvider.get()
        val paymentIntent = stripeRepository.retrievePaymentIntent(
            clientSecret = config.clientSecret,
            options = ApiRequest.Options(
                publishableKeyProvider = { paymentConfig.publishableKey },
                stripeAccountIdProvider = { paymentConfig.stripeAccountId },
            ),
        )
        return paymentIntent.getOrNull()?.status
    }

    override suspend fun forcePoll(): StripeIntent.Status? {
        return fetchIntentStatus()
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}

internal fun calculateDelay(attempts: Int): Duration {
    val delay = (1.0 + attempts).pow(2)
    return delay.seconds
}
