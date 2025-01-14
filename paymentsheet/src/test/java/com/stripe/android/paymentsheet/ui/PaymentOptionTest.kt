package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentOptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Turns card label into screen reader-friendly text`() {
        val label = "Card ending in 4242"

        setSavedPaymentMethodTabComposeTestRule(
            label = label,
            shouldShowDefaultBadge = false,
        )
        composeTestRule
            .onNodeWithText(label)
            .assertContentDescriptionEquals("Card ending in 4 2 4 2 ")
    }

    @Test
    fun `Correctly hides default badge when not default`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
        )
        composeTestRule.onNodeWithTag(
            TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `Correctly shows default badge when default`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = true,
        )

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
            useUnmergedTree = true
        ).assertExists()
    }

    private fun setSavedPaymentMethodTabComposeTestRule(
        label: String = "Card ending in 4242",
        shouldShowDefaultBadge: Boolean,
    ) {
        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = shouldShowDefaultBadge,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }
    }
}
