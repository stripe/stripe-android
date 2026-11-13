package com.stripe.android.paymentsheet.stripebalance

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class StripeBalanceFinancialAccountsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun testAccounts() {
        paparazziRule.snapshot {
            StripeBalanceFinancialAccountsUI(
                accounts = listOf(
                    account(id = "fa_operating", displayName = "Operating account", balance = 250_000L),
                    account(id = "fa_test_1234", displayName = null, balance = 12_050L),
                    account(
                        id = "fa_reserve",
                        displayName = "Reserve",
                        balance = 2_500L,
                        ineligibleReason = StripeBalanceFinancialAccount.IneligibleReason.InsufficientBalance,
                    ),
                    StripeBalanceFinancialAccount(
                        id = "fa_gbp",
                        displayName = "UK account",
                        availableBalances = mapOf("gbp" to 100_000L),
                        eligible = false,
                        ineligibleReason = StripeBalanceFinancialAccount.IneligibleReason.CurrencyMismatch,
                    ),
                ),
                currency = "usd",
                selectedFinancialAccountId = "fa_operating",
                enabled = true,
                onSelect = {},
            )
        }
    }

    @Test
    fun testNoAccounts() {
        paparazziRule.snapshot {
            StripeBalanceFinancialAccountsUI(
                accounts = emptyList(),
                currency = "usd",
                selectedFinancialAccountId = null,
                enabled = true,
                onSelect = {},
            )
        }
    }

    private fun account(
        id: String,
        displayName: String?,
        balance: Long,
        ineligibleReason: StripeBalanceFinancialAccount.IneligibleReason? = null,
    ) = StripeBalanceFinancialAccount(
        id = id,
        displayName = displayName,
        availableBalances = mapOf("usd" to balance),
        eligible = ineligibleReason == null,
        ineligibleReason = ineligibleReason,
    )
}
