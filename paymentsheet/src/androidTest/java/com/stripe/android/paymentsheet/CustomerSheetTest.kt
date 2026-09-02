package com.stripe.android.paymentsheet

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.PaymentOptionSelection
import com.stripe.android.model.CardBrand
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.CustomerSheetTestType
import com.stripe.android.paymentsheet.utils.CustomerSheetUtils
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PrefsTestStore
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import com.stripe.android.testing.BurstParameter
import org.junit.Rule
import org.junit.Test

@Burst
internal class CustomerSheetTest(
    private val integrationType: IntegrationType = IntegrationType.Activity,
) {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: CustomerSheetPage = CustomerSheetPage(composeTestRule)

    @Test
    fun testSuccessfulCardSave(
        @BurstParameter customerSheetTestType: CustomerSheetTestType =
            burstValues(CustomerSheetTestType.AttachToCustomer),
    ) = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
            clientAttributionMetadataParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)
        CustomerSheetUtils.enqueueAttachRequests(
            networkRule = networkRule,
            customerSheetTestType = customerSheetTestType
        )

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

        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)

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

        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)

        context.presentCustomerSheet()

        networkRule.enqueue(
            CustomerSheetUtils.detachRequest(),
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
        @BurstParameter customerSheetTestType: CustomerSheetTestType =
            burstValues(CustomerSheetTestType.AttachToCustomer),
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
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

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
            clientAttributionMetadataParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)
        CustomerSheetUtils.enqueueAttachRequests(
            networkRule = networkRule,
            customerSheetTestType = customerSheetTestType
        )

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testSuccessfulCardSaveWithCardBrandChoice_Selector(
        @BurstParameter customerSheetTestType: CustomerSheetTestType =
            burstValues(CustomerSheetTestType.AttachToCustomer),
    ) = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

        context.presentCustomerSheet()

        /*
         * This card is overridden to use a test card compatible with CbcTestCardDelegate to skip
         * checking card account ranges network operation which run only if account ranges aren't
         * stores in memory.
         */
        page.fillOutCardDetails(
            cardNumber = TEST_CBC_CARD_NUMBER
        )
        page.selectCartesBancaire()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(cardNumber = TEST_CBC_CARD_NUMBER),
            cardBrandChoiceParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = true)
        CustomerSheetUtils.enqueueAttachRequests(
            networkRule = networkRule,
            customerSheetTestType = customerSheetTestType
        )

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
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
            clientAttributionMetadataParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            confirmSetupIntentRequest(),
            confirmSetupIntentParams()
        ) { response ->
            response.setResponseCode(402)
            response.testBodyFromFile("setup-intent-confirm_incorrect_cvc.json")
        }

        page.clickSaveButton()

        page.waitForText("Your card's security code is invalid.")

        context.markTestSucceeded()
    }
}
