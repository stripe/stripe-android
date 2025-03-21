package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.testing.PaymentMethodFactory
import org.json.JSONArray

internal object DefaultPaymentMethodsUtils {
    fun enqueueElementsSessionResponse(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false,
        cards: List<PaymentMethod> = emptyList(),
        defaultPaymentMethod: String? = null,
    ) {
        val cardsArray = JSONArray()

        cards.forEach { card ->
            cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
        }

        val responseFile = if (isDeferredIntent) {
            "elements-sessions-deferred_intent_and_default_pms_enabled.json"
        } else {
            "elements-sessions-with_pi_and_default_pms_enabled.json"
        }

        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                responseFile,
                replacements = listOf(
                    ResponseReplacement(
                        "DEFAULT_PAYMENT_METHOD_HERE",
                        defaultPaymentMethod.toString()
                    ),
                    ResponseReplacement(
                        "[PAYMENT_METHODS_HERE]",
                        cardsArray.toString(2),
                    )
                )
            )
        }
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    fun launch(
        testContext: ProductIntegrationTestRunnerContext,
        composeTestRule: ComposeTestRule,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        hasSavedPaymentMethods: Boolean = true,
        isDeferredIntent: Boolean = false,
    ) {
        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                paymentMethodLayout = paymentMethodLayout,
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                )
            ),
            isDeferredIntent = isDeferredIntent,
        )

        if (paymentMethodLayout == PaymentSheet.PaymentMethodLayout.Horizontal && hasSavedPaymentMethods) {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG)).fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }
}
