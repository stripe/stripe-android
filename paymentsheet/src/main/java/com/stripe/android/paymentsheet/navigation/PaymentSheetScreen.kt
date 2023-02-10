package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R
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
            PaymentOptions(
                viewModel = viewModel,
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.stripe_paymentsheet_paymentoptions_margin_top),
                    bottom = dimensionResource(R.dimen.stripe_paymentsheet_paymentoptions_margin_bottom),
                )
            )
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
