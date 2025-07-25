package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class LinkControllerPlaygroundViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val status = MutableStateFlow<StatusMessage?>(null)
    val linkControllerState = MutableStateFlow(LinkControllerPlaygroundState())

    fun onConfigureResult(result: LinkController.ConfigureResult) {
        linkControllerState.update { it.copy(configureResult = result) }
    }

    fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        linkControllerState.update { it.copy(presentPaymentMethodsResult = result) }
    }

    fun onLinkControllerLookupConsumer(result: LinkController.LookupConsumerResult) {
        linkControllerState.update { it.copy(lookupConsumerResult = result) }
    }

    fun onLinkControllerCreatePaymentMethod(result: LinkController.CreatePaymentMethodResult) {
        linkControllerState.update { it.copy(createPaymentMethodResult = result) }
    }

    fun onLinkControllerAuthentication(result: LinkController.AuthenticationResult) {
        linkControllerState.update { it.copy(authenticationResult = result) }
    }

    fun onRegisterConsumer(result: LinkController.RegisterConsumerResult) {
        linkControllerState.update { it.copy(registerConsumerResult = result) }
    }
}
