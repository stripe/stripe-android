package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetLoading
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean

    @Composable
    fun Content(viewModel: BaseSheetViewModel, modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            PaymentSheetLoading()
        }
    }

    object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            PaymentOptions(
                viewModel = viewModel,
                modifier = modifier,
            )
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }
}

@Composable
internal fun PaymentSheetScreen.Content(viewModel: BaseSheetViewModel) {
    Content(viewModel, modifier = Modifier)
}
