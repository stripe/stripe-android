package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeRepository
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class CheckoutViewModel internal constructor(
    application: Application,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    fun getPaymentMethods(
        customerId: String,
        ephemeralKey: String,
        stripeAccountId: String? = this.stripeAccountId
    ) = liveData(workDispatcher) {
        val result =
            kotlin.runCatching {
                stripeRepository.getPaymentMethods(
                    ListPaymentMethodsParams(
                        customerId = customerId,
                        paymentMethodType = PaymentMethod.Type.Card
                    ),
                    publishableKey,
                    setOf(), // TODO: Add product usage tokens
                    ApiRequest.Options(ephemeralKey, stripeAccountId)
                )
            }
        emit(result)
    }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val stripeRepository = StripeApiRepository(
                application,
                publishableKey
            )

            return CheckoutViewModel(application, publishableKey, stripeAccountId, stripeRepository) as T
        }
    }
}
