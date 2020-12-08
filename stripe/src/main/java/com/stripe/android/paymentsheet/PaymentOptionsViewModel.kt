package com.stripe.android.paymentsheet

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal class PaymentOptionsViewModel(
    args: PaymentOptionsActivityStarter.Args,
    googlePayRepository: GooglePayRepository
) : SheetViewModel<PaymentOptionsViewModel.TransitionTarget, PaymentOptionViewState>(
    customerConfig = args.config?.customer,
    isGooglePayEnabled = args.config?.googlePay != null,
    googlePayRepository = googlePayRepository
) {
    init {
        mutablePaymentIntent.value = args.paymentIntent
        mutablePaymentMethods.value = args.paymentMethods
    }

    fun selectPaymentOption() {
        selection.value?.let { paymentSelection ->
            mutableViewState.value = PaymentOptionViewState.Completed(paymentSelection)
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
            val googlePayRepository = DefaultGooglePayRepository(
                applicationSupplier(),
                starterArgs.config?.googlePay?.environment ?: PaymentSheet.GooglePayConfiguration.Environment.Test
            )

            return PaymentOptionsViewModel(
                starterArgs,
                googlePayRepository
            ) as T
        }
    }
}
