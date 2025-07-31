package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LinkControllerPlaygroundViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val status = MutableStateFlow<StatusMessage?>(null)
    val linkControllerState = MutableStateFlow(LinkControllerPlaygroundState())

    fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        linkControllerState.update { it.copy(presentPaymentMethodsResult = result) }
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

    fun lookupConsumer(email: String, linkController: LinkController) {
        viewModelScope.launch {
            val result = linkController.lookupConsumer(email)
            linkControllerState.update { it.copy(lookupConsumerResult = result) }
        }
    }
}
