package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.testing.PaymentMethodFactory
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
    fun `Navigates to AddAnotherPaymentMethod screen when add card is pressed`() {
        testNavigatesToAddAnotherPaymentMethodWhenAddCardIsPressed(useUpdatePaymentMethodScreen = false)
    }

    @Test
    fun `Navigates to AddAnotherPaymentMethod screen when add card is pressed using new updatePM screen`() {
        testNavigatesToAddAnotherPaymentMethodWhenAddCardIsPressed(useUpdatePaymentMethodScreen = true)
    }

    private fun testNavigatesToAddAnotherPaymentMethodWhenAddCardIsPressed(useUpdatePaymentMethodScreen: Boolean) {
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
                onItemRemoved = {},
                useUpdatePaymentMethodScreen = useUpdatePaymentMethodScreen,
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
    fun `Updates selection when item is pressed`() {
        testUpdatesSelectionWhenItemIsPressed(useUpdatePaymentMethodScreen = false)
    }

    @Test
    fun `Updates selection when item is pressed using updatePM screen`() {
        testUpdatesSelectionWhenItemIsPressed(useUpdatePaymentMethodScreen = true)
    }

    private fun testUpdatesSelectionWhenItemIsPressed(useUpdatePaymentMethodScreen: Boolean) {
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
                onItemRemoved = {},
                useUpdatePaymentMethodScreen = useUpdatePaymentMethodScreen,
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
    fun `Does not update selection when item is pressed in edit mode`() {
        testDoesNotUpdateSelectWhenItemIsPressedInEditMode(useUpdatePaymentMethodScreen = false)
    }

    @Test
    fun `Does not update selection when item is pressed in edit mode, using updatePM screen`() {
        testDoesNotUpdateSelectWhenItemIsPressedInEditMode(useUpdatePaymentMethodScreen = true)
    }

    private fun testDoesNotUpdateSelectWhenItemIsPressedInEditMode(useUpdatePaymentMethodScreen: Boolean) {
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
                onItemRemoved = {},
                useUpdatePaymentMethodScreen = useUpdatePaymentMethodScreen,
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
    fun `If saved payment methods are disabled, all tabs should be disabled as well`() {
        composeTestRule.setContent {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.SavedPaymentMethod(
                        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod(
                            displayName = resolvableString("4242"),
                            paymentMethod = createCard(last4 = "4242"),
                        ),
                        canRemovePaymentMethods = false,
                    ),
                    PaymentOptionsItem.SavedPaymentMethod(
                        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod(
                            displayName = resolvableString("5555"),
                            paymentMethod = createCard(last4 = "5555"),
                        ),
                        canRemovePaymentMethods = false,
                    )
                ),
                selectedPaymentOptionsItem = PaymentOptionsItem.GooglePay,
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
                useUpdatePaymentMethodScreen = false,
            )
        }

        composeTestRule.onTab(last4 = "4242").assertIsNotEnabled()
        composeTestRule.onTab(last4 = "5555").assertIsNotEnabled()
    }

    @Test
    fun `If is editing and using update payment method screen, tabs should be enabled`() {
        // This isn't a realistic scenario -- usually a user would not be able to get into "edit" mode unless at least
        // one card was also modifiable.
        composeTestRule.setContent {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.SavedPaymentMethod(
                        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod(
                            displayName = resolvableString("4242"),
                            paymentMethod = createCard(last4 = "4242"),
                        ),
                        canRemovePaymentMethods = false,
                    ),
                    PaymentOptionsItem.SavedPaymentMethod(
                        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod(
                            displayName = resolvableString("5555"),
                            paymentMethod = createCard(last4 = "5555"),
                        ),
                        canRemovePaymentMethods = false,
                    )
                ),
                selectedPaymentOptionsItem = PaymentOptionsItem.GooglePay,
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
                useUpdatePaymentMethodScreen = true,
            )
        }

        composeTestRule.onTab(last4 = "4242").assertIsEnabled()
        composeTestRule.onTab(last4 = "5555").assertIsEnabled()
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

    private fun createCard(last4: String): PaymentMethod {
        return PaymentMethodFactory.card(random = true).run {
            copy(
                card = card?.copy(
                    last4 = last4,
                )
            )
        }
    }

    private fun ComposeTestRule.onTab(last4: String): SemanticsNodeInteraction {
        return onNode(hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG).and(hasText(last4, substring = true)))
    }
}
