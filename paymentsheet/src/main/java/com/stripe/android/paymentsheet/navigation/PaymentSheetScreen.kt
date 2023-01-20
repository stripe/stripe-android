package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsListBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetListBinding
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
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
            AndroidViewBinding(
                factory = FragmentPaymentSheetListBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }

        @Composable
        override fun PaymentOptionsContent(args: PaymentOptionContract.Args) {
            AndroidViewBinding(
                factory = FragmentPaymentOptionsListBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
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

private val PaymentSheetScreen.testTag: String
    get() = this::class.java.simpleName
