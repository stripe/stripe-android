package com.stripe.android.paymentsheet

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal class PaymentOptionsViewModel(
    args: PaymentOptionsActivityStarter.Args,
    googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter
) : SheetViewModel<PaymentOptionsViewModel.TransitionTarget, PaymentOptionViewState>(
    config = args.config,
    isGooglePayEnabled = args.config?.googlePay != null,
    googlePayRepository = googlePayRepository,
    prefsRepository = prefsRepository
) {
    init {
        _paymentIntent.value = args.paymentIntent
        _paymentMethods.value = args.paymentMethods
        _processing.postValue(false)
    }

    fun selectPaymentOption() {
        selection.value?.let { paymentSelection ->
            eventReporter.onSelectPaymentOption(paymentSelection)
            prefsRepository.savePaymentSelection(paymentSelection)
            _viewState.value = PaymentOptionViewState.Completed(paymentSelection)
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
        private val starterArgsSupplier: () -> PaymentOptionsActivityStarter.Args
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val starterArgs = starterArgsSupplier()
            val application = applicationSupplier()
            val googlePayRepository = DefaultGooglePayRepository(
                application,
                starterArgs.config?.googlePay?.environment ?: PaymentSheet.GooglePayConfiguration.Environment.Test
            )

            val prefsRepository = starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    customerId = id,
                    PaymentSessionPrefs.Default(application)
                )
            } ?: PrefsRepository.Noop()

            return PaymentOptionsViewModel(
                starterArgs,
                googlePayRepository,
                prefsRepository,
                DefaultEventReporter(
                    mode = EventReporter.Mode.Custom,
                    application
                )
            ) as T
        }
    }
}
