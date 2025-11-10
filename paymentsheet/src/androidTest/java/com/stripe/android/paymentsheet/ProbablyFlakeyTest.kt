package com.stripe.android.paymentsheet

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.testing.ShampooRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProbablyFlakeyTest {
    @get:Rule
    val testRules: TestRules = TestRules.create {
        around(ShampooRule(500))
    }

    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(testRules.compose)

    @Test
    fun testDeferredIntentCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        builder = {
            createIntentCallback { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_example")
            }
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = PaymentSheet.Configuration.Builder(
                    merchantDisplayName = "Example, Inc."
                )
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .billingDetailsCollectionConfiguration(
                        PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                        )
                    )
                    .build()
            )
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
            clientAttributionMetadataParamsForDeferredIntent(),
            bodyPart(urlEncode("billing_details[name]"),"JayUpdated"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.waitForText("Name on card")
        page.replaceText("Name on card", "Jay")
        page.fillOutCardDetails()
        page.replaceText("Name on card", "JayUpdated")

        page.clickPrimaryButton()
    }
}
