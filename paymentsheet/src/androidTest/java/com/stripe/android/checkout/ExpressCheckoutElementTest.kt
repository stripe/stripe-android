package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.CheckoutInitResponseFactory
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
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

    @Test
    fun testSuccessfulGooglePayPayment() {
        // This is called twice during load - once for ECE, once for PE
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
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
        repeat (2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.Link()
                )
            },
        ) {
            enqueueSuccessfulNativeLinkPayment()

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testGooglePayOnlyLoad() {
        // This is called for PE, but we skip this account lookup for ECE since its Link config sets display to never.
        networkRule.enqueueLinkAccountLookup()

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
            configurationUpdates = {
                it.linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                )
            }
        ) { testContext ->
            // Just testing load, no need to confirm.
            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testFailedGooglePayPayment() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        val expectedErrorMessage = "Google Pay failed"
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val error = (result as CheckoutController.Result.Failed).error
                assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
            },
        ) {
            enqueueFailedGooglePayPayment(IllegalStateException(expectedErrorMessage))

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        val expectedErrorMessage = "Link failed"
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val error = (result as CheckoutController.Result.Failed).error
                assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
            },
        ) {
            enqueueFailedNativeLinkPayment(IllegalStateException(expectedErrorMessage))

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testLinkIsHiddenWhenShippingAddressIsRequired() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = ::createCheckoutInitResponseWithRequiredShippingAddress,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
        ) { testContext ->
            page.assertGooglePayButtonExists()
            page.assertLinkButtonDoesNotExist()
            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testGooglePayCollectsAndConfirmsRequiredShippingAddress() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = ::createCheckoutInitResponseWithRequiredShippingAddress,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()
            val shippingInformation = ShippingInformation(
                address = Address(
                    city = "San Francisco",
                    country = "US",
                    line1 = "510 Townsend St",
                    line2 = "Floor 3",
                    postalCode = "94103",
                    state = "CA",
                ),
                name = "Jenny Rosen",
                phone = null,
            )

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
    fun testGooglePayCollectsAndConfirmsRequiredBillingAddress() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddress,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
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
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
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
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val error = (result as CheckoutController.Result.Failed).error
                assertThat(error).isInstanceOf(LocalStripeException::class.java)
                assertThat((error as LocalStripeException).stripeError?.code)
                    .isEqualTo("checkout_session_total_changed")
            },
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONArray("checkout_items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .put("total", 5399)
                }
            }
            networkRule.checkoutInit(
                responseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testNativeLinkCollectsAndConfirmsRequiredBillingAddress() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddress,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueNativeLinkPaymentMethod(paymentMethod)
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickLinkButton()
        }

        assertNativeLinkCalledWithRequiredBillingAddress()
    }
}
