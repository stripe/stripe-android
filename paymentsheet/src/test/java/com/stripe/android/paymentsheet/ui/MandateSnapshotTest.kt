package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class MandateSnapshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun testHtmlMandateText() {
        paparazziRule.snapshot {
            Mandate(
                mandateText = "By saving your bank account for Merchant you agree to authorize payments pursuant to " +
                    "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    @Test
    fun testLongMandateText() {
        paparazziRule.snapshot {
            Mandate(
                mandateText = "By providing your payment information and confirming this payment, you authorise (A) " +
                    "Merchant and Stripe, our payment service provider, to send instructions to your bank to debit " +
                    "your account and (B) your bank to debit your account in accordance with those instructions. As " +
                    "part of your rights, you are entitled to a refund from your bank under the terms and conditions " +
                    "of your agreement with your bank. A refund must be claimed within 8 weeks starting from the date" +
                    " on which your account was debited. Your rights are explained in a statement that you can obtain" +
                    " from your bank. You agree to receive notifications for future debits up to 2 days before they" +
                    " occur.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
