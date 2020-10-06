package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiRequest
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripePaymentController
import com.stripe.android.StripeRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
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
    private val stripe: Stripe,
    private val workContext: CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application) {
    private val mutableError = MutableLiveData<Throwable>()
    private val mutableTransition = MutableLiveData<TransitionTarget>()
    private val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    private val mutableSelection = MutableLiveData<PaymentSelection?>()
    private val mutablePaymentIntentResult = MutableLiveData<PaymentIntentResult>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods
    internal val error: LiveData<Throwable> = mutableError
    internal val transition: LiveData<TransitionTarget> = mutableTransition
    internal val selection: LiveData<PaymentSelection?> = mutableSelection
    internal val paymentIntentResult: LiveData<PaymentIntentResult> = mutablePaymentIntentResult

    internal constructor(
        application: Application,
        publishableKey: String,
        stripeAccountId: String?,
        stripeRepository: StripeRepository,
        workContext: CoroutineContext = Dispatchers.IO
    ) : this(
        application,
        publishableKey,
        stripeAccountId,
        stripeRepository,
        Stripe(
            stripeRepository,
            StripePaymentController(
                application.applicationContext,
                publishableKey,
                stripeRepository,
                true,
                workContext = workContext
            ),
            publishableKey,
            stripeAccountId
        ),
        workContext = workContext
    )

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
        getPaymentSheetActivityArgs(intent)?.let { args ->
            updatePaymentMethods(
                args.ephemeralKey,
                args.customerId
            )
        }
    }

    fun checkout(activity: Activity) {
        val args = getPaymentSheetActivityArgs(activity.intent) ?: return
        // TODO(smaskell): Show processing indicator
        when (val selection = selection.value) {
            PaymentSelection.GooglePay -> TODO("smaskell: handle Google Pay confirmation")
            is PaymentSelection.Saved -> {
                // TODO(smaskell): Properly set savePaymentMethod/setupFutureUsage
                stripe.confirmPayment(
                    activity,
                    ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        selection.paymentMethodId,
                        args.clientSecret
                    )
                )
            }
            is PaymentSelection.New -> {
                stripe.confirmPayment(
                    activity,
                    ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        selection.paymentMethodCreateParams,
                        args.clientSecret
                    )
                )
            }
            null -> onError(IllegalStateException("checkout called when no payment method selected"))
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        stripe.onPaymentResult(
            requestCode, data,
            object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    mutablePaymentIntentResult.postValue(result)
                }

                override fun onError(e: Exception) {
                    this@PaymentSheetViewModel.onError(e)
                }
            }
        )
    }

    @VisibleForTesting
    internal fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        mutablePaymentMethods.postValue(paymentMethods)
    }

    private fun getPaymentSheetActivityArgs(intent: Intent): PaymentSheetActivityStarter.Args? {
        val args: PaymentSheetActivityStarter.Args? = PaymentSheetActivityStarter.Args.fromIntent(intent)
        if (args == null) {
            onError(IllegalStateException("Missing activity args"))
        }
        return args
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
                onSuccess = this@PaymentSheetViewModel::setPaymentMethods,
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
        private val applicationProvider: () -> Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationProvider()
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
