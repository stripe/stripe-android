package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.RequiresLiveBackend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
@RequiresLiveBackend
internal class CheckoutPaymentElementLiveTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contentPage = EmbeddedContentPage(composeRule)
    private val formPage = EmbeddedFormPage(composeRule)
    private val configuration = CheckoutElementsController.Configuration()
        .merchantDisplayName("Checkout E2E")
        .paymentElement(
            PaymentElement.Configuration()
                .googlePayConfiguration(
                    PaymentElement.Configuration.GooglePayConfiguration()
                        .display(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
                )
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration()
                        .display(PaymentElement.Configuration.LinkConfiguration.Display.Never)
                )
        )

    @Test
    fun testSuccessfulCardPayment() {
        val result = runScenario(cardNumber = "4242424242424242")

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    @Test
    fun testDeclinedCardReportsFailure() {
        val result = runScenario(cardNumber = "4000000000000002")

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
        assertThat((result as CheckoutElementsController.Result.Failed).displayMessage)
            .contains("Your card was declined")
    }

    private fun runScenario(cardNumber: String): CheckoutElementsController.Result {
        val backend = CheckoutLiveBackend(
            baseUrl = InstrumentationRegistry.getArguments().getString("checkoutBackendUrl")
                ?: "https://stp-mobile-playground-backend-v7.stripedemos.com/",
        )
        val (publishableKey, clientSecret) = runBlocking {
            withTimeout(OPERATION_TIMEOUT) {
                backend.fetchPublishableKey() to backend.createCheckoutSession(cardOnlySessionParams())
            }
        }
        val checkoutResult = CompletableDeferred<CheckoutElementsController.Result>()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            PaymentConfiguration.init(ApplicationProvider.getApplicationContext(), publishableKey)

            lateinit var controller: CheckoutElementsController
            scenario.onActivity {
                controller = CheckoutElementsController.Builder(
                    application = ApplicationProvider.getApplicationContext(),
                    savedStateHandle = SavedStateHandle(),
                ).resultCallback { checkoutResult.complete(it) }.build()
            }
            try {
                val configureResult = runBlocking {
                    withTimeout(OPERATION_TIMEOUT) {
                        controller.configure(clientSecret, configuration)
                    }
                }
                assertThat(configureResult).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)

                lateinit var presenter: CheckoutElementsPresenter
                scenario.onActivity { activity ->
                    presenter = controller.createPresenter(activity)
                    val paymentElement = presenter.paymentElement()
                    activity.setContent {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            paymentElement.Content()
                        }
                    }
                }
                scenario.moveToState(Lifecycle.State.RESUMED)

                contentPage.clickOnLpm("card")
                formPage.fillOutCardDetails(newCardNumber = cardNumber)
                formPage.clickPrimaryButton()
                presenter.confirm()

                return runBlocking {
                    withTimeout(OPERATION_TIMEOUT) { checkoutResult.await() }
                }
            } finally {
                scenario.onActivity { controller.destroy() }
            }
        }
    }

    private fun cardOnlySessionParams(): JsonObject {
        val lineItem = buildJsonObject {
            put("quantity", 1)
            put(
                "price_data",
                buildJsonObject {
                    put("currency", "usd")
                    put("unit_amount", 1000)
                    put("product_data", buildJsonObject { put("name", "Checkout E2E") })
                },
            )
        }
        return buildJsonObject {
            put("ui_mode", "mobile_elements")
            put("currency", "usd")
            put("customer_email", "checkout-e2e@example.com")
            put("allowed_payment_method_types", JsonArray(listOf(JsonPrimitive("card"))))
            put(
                "items",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "one_time_price")
                            put("one_time_price", buildJsonObject { put("items", JsonArray(listOf(lineItem))) })
                        },
                    )
                },
            )
        }
    }

    private companion object {
        val OPERATION_TIMEOUT = 60.seconds
    }
}
