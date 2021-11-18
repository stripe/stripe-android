package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.core.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentFlowViewModel(
    private val customerSession: CustomerSession,
    internal var paymentSessionData: PaymentSessionData,
    private val workContext: CoroutineContext
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
    ) = liveData {
        val result = withContext(workContext) {
            val isValid = shippingInfoValidator.isValid(shippingInformation)
            if (isValid) {
                runCatching {
                    shippingMethodsFactory?.create(shippingInformation).orEmpty()
                }
            } else {
                runCatching {
                    shippingInfoValidator.getErrorMessage(shippingInformation)
                }.fold(
                    onSuccess = { RuntimeException(it) },
                    onFailure = { it }
                ).let {
                    Result.failure(it)
                }
            }
        }
        shippingMethods = result.getOrDefault(emptyList())
        emit(result)
    }

    internal class Factory(
        private val customerSession: CustomerSession,
        private val paymentSessionData: PaymentSessionData
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(
                customerSession,
                paymentSessionData,
                Dispatchers.IO
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
