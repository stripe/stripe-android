package com.stripe.android.polling

import androidx.annotation.RestrictTo
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentStatusPoller @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val config: IntentStatusPoller.Config,
    private val dispatcher: CoroutineDispatcher,
) : IntentStatusPoller {

    private var pollingJob: Job? = null

    private val _state = MutableStateFlow<StripeIntent.Status?>(null)
    override val state: StateFlow<StripeIntent.Status?> = _state

    override fun startPolling(scope: CoroutineScope) {
        pollingJob?.cancel()
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

        delay(config.pollingInterval)
        performPoll()
    }

    private suspend fun fetchIntentStatus(): StripeIntent.Status? {
        val stripeIntent = stripeRepository.retrieveStripeIntent(
            clientSecret = config.clientSecret,
            options = requestOptions,
        )
        return stripeIntent.getOrNull()?.status
    }

    override suspend fun forcePoll(): StripeIntent.Status? {
        return fetchIntentStatus()
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
