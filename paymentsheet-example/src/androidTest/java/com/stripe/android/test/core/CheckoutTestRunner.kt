package com.stripe.android.test.core

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.example.playground.checkout.CHECKOUT_COMPLETED_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.checkout.CHECKOUT_CONFIRMATION_MESSAGE_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.checkout.CHECKOUT_CONFIRM_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.checkout.CHECKOUT_CREATE_SESSION_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutElementsControllerExampleActivity
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.paymentelementtestpages.FormPage

/**
 * Runs [block] in [CheckoutElementsControllerExampleActivity] end to end against the playground backend.
 *
 * @param settings applied on top of the example's defaults, not the device's saved settings.
 */
internal fun runCheckoutTest(
    composeTestRule: ComposeTestRule,
    settings: CheckoutPlaygroundSettings.() -> Unit,
    block: CheckoutTestContext.() -> Unit,
) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val checkoutSettings = CheckoutPlaygroundSettings.createInMemory(json = null).apply(settings)
    ActivityScenario.launch<CheckoutElementsControllerExampleActivity>(
        CheckoutElementsControllerExampleActivity.createTestIntent(context, checkoutSettings.asJsonString())
    ).use {
        val createSessionButton = ComposeButton(composeTestRule, hasTestTag(CHECKOUT_CREATE_SESSION_BUTTON_TEST_TAG))
        createSessionButton.waitForEnabled()
        createSessionButton.click()

        val testContext = CheckoutTestContext(composeTestRule)
        testContext.waitUntilConfigured()
        testContext.block()
    }
}

@OptIn(ExperimentalTestApi::class)
internal class CheckoutTestContext(
    private val composeTestRule: ComposeTestRule,
) {
    private val confirmButton = ComposeButton(composeTestRule, hasTestTag(CHECKOUT_CONFIRM_BUTTON_TEST_TAG))
    private val formPrimaryButton = ComposeButton(composeTestRule, hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG))

    // The Confirm button only renders once the session is created and the controller is configured.
    fun waitUntilConfigured() {
        confirmButton.waitForEnabled()
    }

    fun fillNewCard(cardNumber: String? = null) {
        selectNewPaymentMethod(PaymentMethod.Type.Card.code)

        val formPage = FormPage(composeTestRule)
        if (cardNumber != null) {
            formPage.fillCardNumber(cardNumber)
            formPage.fillOutCardDetails(fillOutCardNumber = false)
        } else {
            formPage.fillOutCardDetails()
        }

        formPrimaryButton.waitForEnabled(requireClickAction = false)
        formPrimaryButton.click()
        composeTestRule.waitUntilDoesNotExist(
            hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )
    }

    fun confirm() {
        confirmButton.waitForEnabled()
        confirmButton.click()
    }

    fun assertCompleted() {
        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(CHECKOUT_COMPLETED_TEST_TAG),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )
    }

    fun assertConfirmationFailed(expectedMessage: String) {
        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(CHECKOUT_CONFIRMATION_MESSAGE_TEST_TAG),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )
        composeTestRule.onNodeWithTag(CHECKOUT_CONFIRMATION_MESSAGE_TEST_TAG)
            .assertTextContains(expectedMessage, substring = true)
    }

    private fun selectNewPaymentMethod(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )

        composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }
}
