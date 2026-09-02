package com.stripe.android.checkout

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.utils.PrefsTestStoreRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(PrefsTestStoreRule())
            .around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(IntentsRule())
    }

    @Test
    fun testSuccessfulGooglePayPayment() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
        ) {
            val googlePayButton = hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG) and isEnabled() and hasClickAction()
            testRules.compose.waitUntil(
                conditionDescription = "Google Pay button is enabled and clickable",
                timeoutMillis = 5_000,
            ) {
                testRules.compose.onAllNodes(googlePayButton)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            val paymentMethod = PaymentMethodFactory.card()

            intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(
                        "extra_result",
                        GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod),
                    ),
                )
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            testRules.compose.onNode(googlePayButton).performClick()
        }

        intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.Link()
                )
            },
        ) {
            val linkButton = hasTestTag(LinkButtonTestTag) and isEnabled() and hasClickAction()
            testRules.compose.waitUntil(
                conditionDescription = "Link button is enabled and clickable",
                timeoutMillis = 5_000,
            ) {
                testRules.compose.onAllNodes(linkButton)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            intending(hasComponent(LinkActivity::class.java.name)).respondWith(
                Instrumentation.ActivityResult(
                    LinkActivity.RESULT_COMPLETE,
                    Intent().putExtra(
                        LinkActivityContract.EXTRA_RESULT,
                        LinkActivityResult.Completed(LinkAccountUpdate.None),
                    ),
                )
            )

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
            networkRule.checkoutInit(responseFactory = CheckoutInitResponseFactory::create)

            testRules.compose.onNode(linkButton).performClick()
        }

        intended(hasComponent(LinkActivity::class.java.name))
    }

    private companion object {
        const val GOOGLE_PAY_ACTIVITY_NAME =
            "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"
    }
}
