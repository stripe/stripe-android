package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetLoading
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean

    @Composable
    fun Content(viewModel: BaseSheetViewModel)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel) {
            PaymentSheetLoading()
        }
    }

    object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel) {
            PaymentOptions(viewModel)
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel) {
            AddPaymentMethod(viewModel)
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel) {
            AddPaymentMethod(viewModel)
        }
    }
}
