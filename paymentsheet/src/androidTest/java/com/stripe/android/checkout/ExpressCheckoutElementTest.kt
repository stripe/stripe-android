package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.CheckoutInitResponseFactory
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutFetch
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.link.ui.wallet.LinkWalletPage
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.enqueueConsumerSessionLookup
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
            around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(IntentsRule())
    }

    private val page = ExpressCheckoutElementPage(testRules.compose)
    private val linkWalletPage = LinkWalletPage(testRules.compose)

    @Test
    fun testSuccessfulGooglePayPayment() {
        // Link is disabled for ECE in this test, but this is still called for PE.
        networkRule.enqueueConsumerSessionLookup()

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(ExpressCheckoutElement.PaymentMethod.GooglePay())
            },
            configurationUpdates = {
                it.linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                )
            }
        ) {
            val paymentMethod = PaymentMethodFactory.card()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
                bodyPart("client_attribution_metadata[merchant_integration_source]", "mobile-checkout"),
                bodyPart(
                    "client_attribution_metadata[merchant_integration_subtype]",
                    "mobile-express-checkout-element",
                ),
                bodyPart(
                    "client_attribution_metadata[merchant_integration_additional_elements][]",
                    "mobile-express-checkout-element",
                ),
                not(hasBodyPart("client_attribution_metadata[payment_method_selection_flow]")),
                not(hasBodyPart("client_attribution_metadata[payment_intent_creation_flow]")),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        // This is called twice during load - once for ECE, once for PE
        repeat(2) {
            networkRule.enqueueNativeLinkAccount()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = { response -> createNativeLinkCheckoutInitResponse(response) },
            resultCallback = ::assertNativeLinkPaymentCompleted,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(ExpressCheckoutElement.PaymentMethod.Link())
            },
            configurationUpdates = {
                it.googlePayConfiguration(
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                        .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
                )
            },
        ) {
            networkRule.enqueueNativeLinkPaymentDetails()
            networkRule.checkoutConfirmNativeLinkPayment(
                bodyPart("expected_amount", "5099"),
            )

            page.clickLinkButton()
            linkWalletPage.pay()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testGooglePayAndLinkLoad() {
        // This is called for both ECE and PE
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(
                    ExpressCheckoutElement.PaymentMethod.GooglePay(),
                    ExpressCheckoutElement.PaymentMethod.Link(),
                )
            },
        ) {
            // Just testing load, no need to confirm.
            markTestSucceeded()
        }
    }

    @Test
    fun testFailedGooglePayPayment() {
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        val expectedErrorMessage = "Google Pay failed"
        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
                val error = (result as CheckoutElementsController.Result.Failed).error
                assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
            },
        ) {
            enqueueFailedGooglePayPayment(IllegalStateException(expectedErrorMessage))

            repeat(2) {
                networkRule.enqueueConsumerSessionLookup()
            }
            networkRule.checkoutFetch()

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        repeat(2) {
            networkRule.enqueueNativeLinkAccount()
        }

        var checkoutResult: CheckoutElementsController.Result? = null
        val expectedErrorMessage = "Link failed"
        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = { response -> createNativeLinkCheckoutInitResponse(response) },
            resultCallback = { result -> checkoutResult = result },
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/payment_details/list"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody(
                    """{"error":{"type":"invalid_request_error","message":"$expectedErrorMessage"}}"""
                )
            }
            networkRule.checkoutFetch(
                responseFactory = { response -> createNativeLinkCheckoutInitResponse(response) },
            )

            page.clickLinkButton()
            testRules.compose.waitUntilWithIdle(
                conditionDescription = "Link load failure is reported",
            ) {
                checkoutResult != null
            }
        }

        assertThat(checkoutResult).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
        val error = (checkoutResult as CheckoutElementsController.Result.Failed).error
        assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
        assertNativeLinkCalled()
    }

    @Test
    fun testLinkIsHiddenWhenShippingAddressIsRequired() {
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = ::createCheckoutInitResponseWithRequiredShippingAddress,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(ExpressCheckoutElement.PaymentMethod.GooglePay())
            },
        ) {
            page.assertGooglePayButtonExists()
            page.assertLinkButtonDoesNotExist()
            markTestSucceeded()
        }
    }

    @Test
    fun testGooglePayCollectsAndConfirmsRequiredShippingAddress() {
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = ::createCheckoutInitResponseWithRequiredShippingAddress,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()
            val shippingInformation = createShippingInformation()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
                shippingInformation = shippingInformation,
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
                bodyPart("shipping[name]", "Jenny Rosen"),
                bodyPart("shipping[address][line1]", "510 Townsend St"),
                bodyPart("shipping[address][line2]", "Floor 3"),
                bodyPart("shipping[address][city]", "San Francisco"),
                bodyPart("shipping[address][state]", "CA"),
                bodyPart("shipping[address][postal_code]", "94103"),
                bodyPart("shipping[address][country]", "US"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithShippingAddressParameters(
            GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = setOf("US", "CA"),
            )
        )
    }

    @Test
    fun testGooglePayUpdatesAutomaticTaxForRequiredShippingAddress() {
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory =
                ::createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()
            val shippingInformation = createShippingInformation()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
                shippingInformation = shippingInformation,
                paymentDataUpdate = GooglePayPaymentDataUpdate(
                    callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                    shippingAddress = GooglePayPaymentDataUpdate.GooglePayAddress(
                        administrativeArea = "California",
                        countryCode = "US",
                        locality = "San Francisco",
                        postalCode = "94103",
                        iso3166AdministrativeArea = "US-CA",
                    ),
                    billingAddress = null,
                ),
            )
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax(response)
            }
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
                bodyPart("shipping[name]", "Jenny Rosen"),
                bodyPart("shipping[address][line1]", "510 Townsend St"),
                bodyPart("shipping[address][line2]", "Floor 3"),
                bodyPart("shipping[address][city]", "San Francisco"),
                bodyPart("shipping[address][state]", "CA"),
                bodyPart("shipping[address][postal_code]", "94103"),
                bodyPart("shipping[address][country]", "US"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithShippingAddressParameters(
            GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = setOf("US", "CA"),
            )
        )
    }

    @Test
    fun testGooglePayCollectsAndConfirmsRequiredBillingAddress() {
        repeat(2) {
            networkRule.enqueueConsumerSessionLookup()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddress,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testGooglePaySendsRequiredBillingAddressForAutomaticTax() {
        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[line1]", "510 Townsend St"),
                bodyPart("tax_region[line2]", "Floor 3"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                CheckoutInitResponseFactory.createWithRequiredBillingAddressForAutomaticTax(response)
            }
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testGooglePayFailsWhenAutomaticTaxUpdateChangesTotal() {
        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
                val error = (result as CheckoutElementsController.Result.Failed).error
                assertThat(error).isInstanceOf(LocalStripeException::class.java)
                assertThat((error as LocalStripeException).stripeError?.code)
                    .isEqualTo("checkout_session_total_changed")
            },
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONArray("items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .getJSONObject("amount_details")
                        .put("total", 5399).put("tax_exclusive", 300)
                    json.getJSONObject("total_summary").put("total", 5399).put("tax_exclusive", 300)
                }
            }
            networkRule.checkoutFetch(
                responseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testNativeLinkConfirmsRequiredBillingAddress() {
        repeat(2) {
            networkRule.enqueueNativeLinkAccount()
        }

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = { response ->
                createNativeLinkCheckoutInitResponse(response) { json ->
                    json.put("billing_address_collection", "required")
                }
            },
            resultCallback = ::assertNativeLinkPaymentCompleted,
        ) {
            networkRule.enqueueNativeLinkPaymentDetails { paymentDetails ->
                paymentDetails.getJSONObject("billing_address")
                    .put("line_1", "510 Townsend St")
                    .put("line_2", "Floor 3")
                    .put("locality", "San Francisco")
                    .put("administrative_area", "CA")
                    .put("postal_code", "94103")
            }
            networkRule.checkoutConfirmNativeLinkPayment(
                bodyPart("expected_amount", "5099"),
                bodyPart("payment_method_data[billing_details][address][line1]", "510 Townsend St"),
                bodyPart("payment_method_data[billing_details][address][line2]", "Floor 3"),
                bodyPart("payment_method_data[billing_details][address][city]", "San Francisco"),
                bodyPart("payment_method_data[billing_details][address][state]", "CA"),
                bodyPart("payment_method_data[billing_details][address][postal_code]", "94103"),
                bodyPart("payment_method_data[billing_details][address][country]", "US"),
            )

            page.clickLinkButton()
            linkWalletPage.pay()
        }

        assertNativeLinkCalledWithRequiredBillingAddress()
    }
}
