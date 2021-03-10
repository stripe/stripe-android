package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentOptionsViewModel(
    args: PaymentOptionContract.Args,
    prefsRepository: PrefsRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val eventReporter: EventReporter,
    workContext: CoroutineContext
) : BaseSheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    config = args.config,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    @VisibleForTesting
    internal val _viewState = MutableLiveData<ViewState.PaymentOptions>(
        ViewState.PaymentOptions.Ready
    )
    internal val viewState: LiveData<ViewState.PaymentOptions> = _viewState.distinctUntilChanged()

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newCard = args.newCard

    // This is used in the case where the last card was new and not saved. In this scenario
    // when the payment options is opened it should jump to the add card, but if the user
    // presses the back button, they shouldn't transition to it again
    private var hasTransitionToUnsavedCard = false
    private val shouldTransitionToUnsavedCard: Boolean
        get() =
            !hasTransitionToUnsavedCard &&
                (newCard as? PaymentSelection.New)?.let { !it.shouldSavePaymentMethod } ?: false

    init {
        _isGooglePayReady.value = args.isGooglePayReady
        _paymentIntent.value = args.paymentIntent
        _paymentMethods.value = args.paymentMethods
        _processing.postValue(false)
    }

    fun onUserSelection() {
        selection.value?.let { paymentSelection ->
            // TODO: Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            val requestSaveNewCard =
                (paymentSelection as? PaymentSelection.New)?.shouldSavePaymentMethod
                    ?: false
            if (requestSaveNewCard) {
                processSaveNewCard(paymentSelection)
            } else {
                processUnsavedNewCard(paymentSelection)
            }
        }
    }

    private fun processUnsavedNewCard(paymentSelection: PaymentSelection) {
        _viewState.value = ViewState.PaymentOptions.Ready
        prefsRepository.savePaymentSelection(paymentSelection)
        _viewState.value = ViewState.PaymentOptions.ProcessResult(
            PaymentOptionResult.Succeeded(paymentSelection)
        )
    }

    private fun processSaveNewCard(paymentSelection: PaymentSelection) {
        _viewState.value = ViewState.PaymentOptions.StartProcessing
        savePaymentSelection(
            paymentSelection as PaymentSelection.New,
            onSuccess = { paymentMethod ->
                // If the payment method is null we will just ignore the fact that the save
                // didn't happen, the important thing is that the card is saved and now usable
                prefsRepository.savePaymentSelection(PaymentSelection.Saved(paymentMethod))

                _viewState.value = ViewState.PaymentOptions.FinishProcessing {
                    _viewState.value = ViewState.PaymentOptions.ProcessResult(
                        PaymentOptionResult.Succeeded(paymentSelection)
                    )
                }
            },
            onFailure = {
                // TODO(michelleb-stripe): Handle failure cases
            }
        )
    }

    private fun savePaymentSelection(
        paymentSelection: PaymentSelection.New,
        onSuccess: (PaymentMethod) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(workContext) {
                    paymentMethodsRepository.save(
                        customerConfig!!,
                        paymentSelection.paymentMethodCreateParams
                    )
                }
            }.fold(onSuccess, onFailure)
        }
    }

    fun getPaymentOptionResult(): PaymentOptionResult {
        return selection.value?.let {
            PaymentOptionResult.Succeeded(it)
        } ?: PaymentOptionResult.Canceled(
            mostRecentError = fatal.value
        )
    }

    fun resolveTransitionTarget(config: FragmentConfig) {
        if (shouldTransitionToUnsavedCard) {
            hasTransitionToUnsavedCard = true
            transitionTo(
                // Until we add a flag to the transitionTarget to specify if we want to add the item
                // to the backstack, we need to use the full sheet.
                TransitionTarget.AddPaymentMethodFull(config)
            )
        }
    }

    internal sealed class TransitionTarget {
        abstract val fragmentConfig: FragmentConfig
        abstract val sheetMode: SheetMode

        // User has saved PM's and is selected
        data class SelectSavedPaymentMethod(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget() {
            override val sheetMode = SheetMode.Wrapped
        }

        // User has saved PM's and is adding a new one
        data class AddPaymentMethodFull(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget() {
            override val sheetMode = SheetMode.Full
        }

        // User has no saved PM's
        data class AddPaymentMethodSheet(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget() {
            override val sheetMode = SheetMode.FullCollapsed
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentOptionContract.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val starterArgs = starterArgsSupplier()
            val application = applicationSupplier()
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val stripeRepository = StripeApiRepository(
                application,
                publishableKey
            )

            val prefsRepository = starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    application,
                    customerId = id,
                    isGooglePayReady = { starterArgs.isGooglePayReady },
                    workContext = Dispatchers.IO
                )
            } ?: PrefsRepository.Noop()

            val paymentMethodsRepository = PaymentMethodsRepository.Api(
                stripeRepository = stripeRepository,
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                workContext = Dispatchers.IO
            )

            return PaymentOptionsViewModel(
                starterArgs,
                prefsRepository,
                paymentMethodsRepository,
                DefaultEventReporter(
                    mode = EventReporter.Mode.Custom,
                    starterArgs.sessionId,
                    application
                ),
                workContext = Dispatchers.IO
            ) as T
        }
    }
}
