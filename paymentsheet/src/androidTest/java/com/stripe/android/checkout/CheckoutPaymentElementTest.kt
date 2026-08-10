package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.utils.TestRules
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testBackingOutOfFormPreservesPreviouslySelectedPaymentMethod() {
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("customer_email", "checkout@example.com")
                json.getJSONObject("elements_session").remove("link_settings")
            }
        }

        var checkoutResult: CheckoutController.Result? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            lateinit var controller: CheckoutController
            scenario.onActivity { activity ->
                PaymentConfiguration.init(activity, "pk_test_123")
                controller = CheckoutController.Builder(
                    application = activity.application,
                    savedStateHandle = SavedStateHandle(),
                ).resultCallback { result ->
                    checkoutResult = result
                }.build()
            }

            runBlocking {
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            }
            controller.clearPaymentOption().getOrThrow()

            lateinit var paymentElement: PaymentElement
            scenario.onActivity { activity ->
                paymentElement = controller.createPresenter(activity).paymentElement()
                activity.setContent {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        paymentElement.PaymentOptionsContent()
                    }
                }
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

            // Open the card form, then back out without entering any details.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Select a payment method that does not require a form.
            contentPage.clickOnLpm("cashapp")
            contentPage.assertHasSelectedLpm("cashapp")

            // Re-open the card form and back out again.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Backing out of the form must not clear the previously selected payment method.
            contentPage.assertHasSelectedLpm("cashapp")
            assertThat(checkoutResult).isNull()

            controller.destroy()
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
