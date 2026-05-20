package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@CheckoutSessionPreview
internal class CurrencySelectorViewModel(
    private val checkoutSession: StateFlow<CheckoutSession>,
    private val updateCurrency: suspend (String) -> Result<Unit>,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<ResolvableString?>(null)
    val errorMessage: StateFlow<ResolvableString?> = _errorMessage.asStateFlow()

    init {
        val previouslyInitialized: Boolean? = savedStateHandle[KEY_INITIALIZED]
        if (previouslyInitialized == null) {
            savedStateHandle[KEY_INITIALIZED] = true
            fireEvent(PaymentSheetEvent.AdaptivePricingCurrencySelectorInit())
        }

        viewModelScope.launch {
            checkoutSession.collect {
                _errorMessage.value = null
            }
        }
    }

    fun onCurrencySelected(currencyCode: String) {
        viewModelScope.launch {
            updateCurrency(currencyCode)
                .onFailure { throwable ->
                    _errorMessage.value = throwable.stripeErrorMessage()
                }
        }
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        viewModelScope.launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                )
            )
        }
    }

    companion object {
        private const val KEY_INITIALIZED = "currency_selector_initialized"
    }

    internal class Factory(
        private val checkoutSession: StateFlow<CheckoutSession>,
        private val updateCurrency: suspend (String) -> Result<Unit>,
        private val analyticsRequestExecutor: AnalyticsRequestExecutor,
        private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException("Use create(modelClass, extras) instead")
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: androidx.lifecycle.viewmodel.CreationExtras
        ): T {
            return CurrencySelectorViewModel(
                checkoutSession = checkoutSession,
                updateCurrency = updateCurrency,
                analyticsRequestExecutor = analyticsRequestExecutor,
                paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }
}
