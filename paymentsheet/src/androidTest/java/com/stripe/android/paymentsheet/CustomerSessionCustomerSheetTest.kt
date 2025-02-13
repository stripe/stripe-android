package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.PaymentOptionSelection
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.utils.CustomerSheetTestType
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.json.JSONArray
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class CustomerSessionCustomerSheetTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: CustomerSheetPage = CustomerSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Ignore
    @Test
    fun testSuccessfulCardSave() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "4242",
                expectedBrand = CardBrand.Visa,
                result = result,
            )
        }
    ) { context ->
        enqueueElementsSession(cards = listOf())

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        enqueuePaymentMethodCreation()
        enqueueSetupIntentRetrieval()
        enqueueSetupIntentConfirmation()

        enqueueElementsSession(
            cards = listOf(
                PaymentMethodFactory.card(id = "pm_12345").update(
                    last4 = "4242",
                    addCbcNetworks = false,
                    brand = CardBrand.Visa,
                )
            )
        )

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Ignore
    @Test
    fun testSuccessfulCardSaveWithCardBrandChoice() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "1001",
                expectedBrand = CardBrand.CartesBancaires,
                result = result,
            )
        }
    ) { context ->
        enqueueElementsSession(
            cards = listOf(),
            isCbcEligible = true,
        )

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

        enqueueCbcPaymentMethodCreation()
        enqueueSetupIntentRetrieval()
        enqueueSetupIntentConfirmation()

        enqueueElementsSession(
            cards = listOf(
                PaymentMethodFactory.card(id = "pm_12345").update(
                    last4 = "1001",
                    addCbcNetworks = true,
                    brand = CardBrand.CartesBancaires,
                )
            ),
            isCbcEligible = true,
        )

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Ignore
    @Test
    fun testSepaSuccessfullyHiddenWhenDefaultPMsFeatureEnabled() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "4242",
                expectedBrand = CardBrand.Visa,
                result = result,
            )
        }
    ) { context ->
        enqueueElementsSession(
            cards = listOf(),
            sepaPaymentMethod = PaymentMethodFactory.sepaDebit(),
            isPaymentMethodSyncDefaultEnabled = true,
        )

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        enqueuePaymentMethodCreation()
        enqueueSetupIntentRetrieval()
        enqueueSetupIntentConfirmation()

        enqueueElementsSession(
            cards = listOf(
                PaymentMethodFactory.card(id = "pm_12345").update(
                    last4 = "4242",
                    addCbcNetworks = false,
                    brand = CardBrand.Visa,
                )
            ),
            sepaPaymentMethod = PaymentMethodFactory.sepaDebit(),
            isPaymentMethodSyncDefaultEnabled = true,
        )

        page.clickSaveButton()
        assertOnlySavedCardIsDisplayed()

        page.clickConfirmButton()
    }

    @Ignore
    @Test
    fun initialPaymentSelectionIsFirstPaymentMethodWhenDefaultPMsEnabled() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "4242",
                expectedBrand = CardBrand.Visa,
                result = result,
            )
        },
        confirmCompleted = false,
    ) { context ->
        val expectedPaymentMethod = PaymentMethodFactory.card()
        enqueueElementsSession(
            cards = listOf(expectedPaymentMethod),
            isPaymentMethodSyncDefaultEnabled = true,
        )

        val paymentMethodOption = context.retrievePaymentMethodOption()

        assertPaymentOptionSelectionIsExpectedPaymentMethod(
            customerSheetResult = paymentMethodOption,
            expectedPaymentMethod = expectedPaymentMethod,
        )
    }

    @Ignore
    @Test
    fun initialPaymentSelectionIsDefaultPaymentMethodWhenDefaultPMsEnabled() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "4242",
                expectedBrand = CardBrand.Visa,
                result = result,
            )
        },
        confirmCompleted = false,
    ) { context ->
        val expectedPaymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val otherPaymentMethodForUser = PaymentMethodFactory.card(id = "pm_2")
        enqueueElementsSession(
            cards = listOf(otherPaymentMethodForUser, expectedPaymentMethod),
            defaultPaymentMethodId = expectedPaymentMethod.id,
            isPaymentMethodSyncDefaultEnabled = true,
        )

        val paymentMethodOption = context.retrievePaymentMethodOption()

        assertPaymentOptionSelectionIsExpectedPaymentMethod(
            customerSheetResult = paymentMethodOption,
            expectedPaymentMethod = expectedPaymentMethod,
        )
    }

    private fun assertPaymentOptionSelectionIsExpectedPaymentMethod(
        customerSheetResult: CustomerSheetResult,
        expectedPaymentMethod: PaymentMethod,
    ) {
        assertThat(customerSheetResult).isInstanceOf(CustomerSheetResult.Selected::class.java)
        val selectedPaymentOption = customerSheetResult as CustomerSheetResult.Selected
        assertThat(selectedPaymentOption.selection).isInstanceOf(PaymentOptionSelection.PaymentMethod::class.java)
        val paymentMethodPaymentSelection = selectedPaymentOption.selection as PaymentOptionSelection.PaymentMethod
        assertThat(paymentMethodPaymentSelection.paymentMethod).isEqualTo(expectedPaymentMethod)
    }

    @Ignore
    @Test
    fun initialPaymentSelectionIgnoresSavedSelectionWhenDefaultPMsEnabled() = runCustomerSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        customerSheetTestType = CustomerSheetTestType.CustomerSession,
        resultCallback = { result ->
            verifySelected(
                expectedLast4 = "4242",
                expectedBrand = CardBrand.Visa,
                result = result,
            )
        },
        paymentMethodSavedSelection = PaymentMethodFactory.card().id,
        confirmCompleted = false,
    ) { context ->
        enqueueElementsSession(
            cards = emptyList(),
            isPaymentMethodSyncDefaultEnabled = true,
        )

        val paymentMethodOption = context.retrievePaymentMethodOption()

        assertThat(paymentMethodOption).isInstanceOf(CustomerSheetResult.Selected::class.java)
        val selectedPaymentOption = paymentMethodOption as CustomerSheetResult.Selected
        assertThat(selectedPaymentOption.selection).isNull()
    }

    @Test
    fun initialPaymentSelectionUsesSavedSelectionWhenDefaultPMsIsNotEnabled() {
        val expectedPaymentMethod = PaymentMethodFactory.card(id = "pm_1")
        runCustomerSheetTest(
            networkRule = networkRule,
            integrationType = integrationType,
            customerSheetTestType = CustomerSheetTestType.CustomerSession,
            resultCallback = { result ->
                verifySelected(
                    expectedLast4 = "4242",
                    expectedBrand = CardBrand.Visa,
                    result = result,
                )
            },
            paymentMethodSavedSelection = expectedPaymentMethod.id,
            confirmCompleted = false,
        ) { context ->
            enqueueElementsSession(
                cards = listOf(PaymentMethodFactory.card(id = "pm_2"), expectedPaymentMethod),
                isPaymentMethodSyncDefaultEnabled = false,
            )

            val paymentMethodOption = context.retrievePaymentMethodOption()

            assertPaymentOptionSelectionIsExpectedPaymentMethod(
                paymentMethodOption,
                expectedPaymentMethod
            )
        }
    }

    private fun assertOnlySavedCardIsDisplayed() {
        val savedPaymentMethodMatcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
            .and(hasText("4242", substring = true))

        page.waitUntil(savedPaymentMethodMatcher)
        assertThat(
            composeTestRule.onAllNodesWithTag(SAVED_PAYMENT_OPTION_TEST_TAG).fetchSemanticsNodes().size
        ).isEqualTo(1)
    }

    private fun enqueueElementsSession(
        cards: List<PaymentMethod>,
        defaultPaymentMethodId: String? = null,
        sepaPaymentMethod: PaymentMethod? = null,
        isPaymentMethodSyncDefaultEnabled: Boolean = false,
        isCbcEligible: Boolean = false,
    ) {
        val paymentMethodsArray = JSONArray()

        cards.forEach { card ->
            paymentMethodsArray.put(PaymentMethodFactory.convertCardToJson(card))
        }

        sepaPaymentMethod?.let {
            paymentMethodsArray.put(PaymentMethodFactory.convertSepaPaymentMethodToJson(sepaPaymentMethod))
        }

        val syncDefaultFeature = if (isPaymentMethodSyncDefaultEnabled) {
            "enabled"
        } else {
            "disabled"
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                filename = "elements-sessions-requires_pm_with_cs.json",
                replacements = listOf(
                    ResponseReplacement(
                        original = "[PAYMENT_METHODS_HERE]",
                        new = paymentMethodsArray.toString(2),
                    ),
                    ResponseReplacement(
                        original = "CARD_BRAND_CHOICE_ELIGIBILITY",
                        new = isCbcEligible.toString(),
                    ),
                    ResponseReplacement(
                        original = "PAYMENT_METHOD_SYNC_DEFAULT_FEATURE",
                        new = syncDefaultFeature,
                    ),
                    ResponseReplacement(
                        original = "[DEFAULT_PAYMENT_METHOD_ID]",
                        new = defaultPaymentMethodId.toString()
                    ),
                ),
            )
        }
    }

    private fun enqueuePaymentMethodCreation() {
        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(),
            billingDetailsParams(),
            bodyPart("allow_redisplay", "always"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
    }

    private fun enqueueCbcPaymentMethodCreation() {
        networkRule.enqueue(
            createPaymentMethodsRequest(),
            cardDetailsParams(TEST_CBC_CARD_NUMBER),
            cardBrandChoiceParams(),
            billingDetailsParams(),
            bodyPart("allow_redisplay", "always"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
    }

    private fun enqueueSetupIntentRetrieval() {
        networkRule.enqueue(
            retrieveSetupIntentRequest(),
            retrieveSetupIntentParams(),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }
    }

    private fun enqueueSetupIntentConfirmation() {
        networkRule.enqueue(
            confirmSetupIntentRequest(),
            confirmSetupIntentParams(),
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }
    }

    private fun verifySelected(
        expectedLast4: String,
        expectedBrand: CardBrand,
        result: CustomerSheetResult
    ) {
        assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)

        val selected = result as CustomerSheetResult.Selected

        assertThat(selected.selection).isInstanceOf(PaymentOptionSelection.PaymentMethod::class.java)

        val paymentMethodSelection = selected.selection as PaymentOptionSelection.PaymentMethod

        val card = paymentMethodSelection.paymentMethod.card

        assertThat(card?.last4).isEqualTo(expectedLast4)
        assertThat(card?.brand).isEqualTo(expectedBrand)
    }
}
