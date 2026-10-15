package com.stripe.android.elements.ece

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.elements.ece.injection.DaggerStandaloneExpressCheckoutElementComponent

internal class StandaloneExpressCheckoutElementViewModel(
    extras: CreationExtras,
    callbackIdentifier: String,
    statusBarColor: Int?,
) : ViewModel() {
    private val component = DaggerStandaloneExpressCheckoutElementComponent.factory().create(
        application = extras.requireApplication(),
        savedStateHandle = extras.createSavedStateHandle(),
        callbackIdentifier = callbackIdentifier,
        statusBarColor = statusBarColor,
        coroutineScope = viewModelScope,
    )

    val controller = component.controller
    val interactor = component.interactor

    class Factory(
        private val callbackIdentifier: String,
        private val statusBarColor: Int?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return StandaloneExpressCheckoutElementViewModel(extras, callbackIdentifier, statusBarColor) as T
        }
    }
}
