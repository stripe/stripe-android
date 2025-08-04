package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            val result = linkController.configure(config)
            state.update { it.copy(configureResult = result) }
        }
    }

    private fun onLinkControllerPresentPaymentMethod(result: LinkController.PresentPaymentMethodsResult) {
        state.update { it.copy(presentPaymentMethodsResult = result) }
    }

    fun onEmailChange(email: String) {
        viewModelScope.launch {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                val result = linkController.lookupConsumer(email)
                state.update { it.copy(lookupConsumerResult = result) }
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
            state.update { it.copy(registerConsumerResult = result) }
        }
    }

    fun onCreatePaymentMethodClick() {
        viewModelScope.launch {
            val result = linkController.createPaymentMethod()
            state.update { it.copy(createPaymentMethodResult = result) }
        }
    }

    fun onPaymentMethodClick(email: String) {
        linkControllerPresenter?.paymentSelectionHint =
            "Lorem ipsum dolor sit amet consectetur adipiscing elit."
        linkControllerPresenter?.presentPaymentMethods(email = email.takeIf { it.isNotBlank() })
    }

    fun onAuthenticateClick(email: String, existingOnly: Boolean) {
        val cleanedEmail = email.takeIf { it.isNotBlank() } ?: ""
        if (existingOnly) {
            linkControllerPresenter?.authenticateExistingConsumer(cleanedEmail)
        } else {
            linkControllerPresenter?.authenticate(cleanedEmail)
        }
    }

    private fun onLinkControllerAuthentication(result: LinkController.AuthenticationResult) {
        state.update { it.copy(authenticationResult = result) }
    }
}
