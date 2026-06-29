package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeClient
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
internal class EmbeddedPaymentElementViewModel @Inject constructor(
    val embeddedPaymentElementSubcomponentFactory: EmbeddedPaymentElementSubcomponent.Factory,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
    eventReporter: EventReporter,
) : ViewModel() {
    init {
        eventReporter.onInit()
    }

    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val paymentElementCallbackIdentifier: String,
        private val statusBarColor: Int?,
        private val stripeClient: StripeClient? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            val resolvedClient = stripeClient ?: run {
                val config = PaymentConfiguration.getInstance(extras.requireApplication())
                StripeClient(config.publishableKey, config.stripeAccountId)
            }
            val component = DaggerEmbeddedPaymentElementViewModelComponent.factory().build(
                savedStateHandle = extras.createSavedStateHandle(),
                application = extras.requireApplication(),
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                statusBarColor = statusBarColor,
                stripeClient = resolvedClient,
            )
            @Suppress("UNCHECKED_CAST")
            return component.viewModel as T
        }
    }
}
