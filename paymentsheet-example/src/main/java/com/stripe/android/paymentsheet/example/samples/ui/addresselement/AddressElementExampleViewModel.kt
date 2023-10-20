package com.stripe.android.paymentsheet.example.samples.ui.addresselement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.samples.networking.ExamplePublishableKeyResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AddressElementExampleViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AddressElementExampleViewState>(
        value = AddressElementExampleViewState.Loading,
    )
    val state: StateFlow<AddressElementExampleViewState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadPublishableKey()
        }
    }

    fun handleResult(result: AddressLauncherResult) {
        when (result) {
            is AddressLauncherResult.Canceled -> {
                // Nothing to do here
            }
            is AddressLauncherResult.Succeeded -> {
                _state.update {
                    (it as AddressElementExampleViewState.Content).copy(
                        address = result.address,
                        status = null,
                    )
                }
            }
        }
    }

    private suspend fun loadPublishableKey() {
        val apiResult = Fuel
            .post("$backendUrl/publishable_key")
            .suspendable()
            .awaitModel(ExamplePublishableKeyResponse.serializer())

        when (apiResult) {
            is Result.Success -> {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                _state.value = AddressElementExampleViewState.Content(
                    publishableKey = apiResult.value.publishableKey,
                )
            }

            is Result.Failure -> {
                _state.value = AddressElementExampleViewState.Error(
                    message = "Failed to load key\n${apiResult.error.exception}",
                )
            }
        }
    }

    private companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me"
    }
}
