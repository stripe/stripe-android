package com.stripe.android.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    internal suspend fun saveCustomerShippingInformation(
        shippingInformation: ShippingInformation
    ): Result<Customer> = suspendCoroutine { continuation ->
        submittedShippingInfo = shippingInformation
        customerSession.setCustomerShippingInformation(
            shippingInformation = shippingInformation,
            productUsage = PRODUCT_USAGE,
            listener = object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    isShippingInfoSubmitted = true
                    continuation.resume(Result.success(customer))
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    isShippingInfoSubmitted = false
                    continuation.resume(
                        Result.failure(RuntimeException(errorMessage))
                    )
                }
            }
        )
    }

    /**
     * Validate [shippingInformation] using [shippingMethodsFactory]. If valid, use
     * [shippingMethodsFactory] to create the [ShippingMethod] options.
     */
    @JvmSynthetic
    internal suspend fun validateShippingInformation(
        shippingInfoValidator: PaymentSessionConfig.ShippingInformationValidator,
        shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory?,
        shippingInformation: ShippingInformation
    ): Result<List<ShippingMethod>> {
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

        return result
    }

    internal class Factory(
        private val customerSession: CustomerSession,
        private val paymentSessionData: PaymentSessionData
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
