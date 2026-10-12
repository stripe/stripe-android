package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor.Companion.updateCardBrandErrorMessage
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_ERROR_MESSAGE_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.android.uicore.strings.resolve
import com.stripe.paymentelementtestpages.BillingDetailsPage
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityCheckoutTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val networkRule = NetworkRule()

    private val managePage = ManagePage(composeTestRule)
    private val editPage = EditPage(composeTestRule)
    private val billingDetailsPage = BillingDetailsPage(composeTestRule)

    private val selectedCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD
    private val updatedSelectedCard = selectedCard.copy(
        billingDetails = requireNotNull(selectedCard.billingDetails).toBuilder()
            .setAddress(UPDATED_ADDRESS)
            .build(),
    )
    private val secondCard = selectedCard.copy(
        id = SECOND_CARD_ID,
        billingDetails = requireNotNull(selectedCard.billingDetails).toBuilder()
            .setAddress(requireNotNull(selectedCard.billingDetails?.address).copy(line1 = "9 Market St"))
            .build(),
    )
    private val updatedSecondCard = secondCard.copy(
        billingDetails = requireNotNull(secondCard.billingDetails).toBuilder()
            .setAddress(UPDATED_ADDRESS)
            .build(),
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `selected single-card billing save waits for tax update and returns the refreshed checkout response`() {
        val paymentMethodResponseGate = ResponseGate()
        val taxResponseGate = ResponseGate()

        launch(
            paymentMethods = listOf(selectedCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(
                updatedSelectedCard,
                listOf(updatedSelectedCard),
                paymentMethodResponseGate,
            )
            enqueueTaxRegionUpdateResponse(
                updatedSelectedCard,
                listOf(updatedSelectedCard),
                responseGate = taxResponseGate,
            )

            try {
                saveBillingAddress(waitUntilComplete = false)

                composeTestRule.waitUntilWithIdle(conditionDescription = "payment method update request") {
                    composeTestRule.waitForIdle()
                    paymentMethodResponseGate.requestReceived.count == 0L
                }
                assertThat(taxResponseGate.requestReceived.count).isEqualTo(1)

                paymentMethodResponseGate.releaseResponse.countDown()

                composeTestRule.waitUntilWithIdle(conditionDescription = "tax region update request") {
                    composeTestRule.waitForIdle()
                    taxResponseGate.requestReceived.count == 0L
                }
                editPage.assertIsVisible()
                composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG).assertIsNotEnabled()

                taxResponseGate.releaseResponse.countDown()
                editPage.waitUntilMissing()

                assertSuccessfulBillingUpdateResult(
                    completedResult(),
                    updatedSelectedCard,
                    listOf(updatedSelectedCard),
                )
            } finally {
                paymentMethodResponseGate.releaseResponse.countDown()
                taxResponseGate.releaseResponse.countDown()
            }
        }
    }

    @Test
    fun `selected nested billing save is returned when manage closes`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, listOf(updatedSelectedCard, secondCard))
            enqueueTaxRegionUpdateResponse(updatedSelectedCard, listOf(updatedSelectedCard, secondCard))
            saveBillingAddress()
            managePage.waitUntilVisible()

            composeTestRule.onNodeWithContentDescription(
                applicationContext.getString(R.string.stripe_paymentsheet_close)
            ).performClick()

            assertSuccessfulBillingUpdateResult(
                completedResult(),
                updatedSelectedCard,
                listOf(updatedSelectedCard, secondCard),
            )
        }
    }

    @Test
    fun `selected nested billing save can retry after tax update fails`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, listOf(updatedSelectedCard, secondCard))
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[line1]", UPDATED_LINE1),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94111"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Invalid tax region"}}""")
            }
            saveBillingAddress()

            assertSaveErrorIsVisible()

            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, listOf(updatedSelectedCard, secondCard))
            enqueueTaxRegionUpdateResponse(updatedSelectedCard, listOf(updatedSelectedCard, secondCard))
            saveBillingAddress()
            managePage.waitUntilVisible()
            Espresso.pressBack()

            assertSuccessfulBillingUpdateResult(
                completedResult(),
                updatedSelectedCard,
                listOf(updatedSelectedCard, secondCard),
            )
        }
    }

    @Test
    fun `editing an unselected card does not update the checkout tax region`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(secondCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSecondCard, listOf(selectedCard, updatedSecondCard))
            saveBillingAddress()
            managePage.waitUntilVisible()
            Espresso.pressBack()

            val result = completedResult()
            val returnedSelection = result.selection as PaymentSelection.Saved
            assertThat(returnedSelection.paymentMethod.id).isEqualTo(selectedCard.id)
            val returnedSecondCard = requireNotNull(result.customerState)
                .paymentMethods
                .single { it.id == secondCard.id }
            assertThat(returnedSecondCard.billingDetails?.address).isEqualTo(UPDATED_ADDRESS)
            assertThat(result.checkoutSessionResponse).isNull()
            assertThat(result.shouldInvokeSelectionCallback).isFalse()
        }
    }

    @Test
    fun `selecting another card after billing save returns the row tax response`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            val updatedPaymentMethods = listOf(updatedSelectedCard, secondCard)
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, updatedPaymentMethods)
            enqueueTaxRegionUpdateResponse(updatedSelectedCard, updatedPaymentMethods)
            saveBillingAddress()
            managePage.waitUntilVisible()
            managePage.clickDone()
            managePage.assertLpmIsSelected(selectedCard.id)

            enqueueTaxRegionUpdateResponse(secondCard, updatedPaymentMethods, taxAmount = ROW_TAX_AMOUNT)
            managePage.selectPaymentMethod(secondCard.id)

            assertSuccessfulBillingUpdateResult(
                completedResult(),
                secondCard,
                updatedPaymentMethods,
                taxAmount = ROW_TAX_AMOUNT,
                shouldInvokeSelectionCallback = true,
            )
        }
    }

    @Test
    fun `failed row tax update after billing save preserves the edit response when manage closes`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            val updatedPaymentMethods = listOf(updatedSelectedCard, secondCard)
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, updatedPaymentMethods)
            enqueueTaxRegionUpdateResponse(updatedSelectedCard, updatedPaymentMethods)
            saveBillingAddress()
            managePage.waitUntilVisible()
            managePage.clickDone()

            val address = requireNotNull(secondCard.billingDetails?.address)
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", requireNotNull(address.country)),
                bodyPart("tax_region[line1]", requireNotNull(address.line1)),
                bodyPart("tax_region[city]", requireNotNull(address.city)),
                bodyPart("tax_region[state]", requireNotNull(address.state)),
                bodyPart("tax_region[postal_code]", requireNotNull(address.postalCode)),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Invalid tax region"}}""")
            }
            managePage.selectPaymentMethod(secondCard.id)

            val expectedError = applicationContext.getString(R.string.stripe_something_went_wrong)
            composeTestRule.waitUntilWithIdle {
                composeTestRule.onAllNodesWithText(expectedError)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            managePage.assertLpmIsSelected(selectedCard.id)
            composeTestRule.onNodeWithContentDescription(
                applicationContext.getString(R.string.stripe_paymentsheet_close)
            ).performClick()

            assertSuccessfulBillingUpdateResult(
                completedResult(),
                updatedSelectedCard,
                updatedPaymentMethods,
            )
        }
    }

    @Test
    fun `failed payment method save returns no tax response and preserves the selected card`() {
        launch(
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            networkRule.checkoutUpdate(
                bodyPart("payment_method_to_update[payment_method_id]", selectedCard.id),
                bodyPart("payment_method_to_update[billing_details][address][line1]", UPDATED_LINE1),
            ) { response ->
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Could not update card"}}""")
            }
            saveBillingAddress()
            assertSaveErrorIsVisible()
            Espresso.pressBack()
            managePage.waitUntilVisible()
            Espresso.pressBack()

            val result = completedResult()
            assertThat((result.selection as PaymentSelection.Saved).paymentMethod).isEqualTo(selectedCard)
            assertThat(requireNotNull(result.customerState).paymentMethods)
                .containsExactly(selectedCard, secondCard)
            assertThat(result.checkoutSessionResponse).isNull()
            assertThat(result.shouldInvokeSelectionCallback).isFalse()
        }
    }

    private fun assertSaveErrorIsVisible() {
        val expectedError = updateCardBrandErrorMessage.resolve(applicationContext)
        composeTestRule.waitUntilWithIdle {
            composeTestRule.onAllNodesWithTag(UPDATE_PM_ERROR_MESSAGE_TEST_TAG)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
        editPage.assertIsVisible()
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertIsEnabled()
    }

    private fun editBillingAddress() {
        billingDetailsPage.line1.performScrollTo().performTextReplacement(UPDATED_LINE1)
    }

    private fun saveBillingAddress(waitUntilComplete: Boolean = true) {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        editPage.update(waitUntilComplete = waitUntilComplete)
    }

    private fun checkoutPaymentMethodMetadata(): PaymentMethodMetadata {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            amount = BASE_AMOUNT,
            taxMeta = CheckoutSessionResponse.TaxMeta(
                CheckoutSessionResponse.TaxComputationType.AUTOMATIC,
                CheckoutSessionResponse.TaxStatus.COMPLETE,
            ),
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        return PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = checkoutSessionResponse.id,
                instancesKey = "test_instances_key",
                checkoutSessionResponse = checkoutSessionResponse,
                collectedEmail = null,
            ),
        ).copy(
            customerMetadata = CustomerMetadata.CheckoutSession(
                sessionId = checkoutSessionResponse.id,
                customerId = requireNotNull(selectedCard.customerId),
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            ),
        )
    }

    private fun enqueuePaymentMethodUpdateResponse(
        paymentMethod: PaymentMethod,
        paymentMethods: List<PaymentMethod>,
        responseGate: ResponseGate? = null,
    ) {
        networkRule.checkoutUpdate(
            bodyPart("payment_method_to_update[payment_method_id]", paymentMethod.id),
            bodyPart("payment_method_to_update[billing_details][address][line1]", UPDATED_LINE1),
        ) { response ->
            responseGate?.let { gate ->
                gate.requestReceived.countDown()
                check(gate.releaseResponse.await(10, TimeUnit.SECONDS))
            }
            response.setCheckoutSessionResponse(paymentMethods, taxAmount = 0)
        }
    }

    private fun enqueueTaxRegionUpdateResponse(
        paymentMethod: PaymentMethod,
        paymentMethods: List<PaymentMethod>,
        taxAmount: Long = EDIT_TAX_AMOUNT,
        responseGate: ResponseGate? = null,
    ) {
        val address = requireNotNull(paymentMethod.billingDetails?.address)
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", requireNotNull(address.country)),
            bodyPart("tax_region[line1]", requireNotNull(address.line1)),
            bodyPart("tax_region[city]", requireNotNull(address.city)),
            bodyPart("tax_region[state]", requireNotNull(address.state)),
            bodyPart("tax_region[postal_code]", requireNotNull(address.postalCode)),
        ) { response ->
            responseGate?.let { gate ->
                gate.requestReceived.countDown()
                check(gate.releaseResponse.await(10, TimeUnit.SECONDS))
            }
            response.setCheckoutSessionResponse(paymentMethods, taxAmount)
        }
    }

    private fun MockResponse.setCheckoutSessionResponse(
        paymentMethods: List<PaymentMethod>,
        taxAmount: Long,
    ) {
        testBodyFromFile("checkout-session-init.json") { json ->
            json.put(
                "customer",
                JSONObject()
                    .put("id", requireNotNull(paymentMethods.first().customerId))
                    .put("can_detach_payment_method", true)
                    .put("payment_methods", JSONArray(paymentMethods.map { it.toJsonWithBillingDetails() })),
            )
            json.put(
                "tax_context",
                JSONObject()
                    .put("automatic_tax_enabled", true)
                    .put("automatic_tax_address_source", "session.billing"),
            )
            json.put(
                "tax_meta",
                JSONObject()
                    .put("computation_type", "automatic")
                    .put("status", "complete"),
            )
            val item = json.getJSONArray("checkout_items").getJSONObject(0)
                .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
            item.put("total", BASE_AMOUNT + taxAmount)
            item.put("tax_exclusive", taxAmount)
            item.put(
                "tax_amounts",
                JSONArray().apply {
                    if (taxAmount > 0) {
                        put(
                            JSONObject()
                                .put("amount", taxAmount)
                                .put("inclusive", false)
                                .put(
                                    "tax_rate",
                                    JSONObject()
                                        .put("display_name", "Sales Tax")
                                        .put("percentage", taxAmount.toDouble() / BASE_AMOUNT * 100)
                                        .put("rate_type", "percentage"),
                                ),
                        )
                    }
                },
            )
            json.getJSONObject("server_built_elements_session_params")
                .getJSONObject("deferred_intent").put("amount", BASE_AMOUNT + taxAmount)
        }
    }

    private fun PaymentMethod.toJsonWithBillingDetails(): JSONObject {
        val billingDetails = requireNotNull(billingDetails)
        val address = requireNotNull(billingDetails.address)
        return PaymentMethodFactory.convertCardToJson(this).put(
            "billing_details",
            JSONObject()
                .put("name", billingDetails.name)
                .put("email", billingDetails.email)
                .put("phone", billingDetails.phone)
                .put(
                    "address",
                    JSONObject()
                        .put("country", address.country)
                        .put("line1", address.line1)
                        .put("line2", address.line2)
                        .put("city", address.city)
                        .put("state", address.state)
                        .put("postal_code", address.postalCode),
                ),
        )
    }

    private fun assertSuccessfulBillingUpdateResult(
        result: EmbeddedActivityResult.Complete,
        selectedPaymentMethod: PaymentMethod,
        paymentMethods: List<PaymentMethod>,
        taxAmount: Long = EDIT_TAX_AMOUNT,
        shouldInvokeSelectionCallback: Boolean = false,
    ) {
        val returnedSelection = (result.selection as PaymentSelection.Saved).paymentMethod
        assertThat(returnedSelection.id).isEqualTo(selectedPaymentMethod.id)
        assertThat(returnedSelection.billingDetails?.address).isEqualTo(selectedPaymentMethod.billingDetails?.address)

        val returnedPaymentMethods = requireNotNull(result.customerState).paymentMethods
        assertThat(returnedPaymentMethods.map { it.id }).containsExactlyElementsIn(paymentMethods.map { it.id })
        paymentMethods.forEach { expected ->
            val returned = returnedPaymentMethods.single { it.id == expected.id }
            assertThat(returned.billingDetails?.address).isEqualTo(expected.billingDetails?.address)
        }

        val checkoutSessionResponse = requireNotNull(result.checkoutSessionResponse)
        assertThat(checkoutSessionResponse.amount).isEqualTo(BASE_AMOUNT + taxAmount)
        assertThat(checkoutSessionResponse.automaticTaxEnabled).isTrue()
        assertThat(checkoutSessionResponse.taxAddressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.BILLING)
        assertThat(checkoutSessionResponse.taxMeta?.computationType)
            .isEqualTo(CheckoutSessionResponse.TaxComputationType.AUTOMATIC)
        assertThat(checkoutSessionResponse.taxMeta?.status)
            .isEqualTo(CheckoutSessionResponse.TaxStatus.COMPLETE)
        val item = checkoutSessionResponse.checkoutItems.single().oneTimePrice.items.single()
        assertThat(item.taxAmounts.single().amount).isEqualTo(taxAmount)
        assertThat(item.taxExclusive).isEqualTo(taxAmount)
        val checkoutPaymentMethods = requireNotNull(checkoutSessionResponse.customer).paymentMethods
        assertThat(checkoutPaymentMethods.map { it.id }).containsExactlyElementsIn(paymentMethods.map { it.id })
        paymentMethods.forEach { expected ->
            val returned = checkoutPaymentMethods.single { it.id == expected.id }
            assertThat(returned.billingDetails?.address).isEqualTo(expected.billingDetails?.address)
        }
        assertThat(result.shouldInvokeSelectionCallback).isEqualTo(shouldInvokeSelectionCallback)
    }

    private fun launch(
        paymentMethods: List<PaymentMethod>,
        selection: PaymentSelection.Saved,
        block: Scenario.() -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = checkoutPaymentMethodMetadata(),
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                        .build(),
                    productUsage = setOf("EmbeddedPaymentElement"),
                    paymentElementCallbackIdentifier = "EmbeddedSheetActivityCheckoutTestCallbackIdentifier",
                    statusBarColor = null,
                    selection = selection,
                    previousNewSelections = Bundle(),
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                        paymentMethods = paymentMethods,
                    ),
                    linkAccountInfo = LinkAccountUpdate.Value(null),
                    promotions = emptyList(),
                    launchMode = EmbeddedLaunchMode.Manage,
                    presentationState = EmbeddedActivityArgs.PresentationState.Ready,
                ),
            )
        ).use { scenario ->
            Scenario(
                activityScenario = scenario,
            ).block()
        }
    }

    private class Scenario(
        val activityScenario: ActivityScenario<EmbeddedSheetActivity>,
    ) {
        fun completedResult(): EmbeddedActivityResult.Complete {
            val result = EmbeddedSheetContract.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
            return result as EmbeddedActivityResult.Complete
        }
    }

    private data class ResponseGate(
        val requestReceived: CountDownLatch = CountDownLatch(1),
        val releaseResponse: CountDownLatch = CountDownLatch(1),
    )

    private companion object {
        const val BASE_AMOUNT = 5099L
        const val EDIT_TAX_AMOUNT = 510L
        const val ROW_TAX_AMOUNT = 255L
        const val SECOND_CARD_ID = "pm_987654321"
        const val UPDATED_LINE1 = "510 Townsend St"
        val UPDATED_ADDRESS = Address(
            city = "San Francisco",
            country = "US",
            line1 = UPDATED_LINE1,
            line2 = null,
            postalCode = "94111",
            state = "CA",
        )
    }
}
