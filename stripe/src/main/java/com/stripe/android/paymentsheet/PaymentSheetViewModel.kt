package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.stripe.android.ApiRequest
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripePaymentController
import com.stripe.android.StripeRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    private val workContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {
    private val mutablePaymentIntent = MutableLiveData<PaymentIntent>()
    private val mutableError = MutableLiveData<Throwable>()
    private val mutableTransition = MutableLiveData<TransitionTarget>()
    private val mutableSheetMode = MutableLiveData<SheetMode>()
    private val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    private val mutableSelection = MutableLiveData<PaymentSelection?>()
    private val mutablePaymentIntentResult = MutableLiveData<PaymentIntentResult>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods
    internal val error: LiveData<Throwable> = mutableError
    internal val transition: LiveData<TransitionTarget> = mutableTransition
    internal val selection: LiveData<PaymentSelection?> = mutableSelection
    internal val paymentIntentResult: LiveData<PaymentIntentResult> = mutablePaymentIntentResult
    internal val sheetMode: LiveData<SheetMode> = mutableSheetMode.distinctUntilChanged()
    internal val paymentIntent: LiveData<PaymentIntent> = mutablePaymentIntent

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

    fun updateMode(mode: SheetMode) {
        mutableSheetMode.postValue(mode)
    }

    fun fetchPaymentIntent(intent: Intent) {
        getPaymentSheetActivityArgs(intent)?.let { args ->
            CoroutineScope(workContext).launch {
                kotlin.runCatching {
                    stripeRepository.retrievePaymentIntent(args.clientSecret, ApiRequest.Options(publishableKey, stripeAccountId))
                }.fold(
                    onSuccess = mutablePaymentIntent::postValue,
                    onFailure = this@PaymentSheetViewModel::onError
                )
            }
        }
    }

    fun checkout(activity: Activity) {
        val args = getPaymentSheetActivityArgs(activity.intent) ?: return
        // TODO(smaskell): Show processing indicator
        val confirmParams = when (val selection = selection.value) {
            PaymentSelection.GooglePay -> TODO("smaskell: handle Google Pay confirmation")
            is PaymentSelection.Saved -> {
                // TODO(smaskell): Properly set savePaymentMethod/setupFutureUsage
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    selection.paymentMethodId,
                    args.clientSecret
                )
            }
            is PaymentSelection.New -> {
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    selection.paymentMethodCreateParams,
                    args.clientSecret
                )
            }
            null -> {
                onError(IllegalStateException("checkout called when no payment method selected"))
                null
            }
        }
        confirmParams?.let {
            paymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(activity),
                it,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.takeIf { paymentController.shouldHandlePaymentResult(requestCode, it) }?.let {
            paymentController.handlePaymentResult(
                it,
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
        // User has saved PM's and is selected
        SelectSavedPaymentMethod,
        // User has saved PM's and is adding a new one
        AddPaymentMethodFull,
        // User has no saved PM's
        AddPaymentMethodSheet
    }

    internal enum class SheetMode(val height: Int, @BottomSheetBehavior.State val behaviourState: Int) {
        Full(MATCH_PARENT, STATE_EXPANDED),
        FullCollapsed(MATCH_PARENT, STATE_COLLAPSED),
        Wrapped(WRAP_CONTENT, STATE_COLLAPSED)
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
            val paymentController = StripePaymentController(
                application.applicationContext,
                publishableKey,
                stripeRepository,
                true
            )

            return PaymentSheetViewModel(publishableKey, stripeAccountId, stripeRepository, paymentController) as T
        }
    }
}
