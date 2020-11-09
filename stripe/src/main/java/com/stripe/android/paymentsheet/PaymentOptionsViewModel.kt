package com.stripe.android.paymentsheet

import androidx.lifecycle.ViewModel

internal class PaymentOptionsViewModel(
    private val publishableKey: String,
    private val stripeAccountId: String?
) : ViewModel()
