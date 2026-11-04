package com.stripe.android.checkout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.RequiresLiveBackend
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
@RequiresLiveBackend
internal class CheckoutCardLiveTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

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
    fun testSuccessfulCardPayment() = runCheckoutLiveTest(
        composeRule = composeRule,
        merchant = "US",
        sessionParams = cardOnlySessionParams(),
        configuration = configuration,
    ) {
        contentPage.clickOnLpm("card")
        formPage.fillOutCardDetails(newCardNumber = "4242424242424242")
        formPage.clickPrimaryButton()

        val result = confirmAndAwaitResult()

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    @Test
    fun testDeclinedCardReportsFailure() = runCheckoutLiveTest(
        composeRule = composeRule,
        merchant = "US",
        sessionParams = cardOnlySessionParams(),
        configuration = configuration,
    ) {
        contentPage.clickOnLpm("card")
        formPage.fillOutCardDetails(newCardNumber = "4000000000000002")
        formPage.clickPrimaryButton()

        val result = confirmAndAwaitResult()

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
        assertThat((result as CheckoutElementsController.Result.Failed).displayMessage)
            .contains("Your card was declined")
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
}
