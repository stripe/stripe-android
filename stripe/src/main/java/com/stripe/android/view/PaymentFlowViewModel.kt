package com.stripe.android.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation

internal class PaymentFlowViewModel(
    private val customerSession: CustomerSession
) : ViewModel() {

    @JvmSynthetic
    internal fun saveCustomerShippingInformation(
        shippingInformation: ShippingInformation
    ): LiveData<Result<*>> {
        val resultData = MutableLiveData<Result<*>>()
        customerSession.setCustomerShippingInformation(
            shippingInformation,
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    resultData.value = Result.create(customer)
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    resultData.value = Result.create(errorMessage)
                }
            }
        )
        return resultData
    }

    internal data class Result<out T> internal constructor(
        internal val status: Status,
        internal val data: T
    ) {
        enum class Status {
            SUCCESS, ERROR
        }

        internal companion object {
            @JvmSynthetic
            internal fun create(customer: Customer): Result<Customer> {
                return Result(Status.SUCCESS, customer)
            }

            @JvmSynthetic
            internal fun create(errorMessage: String): Result<String> {
                return Result(Status.ERROR, errorMessage)
            }
        }
    }

    internal class Factory(
        private val customerSession: CustomerSession
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentFlowViewModel(customerSession) as T
        }
    }
}
