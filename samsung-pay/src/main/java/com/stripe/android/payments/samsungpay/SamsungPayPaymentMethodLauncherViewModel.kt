package com.stripe.android.payments.samsungpay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class SamsungPayPaymentMethodLauncherViewModel(
    private val stripe: Stripe,
    private val args: SamsungPayPaymentMethodLauncherContract.Args,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    internal var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    private val _result = MutableStateFlow<SamsungPayPaymentMethodLauncher.Result?>(null)
    internal val result = _result.asStateFlow()

    fun updateResult(result: SamsungPayPaymentMethodLauncher.Result) {
        _result.value = result
    }

    suspend fun createPaymentMethod(
        paymentCredential: String,
    ): SamsungPayPaymentMethodLauncher.Result {
        val params = PaymentMethodCreateParams.createSamsungPay(paymentCredential)

        return withContext(Dispatchers.IO) {
            runCatching {
                stripe.createPaymentMethodSynchronous(params)
            }.fold(
                onSuccess = { paymentMethod ->
                    SamsungPayPaymentMethodLauncher.Result.Completed(paymentMethod)
                },
                onFailure = { error ->
                    SamsungPayPaymentMethodLauncher.Result.Failed(
                        error = error,
                        errorCode = SamsungPayPaymentMethodLauncher.INTERNAL_ERROR,
                    )
                }
            )
        }
    }

    internal class Factory(
        private val args: SamsungPayPaymentMethodLauncherContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val config = PaymentConfiguration.getInstance(application)
            val stripe = Stripe(application, config.publishableKey)

            return SamsungPayPaymentMethodLauncherViewModel(
                stripe = stripe,
                args = args,
                savedStateHandle = savedStateHandle,
            ) as T
        }
    }

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
