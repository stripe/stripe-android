package com.stripe.android.paymentsheet.example.playground.customersheet

import androidx.compose.runtime.Stable
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.CustomerSheetPlaygroundSettings
import com.stripe.android.paymentsheet.model.PaymentOption

@Stable
@OptIn(ExperimentalCustomerSheetApi::class)
internal data class CustomerSheetPlaygroundState(
    private val snapshot: CustomerSheetPlaygroundSettings.Snapshot,
    val adapter: CustomerAdapter,
    val optionState: PaymentOptionState,
) {
    sealed interface PaymentOptionState {
        data object Unloaded : PaymentOptionState

        data class Loaded(val paymentOption: PaymentOption?) : PaymentOptionState
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun customerSheetConfiguration(): CustomerSheet.Configuration {
        return snapshot.customerSheetConfiguration(this)
    }
}
