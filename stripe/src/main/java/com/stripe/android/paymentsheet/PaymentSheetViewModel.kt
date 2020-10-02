package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeRepository
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    application: Application,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val workContext: CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application) {
    private val mutableError = MutableLiveData<Throwable>()
    private val mutableTransition = MutableLiveData<TransitionTarget>()
    private val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    private val mutableSelection = MutableLiveData<PaymentSelection?>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods
    internal val error: LiveData<Throwable> = mutableError
    internal val transition: LiveData<TransitionTarget> = mutableTransition
    internal val selection: LiveData<PaymentSelection?> = mutableSelection

    fun onError(throwable: Throwable) {
        mutableError.postValue(throwable)
    }

    fun transitionTo(target: TransitionTarget) {
        mutableTransition.postValue(target)
    }

    fun updateSelection(selection: PaymentSelection?) {
        mutableSelection.postValue(selection)
    }

    fun updatePaymentMethods(intent: Intent) {
        val args: PaymentSheetActivityStarter.Args? = PaymentSheetActivityStarter.Args.fromIntent(intent)
        if (args == null) {
            onError(IllegalStateException("Missing activity args"))
        } else {
            updatePaymentMethods(
                args.ephemeralKey,
                args.customerId
            )
        }
    }

    private fun updatePaymentMethods(
        ephemeralKey: String,
        customerId: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        CoroutineScope(workContext).launch {
            val result = kotlin.runCatching {
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
            result.fold(
                onSuccess = {
                    mutablePaymentMethods.postValue(it)
                },
                onFailure = {
                    onError(it)
                }
            )
        }
    }

    internal enum class TransitionTarget {
        AddCard
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

            return PaymentSheetViewModel(application, publishableKey, stripeAccountId, stripeRepository) as T
        }
    }
}
