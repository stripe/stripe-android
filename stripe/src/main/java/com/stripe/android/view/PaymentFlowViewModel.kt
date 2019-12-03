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

    @JvmSynthetic
    internal fun saveCustomerShippingInformation(
        shippingInformation: ShippingInformation
    ): LiveData<SaveCustomerShippingInfoResult<*>> {
        val resultData = MutableLiveData<SaveCustomerShippingInfoResult<*>>()
        customerSession.setCustomerShippingInformation(
            shippingInformation,
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    resultData.value = SaveCustomerShippingInfoResult.create(customer)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = SaveCustomerShippingInfoResult.create(errorMessage)
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
    ): LiveData<ValidateShippingInfoResult<*>> {
        val resultData = MutableLiveData<ValidateShippingInfoResult<*>>()
        workScope.launch {
            val isValid = shippingInfoValidator.isValid(shippingInformation)
            if (isValid) {
                val shippingMethods =
                    shippingMethodsFactory?.create(shippingInformation).orEmpty()
                resultData.postValue(ValidateShippingInfoResult.create(shippingMethods))
            } else {
                val errorMessage = shippingInfoValidator.getErrorMessage(shippingInformation)
                resultData.postValue(ValidateShippingInfoResult.create(errorMessage))
            }
        }
        return resultData
    }

    internal data class SaveCustomerShippingInfoResult<out T> internal constructor(
        internal val status: Status,
        internal val data: T
    ) {
        internal companion object {
            @JvmSynthetic
            internal fun create(customer: Customer): SaveCustomerShippingInfoResult<Customer> {
                return SaveCustomerShippingInfoResult(Status.SUCCESS, customer)
            }

            @JvmSynthetic
            internal fun create(errorMessage: String): SaveCustomerShippingInfoResult<String> {
                return SaveCustomerShippingInfoResult(Status.ERROR, errorMessage)
            }
        }
    }

    internal data class ValidateShippingInfoResult<out T> internal constructor(
        internal val status: Status,
        internal val data: T
    ) {
        internal companion object {
            @JvmSynthetic
            internal fun create(
                shippingMethods: List<ShippingMethod>
            ): ValidateShippingInfoResult<List<ShippingMethod>> {
                return ValidateShippingInfoResult(Status.SUCCESS, shippingMethods)
            }

            @JvmSynthetic
            internal fun create(errorMessage: String): ValidateShippingInfoResult<String> {
                return ValidateShippingInfoResult(Status.ERROR, errorMessage)
            }
        }
    }

    internal enum class Status {
        SUCCESS, ERROR
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
