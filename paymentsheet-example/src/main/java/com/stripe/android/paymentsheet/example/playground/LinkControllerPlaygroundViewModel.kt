package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class LinkControllerPlaygroundViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val linkController = LinkController.create(application, savedStateHandle)
    private var linkControllerPresenter: LinkController.Presenter? = null

    val status = MutableStateFlow<StatusMessage?>(null)

    val state = MutableStateFlow(LinkControllerPlaygroundState())

    fun onCreateActivity(activity: ComponentActivity) {
        linkControllerPresenter = linkController.createPresenter(
            activity = activity,
            presentPaymentMethodsCallback = this::onLinkControllerPresentPaymentMethod,
            authenticationCallback = this::onLinkControllerAuthentication,
            authorizeCallback = this::onLinkControllerAuthorization,
        )
        activity.lifecycleScope.launch {
            linkController.state(activity).collect { controllerState ->
                state.update { it.copy(controllerState = controllerState) }
            }
        }
    }

    fun onDestroyActivity() {
        linkControllerPresenter = null
    }

    fun configureLinkController(config: LinkController.Configuration) {
        if (state.value.configureResult == LinkController.ConfigureResult.Success) {
            // Assumes `config` doesn't change in LinkControllerPlaygroundActivity.
            return
        }
        viewModelScope.launch {
            val result = linkController.configure(config)
            state.update { it.copy(configureResult = result) }
        }
    }

    private fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        state.update { it.copy(presentPaymentMethodsResult = result) }
    }

    fun onLookupClick(email: String) {
        viewModelScope.launch {
            val result = linkController.lookupConsumer(email)
            state.update { it.copy(lookupConsumerResult = result) }
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
            state.update { it.copy(registerConsumerResult = result) }
        }
    }

    fun onUpdatePhoneNumberClick(phoneNumber: String) {
        viewModelScope.launch {
            val result = linkController.updatePhoneNumber(phoneNumber)
            state.update { it.copy(updatePhoneNumberResult = result) }
        }
    }

    fun onCreatePaymentMethodClick() {
        viewModelScope.launch {
            val result = linkController.createPaymentMethod()
            state.update { it.copy(createPaymentMethodResult = result) }
        }
    }

    fun onPaymentMethodClick(email: String, paymentMethodType: LinkController.PaymentMethodType?) {
        linkControllerPresenter?.presentPaymentMethods(
            email = email.takeIf { it.isNotBlank() },
            paymentMethodType = paymentMethodType,
        )
    }

    fun onAuthenticateClick(email: String, existingOnly: Boolean) {
        val cleanedEmail = email.takeIf { it.isNotBlank() } ?: ""
        if (existingOnly) {
            linkControllerPresenter?.authenticateExistingConsumer(cleanedEmail)
        } else {
            linkControllerPresenter?.authenticate(cleanedEmail)
        }
    }

    fun onAuthorizeClick(linkAuthIntentId: String) {
        linkControllerPresenter?.authorize(
            linkAuthIntentId = linkAuthIntentId
        )
    }

    private fun onLinkControllerAuthentication(result: LinkController.AuthenticationResult) {
        state.update { it.copy(authenticationResult = result) }
    }

    private fun onLinkControllerAuthorization(result: LinkController.AuthorizeResult) {
        state.update { it.copy(authorizeResult = result) }
    }

    fun onLogOutClick() {
        viewModelScope.launch {
            val result = linkController.logOut()
            state.update { it.copy(logOutResult = result) }
        }
    }
}
