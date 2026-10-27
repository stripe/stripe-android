package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.replaceText
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.AddressElementPage
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutShippingAddressElementTest {
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = AddressElementPage(testRules.compose, ApplicationProvider.getApplicationContext())

    @Test
    fun testSavedAddressReachesSessionWhenHostIsRecreatedUnderSheet() {
        val configuration = CheckoutController.Configuration()
            .shippingAddressElement(ShippingAddressElement.Configuration())
            .defaults(
                CheckoutController.Configuration.Defaults().shippingDetails(
                    CheckoutController.Configuration.Defaults.ContactDetails()
                        .name("Real Name")
                        .address(
                            CheckoutController.Address()
                                .line1("1234 Main St")
                                .city("Boston")
                                .state("MA")
                                .postalCode("12345")
                                .country("US")
                        )
                )
            )

        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            checkoutInitResponse = ::createCheckoutInitResponse,
            renderPaymentElementContent = false,
            setup = { controller ->
                assertThat(controller.configure(DEFAULT_CLIENT_SECRET, configuration))
                    .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            },
        ) {
            presentShippingAddressElement()
            page.waitUntilVisible()
            page.assertName("Real Name")

            recreateHostUnderSheet()
            page.waitUntilVisible()

            testRules.compose.replaceText("City", "Cambridge")
            page.clickSave()

            testRules.compose.waitUntilWithIdle(
                conditionDescription = "saved shipping address to reach controller.session after host recreation",
            ) {
                controller.session.value?.shippingAddress?.address?.city == "Cambridge"
            }
            val shipping = controller.session.value?.shippingAddress
            assertThat(shipping?.name).isEqualTo("Real Name")
            assertThat(shipping?.address?.line1).isEqualTo("1234 Main St")
            markTestSucceeded()
        }
    }

    private fun createCheckoutInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.getJSONObject("elements_session").remove("link_settings")
            json.put(
                "shipping_address_collection",
                JSONObject().put("allowed_countries", JSONArray().put("US")),
            )
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
