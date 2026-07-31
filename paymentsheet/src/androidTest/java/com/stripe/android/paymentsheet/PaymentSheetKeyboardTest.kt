package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.SavedPaymentMethodsPage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
@RequiresIme
internal class PaymentSheetKeyboardTest {
    @get:Rule
    val testRules: TestRules = TestRules.create {
        around(IntentsRule())
    }

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule
    private val page = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun primaryButtonIsRevealedWhenAddFirstPaymentMethodFormBecomesComplete() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                    defaultBillingDetails = defaultBillingDetailsWithoutPostalCode,
                    billingDetailsCollectionConfiguration = fullAddressCollection,
                ),
            )
        }

        assertPrimaryButtonIsRevealedWhenCardFormBecomesComplete()

        testContext.markTestSucceeded()
    }

    @Test
    fun primaryButtonIsRevealedWhenAddAnotherPaymentMethodFormBecomesComplete() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", PaymentMethod.Type.Card.code),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success.json")
        }
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .customer(
                        PaymentSheet.CustomerConfiguration(
                            id = "cus_1",
                            ephemeralKeySecret = "ek_123",
                        )
                    )
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .defaultBillingDetails(defaultBillingDetailsWithoutPostalCode)
                    .billingDetailsCollectionConfiguration(fullAddressCollection)
                    .link(
                        PaymentSheet.LinkConfiguration.Builder()
                            .display(PaymentSheet.LinkConfiguration.Display.Never)
                            .build()
                    )
                    .build(),
            )
        }

        SavedPaymentMethodsPage(composeTestRule).clickNewCardButton()
        assertPrimaryButtonIsRevealedWhenCardFormBecomesComplete()

        testContext.markTestSucceeded()
    }

    @Test
    fun primaryButtonIsRevealedWhenVerticalModeFormBecomesComplete() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    defaultBillingDetails = defaultBillingDetailsWithoutPostalCode,
                    billingDetailsCollectionConfiguration = fullAddressCollection,
                ),
            )
        }

        VerticalModePage(composeTestRule).clickNewPaymentMethodButton(PaymentMethod.Type.Card.code)
        assertPrimaryButtonIsRevealedWhenCardFormBecomesComplete()

        testContext.markTestSucceeded()
    }

    @Test
    fun primaryButtonIsVisibleWhenCvcRecollectionBecomesComplete() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", PaymentMethod.Type.Card.code),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success.json")
        }
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        page.assertPrimaryButtonEnabled(enabled = false)
        page.focusCvcRecollection()
        page.waitForKeyboardToBeVisible()

        page.enterCvcRecollection("123")
        page.assertPrimaryButtonEnabled(enabled = true)
        page.assertPrimaryButtonVisibleAboveKeyboard()

        testContext.markTestSucceeded()
    }

    private fun assertPrimaryButtonIsRevealedWhenCardFormBecomesComplete() {
        page.fillOutCardDetails(fillOutZipCode = false)
        page.assertPrimaryButtonEnabled(enabled = false)
        page.focusZipCode()
        page.waitForKeyboardToBeVisible()
        page.assertPrimaryButtonBelowKeyboard()

        page.enterZipCode()
        page.assertPrimaryButtonEnabled(enabled = true)
        page.assertPrimaryButtonVisibleAboveKeyboard()
    }

    private companion object {
        val defaultBillingDetailsWithoutPostalCode = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "123 Main Street",
                city = "San Francisco",
                state = "CA",
                country = "US",
            ),
        )
        val fullAddressCollection = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
    }
}
