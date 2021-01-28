package com.stripe.android.paymentsheet

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import kotlinx.coroutines.Dispatchers

internal class PaymentOptionsViewModel(
    args: PaymentOptionContract.Args,
    googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter
) : SheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    config = args.config,
    isGooglePayEnabled = args.config?.googlePay != null,
    googlePayRepository = googlePayRepository,
    prefsRepository = prefsRepository
) {
    private val _userSelection = MutableLiveData<PaymentSelection>()
    val userSelection: LiveData<PaymentSelection> = _userSelection

    init {
        _paymentIntent.value = args.paymentIntent
        _paymentMethods.value = args.paymentMethods
        _processing.postValue(false)
    }

    fun onUserSelection() {
        selection.value?.let { paymentSelection ->
            eventReporter.onSelectPaymentOption(paymentSelection)
            prefsRepository.savePaymentSelection(paymentSelection)
            _userSelection.value = paymentSelection
        }
    }

    fun getPaymentOptionResult(): PaymentOptionResult {
        return selection.value?.let {
            PaymentOptionResult.Succeeded(it)
        } ?: PaymentOptionResult.Canceled(
            mostRecentError = fatal.value
        )
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
        private val starterArgsSupplier: () -> PaymentOptionContract.Args
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val starterArgs = starterArgsSupplier()
            val application = applicationSupplier()
            val googlePayRepository = starterArgs.config?.googlePay?.environment?.let { environment ->
                DefaultGooglePayRepository(
                    application,
                    environment
                )
            } ?: GooglePayRepository.Disabled()

            val prefsRepository = starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    application,
                    customerId = id,
                    googlePayRepository = googlePayRepository,
                    workContext = Dispatchers.IO
                )
            } ?: PrefsRepository.Noop()

            return PaymentOptionsViewModel(
                starterArgs,
                googlePayRepository,
                prefsRepository,
                DefaultEventReporter(
                    mode = EventReporter.Mode.Custom,
                    starterArgs.sessionId,
                    application
                )
            ) as T
        }
    }
}
