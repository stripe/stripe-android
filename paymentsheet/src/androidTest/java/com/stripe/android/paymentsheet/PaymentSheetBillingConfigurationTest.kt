package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet.Builder
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutTypeProvider
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(TestParameterInjector::class)
internal class PaymentSheetBillingConfigurationTest {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()
    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @get:Rule
    val testRules: TestRules = TestRules.create(composeTestRule = composeTestRule)

    private val networkRule = testRules.networkRule

    @Test
    fun testPayloadWithDefaultsAndOverrides() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.setResponseCode(500)
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
            paymentSheet = Builder { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }.build(it)
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
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
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
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.setResponseCode(500)
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
            paymentSheet = Builder { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }.build(it)
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
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
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

    @Test
    fun testAddressInputNotReset() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = IntegrationType.Compose,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
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
                            state = "CA",
                            city = "South San Francisco",
                            line1 = "123 Main Street",
                            line2 = null,
                        ),
                    ),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                        attachDefaultsToPaymentMethod = false,
                    ),
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        page.assertIsOnFormPage()
        page.replaceText("123 Main Street", "123 Main Road")
        page.fillExpirationDate("12/34")

        // Check that line 1 was not reset to default value
        page.waitForText("123 Main Road")

        testContext.markTestSucceeded()
    }

    @Test
    fun testWithDefaults(
        @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
        integrationType: ProductIntegrationType,
        @TestParameter(valuesProvider = PaymentSheetLayoutTypeProvider::class)
        layoutType: PaymentSheetLayoutType,
    ) = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                defaultBillingDetails = PaymentSheet.BillingDetails(
                    name = "Jenny Rosen",
                    email = "foo@bar.com",
                    phone = "+13105551234",
                    address = PaymentSheet.Address(
                        postalCode = "94111",
                        country = "US",
                        state = "CA",
                        city = "South San Francisco",
                        line1 = "123 Main Street",
                        line2 = "Unit #123",
                    ),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    attachDefaultsToPaymentMethod = true,
                ),
                paymentMethodLayout = layoutType.paymentMethodLayout,
                paymentMethodOrder = listOf("cashapp", "card")
            ),
        )

        page.clickOnLpm("cashapp", layoutType is PaymentSheetLayoutType.Vertical)

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[billing_details][name]"), urlEncode("Jenny Rosen")),
            bodyPart(urlEncode("payment_method_data[billing_details][email]"), urlEncode("foo@bar.com")),
            bodyPart(urlEncode("payment_method_data[billing_details][phone]"), urlEncode("+13105551234")),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][line1]"),
                urlEncode("123 Main Street")
            ),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][line2]"),
                urlEncode("Unit #123")
            ),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][city]"),
                urlEncode("South San Francisco")
            ),
            bodyPart(urlEncode("payment_method_data[billing_details][address][state]"), "CA"),
            bodyPart(urlEncode("payment_method_data[billing_details][address][country]"), "US"),
            bodyPart(urlEncode("payment_method_data[billing_details][address][postal_code]"), "94111"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
        testContext.consumePaymentOptionEventForFlowController("cashapp", "Cash App Pay")
    }
}
