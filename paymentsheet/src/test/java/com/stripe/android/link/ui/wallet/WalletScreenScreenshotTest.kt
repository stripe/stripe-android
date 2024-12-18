package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
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
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
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
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
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
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonCompletedState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
                isProcessing = false,
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonProcessingState() {
        snapshot(
            state = WalletUiState(
                paymentDetailsList = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
                selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
                isProcessing = true,
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonDisabledStateDueToExpiredCard() {
        val paymentDetailsList = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD.copy(
                expiryYear = 1999
            ),
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
        snapshot(
            state = WalletUiState(
                paymentDetailsList = paymentDetailsList,
                selectedItem = paymentDetailsList.firstOrNull(),
                isProcessing = false,
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
        )
    }

    @Test
    fun testPayButtonDisabledStateDueToCvcCheck() {
        val paymentDetailsList = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD.copy(
                cvcCheck = CvcCheck.Fail
            ),
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
        snapshot(
            state = WalletUiState(
                paymentDetailsList = paymentDetailsList,
                selectedItem = paymentDetailsList.firstOrNull(),
                isProcessing = false,
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
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
                hasCompleted = false,
                primaryButtonLabel = primaryButtonLabel
            ),
            isExpanded = true
        )
    }

    private fun snapshot(
        state: WalletUiState,
        isExpanded: Boolean = false
    ) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                WalletBody(
                    state = state,
                    isExpanded = isExpanded,
                    onItemSelected = {},
                    onExpandedChanged = {},
                    onPrimaryButtonClick = {},
                    onPayAnotherWayClick = {}
                )
            }
        }
    }

    companion object {
        private val primaryButtonLabel = "Pay $50".resolvableString
    }
}
