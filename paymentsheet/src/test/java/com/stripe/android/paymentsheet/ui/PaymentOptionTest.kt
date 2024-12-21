package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
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

        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                editState = PaymentOptionEditState.None,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule
            .onNodeWithText(label)
            .onParent()
            .assertContentDescriptionEquals("Card ending in 4 2 4 2 ")
    }

    @Test
    fun `Default Label not shown for non default payment method that is not being edited`() {
        val label = "Card ending in 4242"

        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowDefaultBadge = false,
                editState = PaymentOptionEditState.None,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL).assertDoesNotExist()
    }

    @Test
    fun `Default Label not shown for non default payment method that is being edited`() {
        val label = "Card ending in 4242"

        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowDefaultBadge = false,
                editState = PaymentOptionEditState.Modifiable,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL).assertDoesNotExist()
    }

    @Test
    fun `Default Label not shown for default payment method that isn't being edited`() {
        val label = "Card ending in 4242"

        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowDefaultBadge = true,
                editState = PaymentOptionEditState.None,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL).assertDoesNotExist()
    }

    @Test
    fun `Default Label shown for default payment method that is being edited`() {
        val label = "Card ending in 4242"

        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowDefaultBadge = true,
                editState = PaymentOptionEditState.Modifiable,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = label,
                description = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL).assertExists()
    }
}
