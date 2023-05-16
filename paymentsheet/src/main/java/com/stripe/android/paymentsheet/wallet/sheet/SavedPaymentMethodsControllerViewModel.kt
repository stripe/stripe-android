package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle

internal class SavedPaymentMethodsControllerViewModel(
    application: Application,
    private val handle: SavedStateHandle,
) : AndroidViewModel(application) {

    val savedPaymentMethodsControllerStateComponent: SavedPaymentMethodsControllerStateComponent =
        DaggerSavedPaymentMethodsSheetStateComponent
            .builder()
            .appContext(application)
            .savedPaymentMethodsControllerViewModel(this)
            .build()
}