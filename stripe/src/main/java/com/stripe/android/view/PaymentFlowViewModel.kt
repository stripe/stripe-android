package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
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

    internal var selectedShippingMethod: ShippingMethod? = null
    internal var submittedShippingInfo: ShippingInformation? = null

    internal var currentPage: Int = 0

    @JvmSynthetic
    internal fun saveCustomerShippingInformation(
        shippingInformation: ShippingInformation
    ): LiveData<Result<Customer>> {
        submittedShippingInfo = shippingInformation
        val resultData = MutableLiveData<Result<Customer>>()
        customerSession.setCustomerShippingInformation(
            shippingInformation = shippingInformation,
            productUsage = PRODUCT_USAGE,
            listener = object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    isShippingInfoSubmitted = true
                    resultData.value = Result.success(customer)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    isShippingInfoSubmitted = false
                    resultData.value = Result.failure(RuntimeException(errorMessage))
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
    ): LiveData<Result<List<ShippingMethod>>> {
        val resultData = MutableLiveData<Result<List<ShippingMethod>>>()
        workScope.launch {
            val isValid = shippingInfoValidator.isValid(shippingInformation)
            if (isValid) {
                val shippingMethods =
                    shippingMethodsFactory?.create(shippingInformation).orEmpty()
                this@PaymentFlowViewModel.shippingMethods = shippingMethods
                resultData.postValue(Result.success(shippingMethods))
            } else {
                val errorMessage = shippingInfoValidator.getErrorMessage(shippingInformation)
                this@PaymentFlowViewModel.shippingMethods = emptyList()
                resultData.postValue(Result.failure(RuntimeException(errorMessage)))
            }
        }
        return resultData
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

    internal companion object {
        private const val SHIPPING_INFO_PRODUCT_TOKEN = "ShippingInfoScreen"

        val PRODUCT_USAGE = setOf(
            PaymentSession.PRODUCT_TOKEN,
            PaymentFlowActivity.PRODUCT_TOKEN,
            SHIPPING_INFO_PRODUCT_TOKEN
        )
    }
}
