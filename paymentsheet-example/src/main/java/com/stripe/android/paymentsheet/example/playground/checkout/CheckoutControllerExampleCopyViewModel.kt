@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.checkout.GooglePayConfiguration.Environment
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleCopyViewModel(
    private val repository: CheckoutControllerExampleBackendRepository,
    savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {

    private val _status = MutableStateFlow<Status>(Status.Loading)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _sessionComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionComplete: SharedFlow<Unit> = _sessionComplete.asSharedFlow()

    private var checkoutSessionClientSecret: String? = null
    val configuredCheckoutSessionClientSecret: String?
        get() = checkoutSessionClientSecret

    val controller = CheckoutController.Builder(
        application = application,
        savedStateHandle = savedStateHandle,
    ).resultCallback { result ->
        Log.d(TAG, "Result: $result")
    }.build()

    init {
        viewModelScope.launch {
            fetchAndConfigure()
        }
        viewModelScope.launch {
            controller.checkoutSession.collect { session ->
                updateConfiguredState { it.copy(checkoutSession = session) }
                if (session?.status == CheckoutSession.Status.Complete) {
                    _sessionComplete.tryEmit(Unit)
                }
            }
        }
    }

    private fun updateConfiguredState(update: (Status.Configured) -> Status.Configured) {
        val current = _status.value
        if (current is Status.Configured) {
            _status.value = update(current)
        }
    }

    private suspend fun fetchAndConfigure() {
        repository.fetchCheckoutSessionClientSecret().fold(
            onSuccess = { clientSecret ->
                checkoutSessionClientSecret = clientSecret
                configure(clientSecret, ExpressCheckoutExample.BothWalletsHorizontal)
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to fetch checkout session", error)
                _status.value = Status.Error(error.message ?: "Unknown error")
            },
        )
    }

    fun selectExpressCheckoutExample(example: ExpressCheckoutExample) {
        val clientSecret = checkoutSessionClientSecret ?: return
        viewModelScope.launch {
            configure(clientSecret, example)
        }
    }

    private suspend fun configure(
        clientSecret: String,
        example: ExpressCheckoutExample,
    ) {
        controller.configure(
            checkoutSessionClientSecret = clientSecret,
            configuration = CheckoutController.Configuration()
                .googlePayConfiguration(GooglePayConfiguration(environment = Environment.Test))
                .expressCheckoutElement(example.configuration()),
        ).fold(
            onSuccess = {
                _status.value = Status.Configured(
                    checkoutSession = controller.checkoutSession.value,
                    expressCheckoutExample = example,
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to configure", error)
                _status.value = Status.Error(error.message ?: "Configure failed")
            },
        )
    }

    override fun onCleared() {
        super.onCleared()
        controller.destroy()
    }

    sealed interface Status {
        data object Loading : Status
        data class Configured(
            val checkoutSession: CheckoutSession?,
            val expressCheckoutExample: ExpressCheckoutExample,
        ) : Status
        data class Error(val message: String) : Status
    }

    enum class ExpressCheckoutExample(val label: String) {
        BothWalletsHorizontal("Both wallets / horizontal"),
        LinkOnlyVertical("Link only / vertical"),
        GooglePayOnlyVertical("Google Pay only / vertical"),
        ;

        fun configuration(): ExpressCheckoutElement.Configuration {
            val paymentMethods = ExpressCheckoutElement.Configuration.PaymentMethods()
            val configuration = ExpressCheckoutElement.Configuration().paymentMethods(paymentMethods)

            when (this) {
                BothWalletsHorizontal -> {
                    paymentMethods
                        .link(ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Auto)
                        .googlePay(ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Auto)
                    return configuration
                        .buttonHeight(52.dp)
                        .buttonOrientation(ExpressCheckoutElement.Configuration.ButtonOrientation.Horizontal)
                }
                LinkOnlyVertical -> {
                    paymentMethods
                        .link(ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Auto)
                        .googlePay(ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Never)
                    return configuration
                        .buttonHeight(48.dp)
                        .buttonOrientation(ExpressCheckoutElement.Configuration.ButtonOrientation.Vertical)
                }
                GooglePayOnlyVertical -> {
                    paymentMethods
                        .link(ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Never)
                        .googlePay(ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Auto)
                    return configuration
                        .buttonHeight(56.dp)
                        .buttonOrientation(ExpressCheckoutElement.Configuration.ButtonOrientation.Vertical)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CheckoutControllerExampleCopy"

        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                CheckoutControllerExampleCopyViewModel(
                    repository = CheckoutControllerExampleBackendRepository(application),
                    savedStateHandle = createSavedStateHandle(),
                    application = application,
                )
            }
        }
    }
}
