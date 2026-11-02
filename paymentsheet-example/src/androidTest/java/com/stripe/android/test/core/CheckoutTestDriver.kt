package com.stripe.android.test.core

import android.content.Context
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
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutControllerExampleActivity
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.paymentelementtestpages.FormPage
import kotlin.time.Duration.Companion.seconds

/**
 * Drives [CheckoutControllerExampleActivity] end to end against the playground backend.
 */
internal class CheckoutTestDriver(
    private val composeTestRule: ComposeTestRule,
) {
    private var scenario: ActivityScenario<CheckoutControllerExampleActivity>? = null

    private val createSessionButton = ComposeButton(
        composeTestRule,
        hasTestTag(CHECKOUT_CREATE_SESSION_BUTTON_TEST_TAG),
    )
    private val confirmButton = ComposeButton(composeTestRule, hasTestTag(CHECKOUT_CONFIRM_BUTTON_TEST_TAG))
    private val formPrimaryButton = ComposeButton(composeTestRule, hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG))

    fun launch(settings: CheckoutPlaygroundSettings) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scenario = ActivityScenario.launch(
            CheckoutControllerExampleActivity.createTestIntent(context, settings.asJsonString())
        )

        createSessionButton.waitForEnabled()
        createSessionButton.click()

        // The Confirm button only renders once the session is created and the controller is configured.
        confirmButton.waitForEnabled(requireClickAction = true, timeout = SESSION_CREATION_TIMEOUT)
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
        waitUntilGone(SHEET_PRIMARY_BUTTON_TEST_TAG)
    }

    fun confirm() {
        confirmButton.waitForEnabled()
        confirmButton.click()
    }

    fun assertCompleted() {
        waitUntilExists(CHECKOUT_COMPLETED_TEST_TAG, CONFIRMATION_TIMEOUT.inWholeMilliseconds)
    }

    fun assertConfirmationFailed(expectedMessage: String) {
        waitUntilExists(CHECKOUT_CONFIRMATION_MESSAGE_TEST_TAG, CONFIRMATION_TIMEOUT.inWholeMilliseconds)
        composeTestRule.onNodeWithTag(CHECKOUT_CONFIRMATION_MESSAGE_TEST_TAG)
            .assertTextContains(expectedMessage, substring = true)
    }

    fun teardown() {
        scenario?.close()
        scenario = null
    }

    private fun selectNewPaymentMethod(paymentMethodCode: PaymentMethodCode) {
        waitUntilExists(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT, DEFAULT_UI_TIMEOUT.inWholeMilliseconds)

        composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitUntilExists(tag: String, timeoutMillis: Long) {
        composeTestRule.waitUntil(
            conditionDescription = "node with tag $tag to appear",
            timeoutMillis = timeoutMillis,
        ) {
            composeTestRule.onAllNodes(hasTestTag(tag))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitUntilGone(tag: String) {
        composeTestRule.waitUntil(
            conditionDescription = "node with tag $tag to disappear",
            timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        ) {
            composeTestRule.onAllNodes(hasTestTag(tag))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private companion object {
        val SESSION_CREATION_TIMEOUT = 30.seconds
        val CONFIRMATION_TIMEOUT = 30.seconds
    }
}
