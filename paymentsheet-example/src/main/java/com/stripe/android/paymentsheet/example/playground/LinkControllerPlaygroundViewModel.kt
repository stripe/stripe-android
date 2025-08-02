package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LinkControllerPlaygroundViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    val linkController = LinkController.create(application, savedStateHandle)

    val status = MutableStateFlow<StatusMessage?>(null)
    val linkControllerState = MutableStateFlow(LinkControllerPlaygroundState())

    fun configureLinkController(config: LinkController.Configuration) {
        viewModelScope.launch {
            val result = linkController.configure(config)
            linkControllerState.update { it.copy(configureResult = result) }
        }
    }

    fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        linkControllerState.update { it.copy(presentPaymentMethodsResult = result) }
    }

    fun onEmailChange(email: String) {
        viewModelScope.launch {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                val result = linkController.lookupConsumer(email)
                linkControllerState.update { it.copy(lookupConsumerResult = result) }
            }
        }
    }

    fun onRegisterConsumerClick(email: String, phone: String, country: String, name: String?) {
        viewModelScope.launch {
            val result = linkController.registerConsumer(
                email = email,
                phone = phone,
                country = country,
                name = name
            )
            linkControllerState.update { it.copy(registerConsumerResult = result) }
        }
    }

    fun onLinkControllerAuthentication(result: LinkController.AuthenticationResult) {
        linkControllerState.update { it.copy(authenticationResult = result) }
    }

    fun onCreatePaymentMethodClick() {
        viewModelScope.launch {
            val result = linkController.createPaymentMethod()
            linkControllerState.update { it.copy(createPaymentMethodResult = result) }
        }
    }
}
