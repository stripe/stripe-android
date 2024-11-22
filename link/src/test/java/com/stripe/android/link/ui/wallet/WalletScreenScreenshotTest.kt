package com.stripe.android.link.ui.wallet

import com.stripe.android.link.TestFactory
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class WalletScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testEmptyState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = emptyList(),
                selectedItem = null,
                isProcessing = false,
                isExpanded = false
            )
        )
    }

    @Test
    fun testCollapsedState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
                isProcessing = false,
                isExpanded = false
            )
        )
    }

    @Test
    fun testExpandedState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
                isProcessing = false,
                isExpanded = true
            )
        )
    }

    @Test
    fun testBankAccountSelectedState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull {
                    it is ConsumerPaymentDetails.BankAccount
                },
                isProcessing = false,
                isExpanded = true
            )
        )
    }

    private fun snapshot(
        state: WalletUiState
    ) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                WalletBody(
                    state = state,
                    onItemSelected = {},
                    onExpandedChanged = {}
                )
            }
        }
    }
}
