package com.stripe.android.paymentsheet

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.ui.SheetMode

internal class PaymentOptionsViewModel(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val args: PaymentOptionsActivityStarter.Args,
) : ViewModel() {
    private val mutableError = MutableLiveData<Throwable>()
    private val mutableSheetMode = MutableLiveData<SheetMode>()

    internal val error: LiveData<Throwable> = mutableError
    internal val sheetMode: LiveData<SheetMode> = mutableSheetMode.distinctUntilChanged()

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentOptionsActivityStarter.Args
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationSupplier()
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId

            return PaymentOptionsViewModel(
                publishableKey,
                stripeAccountId,
                starterArgsSupplier()
            ) as T
        }
    }
}
