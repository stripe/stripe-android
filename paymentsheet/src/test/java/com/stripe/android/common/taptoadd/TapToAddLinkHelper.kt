package com.stripe.android.common.taptoadd

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile

internal class TapToAddLinkHelper(
    private val composeTestRule: ComposeTestRule,
    private val networkRule: NetworkRule,
) {
    fun enqueueLookup() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
            bodyPart("email_address", urlEncode(EMAIL)),
        ) { response ->
            response.testBodyFromFile("consumer-sessions-lookup-does-not-exist-success.json")
        }
    }

    fun enqueueSignup() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
            bodyPart("email_address", urlEncode(EMAIL)),
            bodyPart("phone_number", urlEncode(PHONE)),
            bodyPart("legal_name", urlEncode(NAME)),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }
    }

    fun enqueueCreatePaymentDetailsFromPaymentMethod(paymentMethodId: String) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/from_payment_method"),
            bodyPart("payment_method_id", urlEncode(paymentMethodId)),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }
    }

    fun checkbox(): SemanticsNodeInteraction {
        return waitForText("Save my info for faster checkout with Link")
            .assertHasClickAction()
    }

    fun fillEmail() {
        return waitForText("Email").inputText(EMAIL)
    }

    fun fillPhone() {
        return waitForText("Phone number").inputText(PHONE_INPUT)
    }

    fun fillName() {
        return waitForText("Full name").inputText("John Doe")
    }

    fun userInput(): UserInput {
        return UserInput.SignUp(
            email = EMAIL,
            phone = PHONE,
            name = NAME,
            country = "US",
            consentAction = SignUpConsentAction.Checkbox,
        )
    }

    private fun waitForText(text: String): SemanticsNodeInteraction {
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT) {
            composeTestRule
                .onAllNodes(hasText(text))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        return composeTestRule.onNode(hasText(text))
    }

    private fun SemanticsNodeInteraction.inputText(text: String) {
        assertExists()
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(text)

        composeTestRule.waitForIdle()
    }

    private companion object {
        const val DEFAULT_UI_TIMEOUT = 5000L

        const val EMAIL = "email@email.com"
        const val PHONE_INPUT = "2113526421"
        const val PHONE = "+1$PHONE_INPUT"
        const val NAME = "John Doe"
    }
}
