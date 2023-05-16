package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle

internal class SavedPaymentMethodsViewModel(
    application: Application,
    private val handle: SavedStateHandle,
) : AndroidViewModel(application) {
    val savedPaymentMethodsSheetStateComponent: SavedPaymentMethodsSheetStateComponent =
        DaggerSavedPaymentMethodsSheetStateComponent
            .builder()
            .appContext(application)
            .savedPaymentMethodsViewModel(this)
            .build()
}