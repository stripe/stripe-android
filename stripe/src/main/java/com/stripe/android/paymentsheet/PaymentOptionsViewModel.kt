package com.stripe.android.paymentsheet

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal class PaymentOptionsViewModel(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val args: PaymentOptionsActivityStarter.Args,
    googlePayRepository: GooglePayRepository
) : SheetViewModel<PaymentOptionsViewModel.TransitionTarget, PaymentOptionViewState>(
    isGuestMode = args is PaymentOptionsActivityStarter.Args.Guest,
    googlePayRepository = googlePayRepository
) {
    init {
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
            val application = applicationSupplier()
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val googlePayRepository = DefaultGooglePayRepository(application)

            return PaymentOptionsViewModel(
                publishableKey,
                stripeAccountId,
                starterArgsSupplier(),
                googlePayRepository
            ) as T
        }
    }
}
