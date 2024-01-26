package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class GooglePayButtonTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            composeTestRule.activity.applicationContext,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `when state is null, should show the Google Pay Button`() {
        composeTestRule.setContent {
            GooglePayButton(
                state = null,
                allowCreditCards = true,
                buttonType = GooglePayButtonType.Pay,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                isEnabled = true,
                onPressed = {}
            )
        }

        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `when state is 'Ready', should show the Google Pay Button`() {
        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                allowCreditCards = true,
                buttonType = GooglePayButtonType.Pay,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                isEnabled = true,
                onPressed = {}
            )
        }

        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `when state is 'StartProcessing, should show the Google Pay Button`() {
        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.StartProcessing,
                allowCreditCards = true,
                buttonType = GooglePayButtonType.Pay,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                isEnabled = true,
                onPressed = {}
            )
        }

        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG).assertExists()
    }

    @Test
    fun `when state is 'FinishProcessing', should show the Google Pay Button`() {
        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.FinishProcessing {},
                allowCreditCards = true,
                buttonType = GooglePayButtonType.Pay,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                isEnabled = true,
                onPressed = {}
            )
        }

        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG).assertExists()
    }
}
