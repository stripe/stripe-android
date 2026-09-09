@file:OptIn(
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutSummaryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `shipping address required shows pending tax message`() = runScenario(
        taxStatus = Session.Tax.Status.RequiresShippingAddress,
    ) {
        page.pendingTax.assertIsDisplayed()
        page.pendingTax.assertTextContains("Tax")
        page.pendingTax.assertTextContains("Enter shipping address to calculate")
        page.calculatedTax.assertDoesNotExist()
    }

    @Test
    fun `billing address required shows pending tax message`() = runScenario(
        taxStatus = Session.Tax.Status.RequiresBillingAddress,
    ) {
        page.pendingTax.assertIsDisplayed()
        page.pendingTax.assertTextContains("Tax")
        page.pendingTax.assertTextContains("Enter billing address to calculate")
        page.calculatedTax.assertDoesNotExist()
    }

    @Test
    fun `ready tax shows calculated tax rows`() = runScenario(
        taxStatus = Session.Tax.Status.Ready,
    ) {
        page.pendingTax.assertDoesNotExist()
        page.calculatedTax.assertIsDisplayed()
    }

    @Test
    fun `unknown tax shows calculated tax rows`() = runScenario(
        taxStatus = Session.Tax.Status.Unknown,
    ) {
        page.pendingTax.assertDoesNotExist()
        page.calculatedTax.assertIsDisplayed()
    }

    private fun runScenario(
        taxStatus: Session.Tax.Status,
        block: Scenario.() -> Unit,
    ) {
        composeRule.setContent {
            TaxSummaryRows(taxStatus = taxStatus) {
                Text(CALCULATED_TAX_TEXT)
            }
        }

        block(Scenario(page = CheckoutSummaryPage(composeRule)))
    }

    private data class Scenario(
        val page: CheckoutSummaryPage,
    )
}

private class CheckoutSummaryPage(
    composeRule: ComposeContentTestRule,
) {
    val pendingTax = composeRule.onNodeWithTag(PENDING_TAX_TEST_TAG)
    val calculatedTax = composeRule.onNodeWithText(CALCULATED_TAX_TEXT)
}

private const val CALCULATED_TAX_TEXT = "Calculated tax"
