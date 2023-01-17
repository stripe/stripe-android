package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsAddPmBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsListBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetAddPmBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetListBinding
import com.stripe.android.paymentsheet.ui.PaymentSheetLoading

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean

    @Composable
    fun PaymentSheetContent()

    @Composable
    fun PaymentOptionsContent()

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false

        @Composable
        override fun PaymentSheetContent() {
            PaymentSheetLoading()
        }

        @Composable
        override fun PaymentOptionsContent() {
            PaymentSheetLoading()
        }
    }

    object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent() {
            AndroidViewBinding(
                factory = FragmentPaymentSheetListBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }

        @Composable
        override fun PaymentOptionsContent() {
            AndroidViewBinding(
                factory = FragmentPaymentOptionsListBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent() {
            AndroidViewBinding(
                factory = FragmentPaymentSheetAddPmBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }

        @Composable
        override fun PaymentOptionsContent() {
            AndroidViewBinding(
                factory = FragmentPaymentOptionsAddPmBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true

        @Composable
        override fun PaymentSheetContent() {
            AndroidViewBinding(
                factory = FragmentPaymentSheetAddPmBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }

        @Composable
        override fun PaymentOptionsContent() {
            AndroidViewBinding(
                factory = FragmentPaymentOptionsAddPmBinding::inflate,
                modifier = Modifier.testTag(testTag),
            )
        }
    }
}

private val PaymentSheetScreen.testTag: String
    get() = this::class.java.simpleName
