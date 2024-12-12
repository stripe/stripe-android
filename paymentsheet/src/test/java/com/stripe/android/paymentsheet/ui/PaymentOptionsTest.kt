package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentOptionsItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S])
class PaymentOptionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Navigates to AddAnotherPaymentMethod screen when add card is pressed using new updatePM screen`() {
        var didCallOnAddCardPressed = false

        composeTestRule.setContent {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(PaymentOptionsItem.AddCard, PaymentOptionsItem.GooglePay),
                selectedPaymentOptionsItem = PaymentOptionsItem.GooglePay,
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = { didCallOnAddCardPressed = true },
                onItemSelected = {},
                onModifyItem = {},
            )
        }

        val label = "+ Add"
        val testTag = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$label"

        assertThat(didCallOnAddCardPressed).isFalse()

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        assertThat(didCallOnAddCardPressed).isTrue()
    }

    @Test
    fun `Updates selection when item is pressed using updatePM screen`() {
        var didCallOnItemSelected = false

        composeTestRule.setContent {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(PaymentOptionsItem.AddCard, PaymentOptionsItem.GooglePay),
                selectedPaymentOptionsItem = PaymentOptionsItem.GooglePay,
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = { didCallOnItemSelected = true },
                onModifyItem = {},
            )
        }

        val testTag = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay"

        assertThat(didCallOnItemSelected).isFalse()

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        assertThat(didCallOnItemSelected).isTrue()
    }

    @Test
    fun `Does not update selection when item is pressed in edit mode, using updatePM screen`() {
        var didCallOnItemSelected = false

        composeTestRule.setContent {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(PaymentOptionsItem.AddCard, PaymentOptionsItem.GooglePay),
                selectedPaymentOptionsItem = PaymentOptionsItem.GooglePay,
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = { didCallOnItemSelected = true },
                onModifyItem = {},
            )
        }

        val testTag = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay"

        assertThat(didCallOnItemSelected).isFalse()

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        assertThat(didCallOnItemSelected).isFalse()
    }

    @Test
    fun `When width is 320dp, calculates item width of 114dp`() {
        composeTestRule.setContent {
            val itemWidth = rememberItemWidth(maxWidth = 320.dp)
            assertThat(itemWidth.value.roundToInt()).isEqualTo(114)
        }
    }

    @Test
    fun `When width is 481dp, calculates item width of 128dp`() {
        composeTestRule.setContent {
            val itemWidth = rememberItemWidth(maxWidth = 481.dp)
            assertThat(itemWidth.value.roundToInt()).isEqualTo(128)
        }
    }

    @Test
    fun `When width is 482dp, calculates item width of 112dp`() {
        composeTestRule.setContent {
            val itemWidth = rememberItemWidth(maxWidth = 482.dp)
            assertThat(itemWidth.value.roundToInt()).isEqualTo(112)
        }
    }
}
