package com.stripe.android.paymentsheet.example.samples.ui.wallet

import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey

@OptIn(ExperimentalSavedPaymentMethodsApi::class)
data class SavedPaymentMethodsViewState(
    val isProcessing: Boolean = false,
    val customerEphemeralKey: CustomerEphemeralKey? = null
)
