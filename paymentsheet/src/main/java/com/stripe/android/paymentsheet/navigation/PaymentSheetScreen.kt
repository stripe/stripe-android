package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsAddPmBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsListBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetAddPmBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetListBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetLoadingBinding

internal sealed interface PaymentSheetScreen {

    @Composable
    fun PaymentSheetContent()

    @Composable
    fun PaymentOptionsContent()

    object SelectSavedPaymentMethods : PaymentSheetScreen {

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

@Composable
internal fun PaymentSheetScreen?.PaymentSheetContent() {
    if (this == null) {
        AndroidViewBinding(FragmentPaymentsheetLoadingBinding::inflate)
    } else {
        PaymentSheetContent()
    }
}

@Composable
internal fun PaymentSheetScreen?.PaymentOptionsContent() {
    if (this == null) {
        AndroidViewBinding(FragmentPaymentsheetLoadingBinding::inflate)
    } else {
        PaymentOptionsContent()
    }
}

private val PaymentSheetScreen.testTag: String
    get() = this::class.java.simpleName
