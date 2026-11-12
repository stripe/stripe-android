package com.stripe.android.checkout.webauth

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CHECKOUT_PLAYGROUND_BACKEND_URL
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutPlaygroundBackend
import com.stripe.android.checkout.runCheckoutIntegrationTest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import kotlinx.coroutines.runBlocking
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

// Runs on a google_apis device in its own CI step: web authentication needs a current System WebView.
@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutWebAuthIntegrationTest {
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
    fun testCardWith3ds2ChallengeCompletes() = runBlocking {
        val result = runCheckoutIntegrationTest(
            backend = CheckoutPlaygroundBackend(
                baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
                merchant = "US",
            ),
            sessionParams = cardOnlySessionParams(),
            configuration = configuration,
            setup = {
                contentPage.clickOnLpm("card")
                formPage.fillOutCardDetails(newCardNumber = "4000000000003220")
                formPage.clickPrimaryButton()
            },
            handleNextAction = {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                val completeButton = device.wait(
                    Until.findObject(By.text("COMPLETE")),
                    CHALLENGE_TIMEOUT.inWholeMilliseconds,
                )
                checkNotNull(completeButton) { "3DS2 challenge COMPLETE button not found" }.click()
            },
        )
        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
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
        val CHALLENGE_TIMEOUT = 30.seconds
    }
}
