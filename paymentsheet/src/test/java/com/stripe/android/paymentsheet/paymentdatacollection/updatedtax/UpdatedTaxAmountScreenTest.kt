package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItem
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpdatedTaxAmountScreenTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(CoroutineTestRule())

    @Test
    fun `shows updated line items and confirms`() {
        var confirmed = false
        composeRule.setContent {
            StripeTheme {
                UpdatedTaxAmountScreen(
                    displayItems = listOf(
                        GooglePayDisplayItem(
                            label = "Widget".resolvableString,
                            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                            price = 1000L,
                        ),
                        GooglePayDisplayItem(
                            label = "Estimated total".resolvableString,
                            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                            price = 1200L,
                        ),
                    ),
                    currency = "usd",
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Confirm updated total").assertIsDisplayed()
        composeRule.onNodeWithText("Widget").assertIsDisplayed()
        composeRule.onNodeWithText("Estimated total").assertIsDisplayed()
        composeRule.onNodeWithTag(UPDATED_TAX_AMOUNT_CONFIRM_BUTTON).performClick()

        assertThat(confirmed).isTrue()
    }
}
