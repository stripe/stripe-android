package com.stripe.android.checkout

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerComposeTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    // Compose's default unconfined dispatcher resumes collectors on the emitter's IO thread.
    // Queue effects on the UI test clock to model the main-thread collector used in an app.
    private val composeRule = createAndroidComposeRule<ComponentActivity>(
        effectContext = StandardTestDispatcher(),
    )
    private val networkRule = NetworkRule()

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        // Dispatch Main work to the test thread, even when an IO mutation resumes a collector.
        .around(CoroutineTestRule(StandardTestDispatcher()))
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(application))

    @Test
    fun `background promotion update succeeds with PaymentElement Content mounted`() = runScenario { controller ->
        networkRule.checkoutUpdate(bodyPart("promotion_code", "10OFF")) { response ->
            successResponse(response) { json ->
                json.getJSONArray("items").getJSONObject(0)
                    .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                    .getJSONObject("amount_details")
                    .put("total", 4099).put("subtotal", 4099)
            }
        }

        withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            assertThat(controller.applyPromotionCode("10OFF"))
                .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
        }

        composeRule.onNodeWithTag(TOTAL_TAG).assertTextEquals("4099.0")
        composeRule.onNodeWithTag(OPTION_TAG).assertTextEquals("card")
        composeRule.onNodeWithTag(ICON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
    }

    @Test
    fun `background clear updates payment option with PaymentElement Content mounted`() = runScenario { controller ->
        withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            assertThat(controller.clearPaymentOption())
                .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
        }

        composeRule.onNodeWithTag(OPTION_TAG).assertTextEquals("none")
        composeRule.onNodeWithTag(ICON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()
    }

    private fun runScenario(
        block: suspend (CheckoutController) -> Unit,
    ) = runTest {
        val savedState = CheckoutControllerSavedState(SavedStateHandle(), "checkout_compose_test")
        val controller = DaggerCheckoutControllerComponent.factory().create(
            application = application,
            paymentElementCallbackIdentifier = "checkout_compose_test",
            resultCallback = {},
            rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
            checkoutControllerSavedState = savedState,
        ).checkoutController
        try {
            networkRule.checkoutInit { response -> successResponse(response) {} }
            assertThat(
                controller.configure(
                    "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
                    CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            )
                .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            CheckoutControllerStateFactory.createStateHolder(savedState.handle)
                .setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(controller.session.value?.totals?.total?.minorUnitsAmount).isNotEqualTo(4099.0)

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            val presenter = controller.createPresenter(composeRule.activity)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            val paymentElement = presenter.paymentElement()
            // The presenter's initialization uses Main's scheduler, separate from Compose's clock.
            testScheduler.runCurrent()
            composeRule.setContent {
                val session by controller.session.collectAsState()
                Column {
                    Text(
                        text = session?.totals?.total?.minorUnitsAmount.toString(),
                        modifier = Modifier.testTag(TOTAL_TAG),
                    )
                    Text(
                        text = session?.paymentOption?.paymentMethodType ?: "none",
                        modifier = Modifier.testTag(OPTION_TAG),
                    )
                    session?.paymentOption?.let { option ->
                        Image(
                            painter = option.iconPainter,
                            contentDescription = null,
                            modifier = Modifier.testTag(ICON_TAG),
                        )
                    }
                    paymentElement.Content()
                }
            }
            composeRule.onNodeWithTag(OPTION_TAG).assertTextEquals("card")
            composeRule.onNodeWithTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT).assertIsDisplayed()

            block(controller)
        } finally {
            controller.destroy()
            PaymentElementCallbackReferences.clear()
        }
    }

    private fun successResponse(response: MockResponse, modifier: (JSONObject) -> Unit) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.remove("link_settings")
            modifier(json)
        }
    }

    private companion object {
        const val TOTAL_TAG = "checkout_total"
        const val OPTION_TAG = "checkout_payment_option"
        const val ICON_TAG = "checkout_payment_option_icon"
    }
}
