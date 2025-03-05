package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.PaymentOptionSelection
import com.stripe.android.model.CardBrand
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.CustomerSheetTestType
import com.stripe.android.paymentsheet.utils.CustomerSheetTestTypeProvider
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.PrefsTestStore
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class CustomerSheetTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: CustomerSheetPage = CustomerSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun testSuccessfulCardSave(
        @TestParameter(valuesProvider = CustomerSheetTestTypeProvider::class)
        customerSheetTestType: CustomerSheetTestType,
    ) = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueueFetchRequests(withCards = false)

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueueFetchRequests(withCards = true)
        networkRule.enqueueAttachRequests(customerSheetTestType)

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testSavedCardReturnedInResultCallback() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)

            val selected = result as CustomerSheetResult.Selected

            assertThat(selected.selection).isInstanceOf(PaymentOptionSelection.PaymentMethod::class.java)

            val paymentMethodSelection = selected.selection as PaymentOptionSelection.PaymentMethod

            val card = paymentMethodSelection.paymentMethod.card

            assertThat(card?.last4).isEqualTo("4242")
            assertThat(card?.brand).isEqualTo(CardBrand.Visa)
        }
    ) { context ->
        context.scenario.onActivity {
            PrefsTestStore(it).clear()
        }

        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueueFetchRequests(withCards = true)

        context.presentCustomerSheet()

        page.clickSavedPaymentMethod(endsWith = "4242")
        page.clickConfirmButton()
    }

    @Test
    fun testDeleteSavedCard() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)

            val selected = result as CustomerSheetResult.Selected

            assertThat(selected.selection).isInstanceOf(PaymentOptionSelection.PaymentMethod::class.java)

            val paymentMethodSelection = selected.selection as PaymentOptionSelection.PaymentMethod

            val card = paymentMethodSelection.paymentMethod.card

            assertThat(card?.last4).isEqualTo("4242")
            assertThat(card?.brand).isEqualTo(CardBrand.Visa)
        }
    ) { context ->
        context.scenario.onActivity {
            PrefsTestStore(it).clear()
        }

        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueueFetchRequests(withCards = true)

        context.presentCustomerSheet()

        networkRule.enqueue(
            detachRequest(),
        ) { response ->
            response.testBodyFromFile("payment-methods-detach.json")
        }

        page.clickEditButton()
        page.clickModifyButton(forEndsWith = "4242")
        page.clickDeleteButton()
        page.clickDialogRemoveButton()

        page.waitUntilRemoved(text = "4242", substring = true)

        context.markTestSucceeded()
    }

    @Test
    fun testSuccessfulCardSaveWithFullBillingDetailsCollection(
        @TestParameter(valuesProvider = CustomerSheetTestTypeProvider::class)
        customerSheetTestType: CustomerSheetTestType,
    ) = runCustomerSheetTest(
        networkRule = networkRule,
        configuration = CustomerSheet.Configuration.builder("Merchant, Inc.")
            .billingDetailsCollectionConfiguration(
                PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
            .build(),
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueueFetchRequests(withCards = false)

        context.presentCustomerSheet()

        page.fillOutContactInformation()
        page.fillOutName()
        page.fillOutCardDetails()
        page.fillOutFullBillingAddress()
        page.closeKeyboard()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            fullBillingDetailsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueueFetchRequests(withCards = true)
        networkRule.enqueueAttachRequests(customerSheetTestType)

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testSuccessfulCardSaveWithCardBrandChoice(
        @TestParameter(valuesProvider = CustomerSheetTestTypeProvider::class)
        customerSheetTestType: CustomerSheetTestType,
    ) = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
        }

        networkRule.enqueueFetchRequests(withCards = false)

        context.presentCustomerSheet()

        /*
         * This card is overridden to use a test card compatible with CbcTestCardDelegate to skip
         * checking card account ranges network operation which run only if account ranges aren't
         * stores in memory.
         */
        page.fillOutCardDetails(
            cardNumber = TEST_CBC_CARD_NUMBER
        )
        page.changeCardBrandChoice()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(cardNumber = TEST_CBC_CARD_NUMBER),
            cardBrandChoiceParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueueFetchRequests(withCards = true)
        networkRule.enqueueAttachRequests(customerSheetTestType)

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testCardNotAttachedOnError() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        resultCallback = {
            error("Shouldn't call CustomerSheetResultCallback")
        }
    ) { context ->
        networkRule.enqueue(
            retrieveElementsSessionRequest(),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueueFetchRequests(withCards = false)

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            retrieveSetupIntentRequest(),
            retrieveSetupIntentParams(),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            confirmSetupIntentRequest(),
            confirmSetupIntentParams()
        ) { response ->
            response.setResponseCode(402)
            response.testBodyFromFile("setup-intent-confirm_incorrect_cvc.json")
        }

        page.clickSaveButton()

        page.waitForText("Your card's security code is incorrect.")

        context.markTestSucceeded()
    }

    private fun NetworkRule.enqueueFetchRequests(withCards: Boolean) {
        enqueue(
            retrievePaymentMethodsRequest(),
            cardPaymentMethodsParams(),
        ) { response ->
            val file = if (withCards) {
                "payment-methods-get-success.json"
            } else {
                "payment-methods-get-success-empty.json"
            }

            response.testBodyFromFile(file)
        }

        enqueue(
            retrievePaymentMethodsRequest(),
            usBankAccountPaymentMethodsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }
    }

    private fun NetworkRule.enqueueAttachRequests(customerSheetTestType: CustomerSheetTestType) {
        when (customerSheetTestType) {
            CustomerSheetTestType.AttachToCustomer -> {
                enqueue(
                    attachPaymentMethodRequest(),
                ) { response ->
                    response.testBodyFromFile("payment-methods-create.json")
                }
            }
            CustomerSheetTestType.AttachToSetupIntent -> {
                enqueue(
                    retrieveSetupIntentRequest(),
                    retrieveSetupIntentParams(),
                ) { response ->
                    response.testBodyFromFile("setup-intent-get.json")
                }

                enqueue(
                    confirmSetupIntentRequest(),
                    confirmSetupIntentParams()
                ) { response ->
                    response.testBodyFromFile("setup-intent-confirm.json")
                }
            }
            CustomerSheetTestType.CustomerSession -> Unit
        }
    }

    private fun retrieveElementsSessionRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        )
    }

    private fun retrievePaymentMethodsRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        )
    }

    private fun attachPaymentMethodRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/pm_12345/attach"),
        )
    }

    private fun detachRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/pm_67890/detach")
        )
    }

    private fun cardPaymentMethodsParams(): RequestMatcher {
        return RequestMatchers.composite(
            query("type", "card"),
        )
    }

    private fun usBankAccountPaymentMethodsParams(): RequestMatcher {
        return RequestMatchers.composite(
            query("type", "us_bank_account"),
        )
    }
}
