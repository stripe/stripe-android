package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    private val googlePayRepository: GooglePayRepository,
    internal val args: PaymentSheetActivityStarter.Args,
    private val workContext: CoroutineContext
) : SheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    isGuestMode = args is PaymentSheetActivityStarter.Args.Guest
) {
    private val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    private val mutablePaymentIntent = MutableLiveData<PaymentIntent?>()
    private val mutableViewState = MutableLiveData<ViewState>(null)

    internal val paymentIntent: LiveData<PaymentIntent?> = mutablePaymentIntent
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods
    internal val viewState: LiveData<ViewState> = mutableViewState.distinctUntilChanged()

    fun updatePaymentMethods() {
        when (args) {
            is PaymentSheetActivityStarter.Args.Default -> {
                updatePaymentMethods(
                    args.ephemeralKey,
                    args.customerId
                )
            }
            is PaymentSheetActivityStarter.Args.Guest -> {
                mutablePaymentMethods.postValue(emptyList())
            }
        }
    }

    fun fetchPaymentIntent() {
        viewModelScope.launch {
            val result = withContext(workContext) {
                runCatching {
                    val paymentIntent = stripeRepository.retrievePaymentIntent(
                        args.clientSecret,
                        ApiRequest.Options(publishableKey, stripeAccountId)
                    )
                    requireNotNull(paymentIntent) {
                        "Could not parse PaymentIntent."
                    }
                }
            }
            result.fold(
                onSuccess = { paymentIntent ->
                    mutablePaymentIntent.value = paymentIntent

                    val amount = paymentIntent.amount
                    val currencyCode = paymentIntent.currency
                    if (amount != null && currencyCode != null) {
                        mutableViewState.value = ViewState.Ready(amount, currencyCode)
                    } else {
                        // TODO(mshafrir-stripe): improve error message
                        onError(IllegalStateException("PaymentIntent is invalid."))
                    }
                },
                onFailure = {
                    mutablePaymentIntent.value = null

                    onError(it)
                }
            )
        }
    }

    fun checkout(activity: Activity) {
        mutableProcessing.value = true

        val confirmParams = createConfirmParams(args.clientSecret)

        when {
            confirmParams != null -> {
                mutableViewState.value = ViewState.Confirming
                paymentController.startConfirmAndAuth(
                    AuthActivityStarter.Host.create(activity),
                    confirmParams,
                    ApiRequest.Options(
                        apiKey = publishableKey,
                        stripeAccount = stripeAccountId
                    )
                )
            }
            else -> {
                onError(
                    IllegalStateException("checkout called when no payment method selected")
                )
            }
        }
    }

    @VisibleForTesting
    internal fun createConfirmParams(
        clientSecret: String
    ): ConfirmPaymentIntentParams? {
        return when (val selection = selection.value) {
            PaymentSelection.GooglePay -> TODO("smaskell: handle Google Pay confirmation")
            is PaymentSelection.Saved -> {
                // TODO(smaskell): Properly set savePaymentMethod/setupFutureUsage
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    selection.paymentMethodId,
                    clientSecret
                )
            }
            is PaymentSelection.New -> {
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    selection.paymentMethodCreateParams,
                    clientSecret,
                    setupFutureUsage = when (shouldSavePaymentMethod) {
                        true -> ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
                        false -> null
                    }
                )
            }
            null -> {
                null
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.takeIf { paymentController.shouldHandlePaymentResult(requestCode, it) }?.let {
            paymentController.handlePaymentResult(
                it,
                object : ApiResultCallback<PaymentIntentResult> {
                    override fun onSuccess(result: PaymentIntentResult) {
                        mutableViewState.value = ViewState.Completed(result)
                    }

                    override fun onError(e: Exception) {
                        this@PaymentSheetViewModel.onError(e)
                    }
                }
            )
        }
    }

    fun isGooglePayReady() = liveData {
        emit(googlePayRepository.isReady().filterNotNull().first())
    }

    @VisibleForTesting
    internal fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        mutablePaymentMethods.postValue(paymentMethods)
    }

    private fun updatePaymentMethods(
        ephemeralKey: String,
        customerId: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        viewModelScope.launch {
            val result = withContext(workContext) {
                runCatching {
                    stripeRepository.getPaymentMethods(
                        ListPaymentMethodsParams(
                            customerId = customerId,
                            paymentMethodType = PaymentMethod.Type.Card
                        ),
                        publishableKey,
                        PRODUCT_USAGE,
                        ApiRequest.Options(ephemeralKey, stripeAccountId)
                    )
                }
            }
            result.fold(
                onSuccess = this@PaymentSheetViewModel::setPaymentMethods,
                onFailure = {
                    onError(it)
                }
            )
        }
    }

    internal enum class TransitionTarget(
        val sheetMode: SheetMode
    ) {
        // User has saved PM's and is selected
        SelectSavedPaymentMethod(SheetMode.Wrapped),

        // User has saved PM's and is adding a new one
        AddPaymentMethodFull(SheetMode.Full),

        // User has no saved PM's
        AddPaymentMethodSheet(SheetMode.FullCollapsed)
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentSheetActivityStarter.Args
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationSupplier()
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val stripeRepository = StripeApiRepository(
                application,
                publishableKey
            )
            val paymentController = StripePaymentController(
                application,
                publishableKey,
                stripeRepository,
                true
            )
            val googlePayRepository = DefaultGooglePayRepository(application)

            return PaymentSheetViewModel(
                publishableKey,
                stripeAccountId,
                stripeRepository,
                paymentController,
                googlePayRepository,
                starterArgsSupplier(),
                Dispatchers.IO
            ) as T
        }
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
