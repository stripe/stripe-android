package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = PaymentSheetPage(testRules.compose)

    @Test
    fun testPaymentOptionsBackNavigationRestoresPreviousSelection() {
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("customer_email", "checkout@example.com")
                json.getJSONObject("elements_session").remove("link_settings")
            }
        }

        var checkoutResult: CheckoutController.Result? = null
        val savedStateHandle = SavedStateHandle()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            lateinit var controller: CheckoutController
            scenario.onActivity { activity ->
                PaymentConfiguration.init(activity, "pk_test_123")
                controller = CheckoutController.Builder(
                    application = activity.application,
                    savedStateHandle = savedStateHandle,
                ).resultCallback {
                    checkoutResult = it
                }.build()
            }

            runBlocking {
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            }
            controller.clearPaymentOption().getOrThrow()

            lateinit var paymentElement: PaymentElement
            scenario.onActivity { activity ->
                val presenter = controller.createPresenter(activity)
                paymentElement = presenter.paymentElement()
                activity.setContent {
                    paymentElement.PaymentOptionsContent()
                }
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity {
                paymentElement.presentPaymentOptions()
            }

            clickOnLpm("card")
            page.waitForCardForm()
            Espresso.pressBack()

            clickOnLpm("cashapp")
            assertLpmSelected("cashapp")

            clickOnLpm("card")
            page.waitForCardForm()
            Espresso.pressBack()

            assertLpmSelected("cashapp")
            assertThat(checkoutResult).isNull()

            controller.destroy()
        }
    }

    private fun clickOnLpm(code: String) {
        val matcher = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code")
        testRules.compose.waitUntil(5_000) {
            testRules.compose
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        testRules.compose.onNode(matcher, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
    }

    private fun assertLpmSelected(code: String) {
        val matcher = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code").and(isSelected())
        testRules.compose.waitUntil(5_000) {
            testRules.compose
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
