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

    fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        linkControllerState.update { it.copy(presentPaymentMethodsResult = result) }
    }

    fun onLinkControllerLookupConsumer(result: LinkController.LookupConsumerResult) {
        linkControllerState.update { it.copy(lookupConsumerResult = result) }
    }

    fun onLinkControllerCreatePaymentMethod(result: LinkController.CreatePaymentMethodResult) {
        linkControllerState.update { it.copy(createPaymentMethodResult = result) }
    }

    fun onLinkControllerPresentForAuthentication(result: LinkController.PresentForAuthenticationResult) {
        linkControllerState.update { it.copy(presentForAuthenticationResult = result) }
    }
}
