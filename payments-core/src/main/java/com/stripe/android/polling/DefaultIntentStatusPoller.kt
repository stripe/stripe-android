package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
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
import kotlin.time.Duration.Companion.seconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentStatusPoller @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val apiConfigProvider: () -> ApiConfiguration.State,
    private val config: IntentStatusPoller.Config,
    private val dispatcher: CoroutineDispatcher,
) : IntentStatusPoller {

    private var pollingJob: Job? = null

    private val _state = MutableStateFlow<StripeIntent.Status?>(null)
    override val state: StateFlow<StripeIntent.Status?> = _state

    override fun startPolling(scope: CoroutineScope) {
        pollingJob = scope.launch(dispatcher) {
            performPoll()
        }
    }

    private suspend fun performPoll() {
        when (state.value) {
            StripeIntent.Status.Canceled,
            StripeIntent.Status.Succeeded ->
                // Do not poll when stripe intent is in terminal state.
                return
            StripeIntent.Status.Processing,
            StripeIntent.Status.RequiresAction,
            StripeIntent.Status.RequiresConfirmation,
            StripeIntent.Status.RequiresPaymentMethod,
            StripeIntent.Status.RequiresCapture,
            null -> {}
        }

        _state.value = fetchIntentStatus()

        delay(1.seconds)
        performPoll()
    }

    private suspend fun fetchIntentStatus(): StripeIntent.Status? {
        val apiConfig = apiConfigProvider()
        val paymentIntent = stripeRepository.retrievePaymentIntent(
            clientSecret = config.clientSecret,
            options = ApiRequest.Options(
                apiKey = apiConfig.publishableKey,
                stripeAccount = apiConfig.stripeAccountId,
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
