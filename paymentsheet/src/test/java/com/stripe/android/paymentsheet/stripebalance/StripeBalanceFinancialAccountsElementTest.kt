package com.stripe.android.paymentsheet.stripebalance

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class StripeBalanceFinancialAccountsElementTest {

    @Test
    fun `is incomplete until an account is selected`() = runTest {
        val element = createElement()

        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactly(fieldValue(null))

            element.select(SELECTABLE_ACCOUNT)

            assertThat(awaitItem()).containsExactly(fieldValue("fa_selectable"))
        }
    }

    @Test
    fun `doesn't select accounts that can't cover the payment`() = runTest {
        val element = createElement()

        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactly(fieldValue(null))

            element.select(INSUFFICIENT_ACCOUNT)

            expectNoEvents()
        }
    }

    @Test
    fun `restores the previous choice if it's still selectable`() = runTest {
        val element = createElement(initialFinancialAccountId = "fa_selectable")

        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactly(fieldValue("fa_selectable"))
        }
    }

    @Test
    fun `doesn't restore a previous choice that can't cover the payment`() = runTest {
        val element = createElement(initialFinancialAccountId = "fa_insufficient")

        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactly(fieldValue(null))
        }
    }

    private fun createElement(initialFinancialAccountId: String? = null) = StripeBalanceFinancialAccountsElement(
        financialAccounts = StripeBalanceFinancialAccounts(
            accounts = listOf(SELECTABLE_ACCOUNT, INSUFFICIENT_ACCOUNT),
            currency = "usd",
        ),
        initialFinancialAccountId = initialFinancialAccountId,
    )

    private fun fieldValue(financialAccountId: String?) =
        StripeBalanceFinancialAccountsElement.FINANCIAL_ACCOUNT_FIELD_ID to FormFieldEntry(
            value = financialAccountId,
            isComplete = financialAccountId != null,
        )

    private companion object {
        val SELECTABLE_ACCOUNT = StripeBalanceFinancialAccount(
            id = "fa_selectable",
            displayName = "Operating account",
            availableBalances = mapOf("usd" to 10_000L),
            eligible = true,
            ineligibleReason = null,
        )

        val INSUFFICIENT_ACCOUNT = StripeBalanceFinancialAccount(
            id = "fa_insufficient",
            displayName = "Reserve",
            availableBalances = mapOf("usd" to 100L),
            eligible = false,
            ineligibleReason = StripeBalanceFinancialAccount.IneligibleReason.InsufficientBalance,
        )
    }
}
