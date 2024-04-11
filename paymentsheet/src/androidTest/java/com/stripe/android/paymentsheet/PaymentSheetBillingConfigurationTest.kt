package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class PaymentSheetBillingConfigurationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun testPayloadWithDefaultsAndOverrides() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    defaultBillingDetails = PaymentSheet.BillingDetails(
                        name = "Jenny Rosen",
                        email = "foo@bar.com",
                        phone = "+13105551234",
                        address = PaymentSheet.Address(
                            postalCode = "94111",
                            country = "US",
                        ),
                    ),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                        attachDefaultsToPaymentMethod = true,
                    ),
                ),
            )
        }

        page.waitForText("Email")
        page.replaceText("Email", "mail@mail.com")
        page.replaceText("Name on card", "Jane Doe")
        page.fillOutCardDetails(fillOutZipCode = false)

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[billing_details][name]"), urlEncode("Jane Doe")),
            bodyPart(urlEncode("payment_method_data[billing_details][email]"), urlEncode("mail@mail.com")),
            bodyPart(urlEncode("payment_method_data[billing_details][phone]"), urlEncode("+13105551234")),
            bodyPart(urlEncode("payment_method_data[billing_details][address][country]"), "US"),
            bodyPart(urlEncode("payment_method_data[billing_details][address][postal_code]"), "94111"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testPayloadWithoutDefaults() {
        repeat(2) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.setResponseCode(500)
            }
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    defaultBillingDetails = PaymentSheet.BillingDetails(
                        name = "Jenny Rosen",
                        email = "foo@bar.com",
                        phone = "+13105551234",
                        address = PaymentSheet.Address(
                            postalCode = "94111",
                            country = "US",
                        ),
                    ),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                        attachDefaultsToPaymentMethod = false,
                    ),
                ),
            )
        }

        page.fillOutCardDetails(fillOutZipCode = false)

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(bodyPart(urlEncode("payment_method_data[billing_details][name]"), urlEncode("Jenny Rosen"))),
            not(bodyPart(urlEncode("payment_method_data[billing_details][email]"), urlEncode("foo@bar.com"))),
            not(bodyPart(urlEncode("payment_method_data[billing_details][phone]"), urlEncode("+13105551234"))),
            not(bodyPart(urlEncode("payment_method_data[billing_details][address][country]"), "US")),
            not(bodyPart(urlEncode("payment_method_data[billing_details][address][postal_code]"), "94111")),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }
}
