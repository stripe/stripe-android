package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetLoading

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean

    @Composable
    fun PaymentSheetContent(args: PaymentSheetContract.Args)

    @Composable
    fun PaymentOptionsContent(args: PaymentOptionContract.Args)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false

        @Composable
        override fun PaymentSheetContent(args: PaymentSheetContract.Args) {
            PaymentSheetLoading()
        }

        @Composable
        override fun PaymentOptionsContent(args: PaymentOptionContract.Args) {
            PaymentSheetLoading()
        }
    }

    object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent(args: PaymentSheetContract.Args) {
            PaymentOptions(args = args)
        }

        @Composable
        override fun PaymentOptionsContent(args: PaymentOptionContract.Args) {
            PaymentOptions(args = args)
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent(args: PaymentSheetContract.Args) {
            AddPaymentMethod(args = args)
        }

        @Composable
        override fun PaymentOptionsContent(args: PaymentOptionContract.Args) {
            AddPaymentMethod(args = args)
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent(args: PaymentSheetContract.Args) {
            AddPaymentMethod(args = args)
        }

        @Composable
        override fun PaymentOptionsContent(args: PaymentOptionContract.Args) {
            AddPaymentMethod(args = args)
        }
    }
}
