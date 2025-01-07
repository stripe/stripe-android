package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.TestFactory
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class WalletPaymentMethodMenuScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testCard() {
        snapshot(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    isDefault = false
                )
        )
    }

    @Test
    fun testCardAsDefault() {
        snapshot(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
                .copy(
                    isDefault = true
                )
        )
    }

    @Test
    fun testBankAccount() {
        snapshot(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                .copy(
                    isDefault = false
                )
        )
    }

    @Test
    fun testBankAccountAsDefault() {
        snapshot(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
                .copy(
                    isDefault = true
                )
        )
    }

    @Test
    fun testPassthrough() {
        snapshot(TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH)
    }

    private fun snapshot(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails
    ) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                WalletPaymentMethodMenu(
                    paymentDetails = paymentDetails,
                    onEditClick = {},
                    onCancelClick = {},
                    onRemoveClick = {},
                    onSetDefaultClick = {}
                )
            }
        }
    }
}
