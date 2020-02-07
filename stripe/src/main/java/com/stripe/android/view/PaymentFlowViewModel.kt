package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class PaymentFlowViewModel(
    private val customerSession: CustomerSession,
    internal var paymentSessionData: PaymentSessionData,
    private val workScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ViewModel() {
    internal var shippingMethods: List<ShippingMethod> = emptyList()
    internal var isShippingInfoSubmitted: Boolean = false

    @JvmSynthetic
    internal fun saveCustomerShippingInformation(
        shippingInformation: ShippingInformation
    ): LiveData<SaveCustomerShippingInfoResult> {
        val resultData = MutableLiveData<SaveCustomerShippingInfoResult>()
        customerSession.setCustomerShippingInformation(
            shippingInformation,
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    isShippingInfoSubmitted = true
                    resultData.value = SaveCustomerShippingInfoResult.Success(customer)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    isShippingInfoSubmitted = false
                    resultData.value = SaveCustomerShippingInfoResult.Error(errorMessage)
                }
            }
        )
        return resultData
    }

    /**
     * Validate [shippingInformation] using [shippingMethodsFactory]. If valid, use
     * [shippingMethodsFactory] to create the [ShippingMethod] options.
     */
    @JvmSynthetic
    internal fun validateShippingInformation(
        shippingInfoValidator: PaymentSessionConfig.ShippingInformationValidator,
        shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory?,
        shippingInformation: ShippingInformation
    ): LiveData<ValidateShippingInfoResult> {
        val resultData = MutableLiveData<ValidateShippingInfoResult>()
        workScope.launch {
            val isValid = shippingInfoValidator.isValid(shippingInformation)
            if (isValid) {
                val shippingMethods =
                    shippingMethodsFactory?.create(shippingInformation).orEmpty()
                this@PaymentFlowViewModel.shippingMethods = shippingMethods
                resultData.postValue(ValidateShippingInfoResult.Success(shippingMethods))
            } else {
                val errorMessage = shippingInfoValidator.getErrorMessage(shippingInformation)
                this@PaymentFlowViewModel.shippingMethods = emptyList()
                resultData.postValue(ValidateShippingInfoResult.Error(errorMessage))
            }
        }
        return resultData
    }

    internal sealed class SaveCustomerShippingInfoResult {
        data class Success(val customer: Customer) : SaveCustomerShippingInfoResult()
        data class Error(val errorMessage: String) : SaveCustomerShippingInfoResult()
    }

    internal sealed class ValidateShippingInfoResult {
        data class Success(val shippingMethods: List<ShippingMethod>) : ValidateShippingInfoResult()
        data class Error(val errorMessage: String) : ValidateShippingInfoResult()
    }

    internal class Factory(
        private val customerSession: CustomerSession,
        private val paymentSessionData: PaymentSessionData
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(
                customerSession,
                paymentSessionData
            ) as T
        }
    }
}
