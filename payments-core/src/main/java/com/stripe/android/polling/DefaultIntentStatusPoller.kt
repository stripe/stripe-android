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

private const val MILLIS_PER_SECOND = 1_000L

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
                val delayInMillis = calculateDelayInMillis(attempts)
                delay(delayInMillis)
                performPoll()
            }
        }
    }

    private suspend fun fetchIntentStatus(): StripeIntent.Status? {
        val paymentConfig = paymentConfigProvider.get()
        val paymentIntent = runCatching {
            stripeRepository.retrievePaymentIntent(
                clientSecret = config.clientSecret,
                options = ApiRequest.Options(
                    publishableKeyProvider = { paymentConfig.publishableKey },
                    stripeAccountIdProvider = { paymentConfig.stripeAccountId },
                ),
            )
        }
        return paymentIntent.getOrNull()?.status
    }

    override suspend fun forcePoll() {
        performPoll(force = true)
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}

internal fun calculateDelayInMillis(attempts: Int): Long {
    val seconds = (1.0 + attempts).pow(2)
    return seconds.toLong() * MILLIS_PER_SECOND
}
