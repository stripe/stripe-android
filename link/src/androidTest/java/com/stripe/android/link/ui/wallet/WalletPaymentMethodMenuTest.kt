package com.stripe.android.link.ui.wallet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val MOCK_CARD = ConsumerPaymentDetails.Card(
    id = "id1",
    isDefault = true,
    expiryYear = 2032,
    expiryMonth = 12,
    brand = CardBrand.Visa,
    last4 = "4242"
)

private val MOCK_BANK_ACCOUNT = ConsumerPaymentDetails.BankAccount(
    id = "id2",
    isDefault = false,
    bankIconCode = "icon",
    bankName = "Stripe Bank",
    last4 = "6789"
)

@RunWith(AndroidJUnit4::class)
class WalletPaymentMethodMenuTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testCardMenuIsDisplayedCorrectly() {
        composeTestRule.setContent {
            WalletPaymentMethodMenu(
                paymentDetails = MOCK_CARD,
                onEditClick = {},
                onRemoveClick = {},
                onCancelClick = {}
            )
        }

        composeTestRule.onNodeWithText("Update card").assertExists()
        composeTestRule.onNodeWithText("Remove card").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun testBankAccountMenuIsDisplayedCorrectly() {
        composeTestRule.setContent {
            WalletPaymentMethodMenu(
                paymentDetails = MOCK_BANK_ACCOUNT,
                onEditClick = {},
                onRemoveClick = {},
                onCancelClick = {}
            )
        }

        composeTestRule.onNodeWithText("Update card").assertDoesNotExist()
        composeTestRule.onNodeWithText("Remove linked account").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun testUpdateCardWorksCorrectly() {
        val clickRecorder = MockClickRecorder()

        composeTestRule.setContent {
            WalletPaymentMethodMenu(
                paymentDetails = MOCK_CARD,
                onEditClick = clickRecorder::onEditClick,
                onRemoveClick = clickRecorder::onRemoveClick,
                onCancelClick = clickRecorder::onCancelClick
            )
        }

        composeTestRule.onNodeWithText("Update card").performClick()

        assertThat(clickRecorder).isEqualTo(
            MockClickRecorder(editClicked = true)
        )
    }

    @Test
    fun testRemoveCardWorksCorrectly() {
        val clickRecorder = MockClickRecorder()

        composeTestRule.setContent {
            WalletPaymentMethodMenu(
                paymentDetails = MOCK_CARD,
                onEditClick = clickRecorder::onEditClick,
                onRemoveClick = clickRecorder::onRemoveClick,
                onCancelClick = clickRecorder::onCancelClick
            )
        }

        composeTestRule.onNodeWithText("Remove card").performClick()

        assertThat(clickRecorder).isEqualTo(
            MockClickRecorder(removeClicked = true)
        )
    }

    @Test
    fun testCancelWorksCorrectly() {
        val clickRecorder = MockClickRecorder()

        composeTestRule.setContent {
            WalletPaymentMethodMenu(
                paymentDetails = MOCK_CARD,
                onEditClick = clickRecorder::onEditClick,
                onRemoveClick = clickRecorder::onRemoveClick,
                onCancelClick = clickRecorder::onCancelClick
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertThat(clickRecorder).isEqualTo(
            MockClickRecorder(cancelClicked = true)
        )
    }

    private data class MockClickRecorder(
        var editClicked: Boolean = false,
        var removeClicked: Boolean = false,
        var cancelClicked: Boolean = false
    ) {
        fun onEditClick() { editClicked = true }
        fun onRemoveClick() { removeClicked = true }
        fun onCancelClick() { cancelClicked = true }
    }
}
