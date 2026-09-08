package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.inputText
import com.stripe.android.testing.waitForNode

class TapToAddLinkTestHelper(
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

    fun enqueueSignup(withName: Boolean = true) {
        val nameMatchers = arrayOf(bodyPart("legal_name", urlEncode(NAME))).takeIf {
            withName
        } ?: emptyArray()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
            bodyPart("email_address", urlEncode(EMAIL)),
            bodyPart("phone_number", urlEncode(PHONE)),
            *nameMatchers,
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }
    }

    fun enqueueCreatePaymentDetailsFromPaymentMethod(
        paymentMethodId: String,
        ephemeralKey: String,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/from_payment_method"),
            bodyPart("payment_method_id", urlEncode(paymentMethodId)),
            bodyPart("customer_ephemeral_key_secret", urlEncode(ephemeralKey)),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }
    }

    fun checkbox(): SemanticsNodeInteraction {
        val matcher = hasText("Save my info for faster checkout with Link")
        composeTestRule.waitForNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
        )
        return composeTestRule.onNode(matcher)
            .assertHasClickAction()
    }

    fun fillEmail() {
        fillText(label = "Email", text = EMAIL)
    }

    fun fillPhone() {
        fillText(label = "Phone number", text = PHONE_INPUT)
    }

    fun fillName() {
        fillText(label = "Full name", text = "John Doe")
    }

    fun input(): Input {
        return Input(
            email = EMAIL,
            phone = PHONE,
            name = NAME,
        )
    }

    private fun fillText(label: String, text: String) {
        val matcher = hasText(label)
        composeTestRule.waitForNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
        )
        composeTestRule.inputText(
            matcher = matcher,
            text = text,
            scrollBehavior = ScrollBehavior.Required,
            settleAfterInput = true,
        )
    }

    data class Input(
        val email: String,
        val phone: String,
        val name: String,
    )

    private companion object {
        const val EMAIL = "email@email.com"
        const val PHONE_INPUT = "2113526421"
        const val PHONE = "+1$PHONE_INPUT"
        const val NAME = "John Doe"
    }
}
