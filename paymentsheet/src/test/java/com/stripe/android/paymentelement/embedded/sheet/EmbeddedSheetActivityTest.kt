package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
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
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.TestApiKeys
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
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.strings.resolve
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupPaymentMethodUpdateResponse
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
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityTest {
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

    private val cbcCardId = "pm_54321"
    private val cbcCardDetails = CardPaymentMethodDetails(
        id = cbcCardId,
        last4 = "1001",
        addCbcNetworks = true,
        brand = CardBrand.CartesBancaires,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `when launched without args should finish with error result`() {
        ActivityScenario.launchActivityForResult(
            EmbeddedSheetActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(EmbeddedActivityResult.Error::class.java)
        }
    }

    @Test
    fun `when a selection is passed in it is displayed as selected`() = launch(
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        managePage.assertLpmIsSelected(PaymentMethodFixtures.CARD_ID)
    }

    @Test
    fun `selecting a payment method returns updated selection`() = launch {
        managePage.assertLpmIsNotSelected(PaymentMethodFixtures.CARD_ID)
        managePage.selectPaymentMethod(PaymentMethodFixtures.CARD_ID)
        assertCompletedResultSelection(PaymentMethodFixtures.CARD_ID)
    }

    @Test
    fun `removing a payment method updates state when the user clicks back`() = launch {
        managePage.clickEdit()
        managePage.clickEdit(PaymentMethodFixtures.CARD_ID)
        editPage.waitUntilVisible()
        networkRule.setupPaymentMethodDetachResponse(PaymentMethodFixtures.CARD_ID)
        editPage.clickRemove()
        managePage.waitUntilVisible()
        managePage.waitUntilGone(PaymentMethodFixtures.CARD_ID)
        managePage.clickDone()
        Espresso.pressBack() // Close sheet.
        assertThat(completedResultPaymentMethods()).hasSize(defaultPaymentMethods().size - 1)
    }

    @Test
    fun `removing last payment method closes the sheet`() = launch(
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    ) {
        networkRule.setupPaymentMethodDetachResponse(PaymentMethodFixtures.CARD_ID)
        editPage.clickRemove()
        assertThat(completedResultPaymentMethods()).isEmpty()
    }

    @Test
    fun `removing last 2 payment method closes the sheet`() = launch(
        paymentMethods = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            cbcCardDetails.createPaymentMethod(),
        )
    ) {
        managePage.clickEdit()
        managePage.clickEdit(PaymentMethodFixtures.CARD_ID)
        editPage.waitUntilVisible()
        networkRule.setupPaymentMethodDetachResponse(PaymentMethodFixtures.CARD_ID)
        editPage.clickRemove()

        managePage.waitUntilVisible()
        managePage.waitUntilGone(PaymentMethodFixtures.CARD_ID)
        managePage.clickEdit(cbcCardId)
        editPage.waitUntilVisible()
        networkRule.setupPaymentMethodDetachResponse(cbcCardId)
        editPage.clickRemove()

        assertThat(completedResultPaymentMethods()).isEmpty()
    }

    @Test
    fun `updating card brand updates in list and returns a result with the new card brand`() = launch {
        managePage.waitUntilVisible()
        managePage.assertCardIsVisible(cbcCardId, "cartes_bancaries")
        managePage.clickEdit()
        managePage.clickEdit(cbcCardId)

        networkRule.setupPaymentMethodUpdateResponse(paymentMethodDetails = cbcCardDetails, cardBrand = "visa")
        editPage.waitUntilVisible()
        editPage.setCardBrandWithSelector("Visa")
        editPage.update()
        managePage.waitUntilVisible()
        managePage.clickDone()
        managePage.assertCardIsVisible(cbcCardId, "visa")
        Espresso.pressBack()
        val updatedCbcCard = completedResultPaymentMethods().first { it.id == cbcCardId }
        assertThat(updatedCbcCard.card?.displayBrand).isEqualTo("visa")
    }

    @Test
    fun `updating card brand prevents sheet from being closed`() = launch {
        managePage.waitUntilVisible()
        managePage.assertCardIsVisible(cbcCardId, "cartes_bancaries")
        managePage.clickEdit()
        managePage.clickEdit(cbcCardId)

        val countDownLatch = CountDownLatch(1)
        networkRule.setupPaymentMethodUpdateResponse(
            paymentMethodDetails = cbcCardDetails,
            cardBrand = "visa",
            countDownLatch = countDownLatch,
        )
        editPage.waitUntilVisible()
        editPage.setCardBrandWithSelector("Visa")
        editPage.update(waitUntilComplete = false)
        Espresso.pressBack()
        managePage.assertNotVisible()
        countDownLatch.countDown()
        managePage.waitUntilVisible()
        managePage.clickDone()
        managePage.assertCardIsVisible(cbcCardId, "visa")
    }

    @Test
    fun `top bar navigation button is disabled while updating card brand`() = launch {
        managePage.waitUntilVisible()
        managePage.clickEdit()
        managePage.clickEdit(cbcCardId)

        val countDownLatch = CountDownLatch(1)
        networkRule.setupPaymentMethodUpdateResponse(
            paymentMethodDetails = cbcCardDetails,
            cardBrand = "visa",
            countDownLatch = countDownLatch,
        )
        editPage.waitUntilVisible()
        editPage.setCardBrandWithSelector("Visa")
        editPage.update(waitUntilComplete = false)

        // While the update is in flight the top bar navigation button must be disabled,
        // matching the blocked system back button and swipe-to-dismiss.
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG).assertIsNotEnabled()

        countDownLatch.countDown()
        managePage.waitUntilVisible()

        // Once the update completes the navigation button is re-enabled.
        composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG).assertIsEnabled()
    }

    @Test
    fun `top bar shows close icon on manage list and back icon on edit screen`() = launch {
        managePage.waitUntilVisible()

        // The manage list is the root screen, so the navigation button closes the sheet.
        composeTestRule.onNodeWithContentDescription(
            applicationContext.getString(R.string.stripe_paymentsheet_close)
        ).assertIsDisplayed()

        managePage.clickEdit()
        managePage.clickEdit(cbcCardId)
        editPage.waitUntilVisible()

        // The edit screen is pushed on top, so the navigation button goes back.
        composeTestRule.onNodeWithContentDescription(
            applicationContext.getString(StripeUiCoreR.string.stripe_back)
        ).assertIsDisplayed()
    }

    @Test
    fun `updating card brand returns a result with the new card brand`() {
        launch(
            paymentMethods = listOf(cbcCardDetails.createPaymentMethod()),
        ) {
            networkRule.setupPaymentMethodUpdateResponse(paymentMethodDetails = cbcCardDetails, cardBrand = "visa")
            editPage.waitUntilVisible()
            editPage.setCardBrandWithSelector("Visa")
            editPage.update()
            editPage.waitUntilMissing()
            val updatedCbcCard = completedResultPaymentMethods().single()
            assertThat(updatedCbcCard.card?.displayBrand).isEqualTo("visa")
        }
    }

    @Test
    fun `selected single-card billing save waits for tax update and returns the refreshed checkout response`() {
        val paymentMethodResponseGate = ResponseGate()
        val taxResponseGate = ResponseGate()

        launch(
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
            paymentMethods = listOf(selectedCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard, paymentMethodResponseGate)
            enqueueTaxRegionUpdateResponse(updatedSelectedCard, taxResponseGate)

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

                assertSuccessfulBillingUpdateResult(completedResult(), updatedSelectedCard)
            } finally {
                paymentMethodResponseGate.releaseResponse.countDown()
                taxResponseGate.releaseResponse.countDown()
            }
        }
    }

    @Test
    fun `selected nested billing save is returned when manage closes`() {
        launch(
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard)
            enqueueTaxRegionUpdateResponse(updatedSelectedCard)
            saveBillingAddress()
            managePage.waitUntilVisible()

            composeTestRule.onNodeWithContentDescription(
                applicationContext.getString(R.string.stripe_paymentsheet_close)
            ).performClick()

            assertSuccessfulBillingUpdateResult(completedResult(), updatedSelectedCard)
        }
    }

    @Test
    fun `selected nested billing save can retry after tax update fails`() {
        launch(
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(selectedCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSelectedCard)
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

            val expectedError = updateCardBrandErrorMessage.resolve(applicationContext)
            composeTestRule.waitUntilWithIdle {
                composeTestRule.onAllNodesWithTag(UPDATE_PM_ERROR_MESSAGE_TEST_TAG)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            editPage.assertIsVisible()
            composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertIsEnabled()

            enqueuePaymentMethodUpdateResponse(updatedSelectedCard)
            enqueueTaxRegionUpdateResponse(updatedSelectedCard)
            saveBillingAddress()
            managePage.waitUntilVisible()
            Espresso.pressBack()

            assertSuccessfulBillingUpdateResult(completedResult(), updatedSelectedCard)
        }
    }

    @Test
    fun `editing an unselected card does not update the checkout tax region`() {
        launch(
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
            paymentMethods = listOf(selectedCard, secondCard),
            selection = PaymentSelection.Saved(selectedCard),
        ) {
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(secondCard.id)
            editPage.waitUntilVisible()
            editBillingAddress()
            enqueuePaymentMethodUpdateResponse(updatedSecondCard)
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
            response.setCheckoutSessionResponse(paymentMethod = paymentMethod)
        }
    }

    private fun enqueueTaxRegionUpdateResponse(
        paymentMethod: PaymentMethod,
        responseGate: ResponseGate? = null,
    ) {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", UPDATED_LINE1),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94111"),
        ) { response ->
            responseGate?.let { gate ->
                gate.requestReceived.countDown()
                check(gate.releaseResponse.await(10, TimeUnit.SECONDS))
            }
            response.setCheckoutSessionResponse(paymentMethod = paymentMethod, taxRegionUpdated = true)
        }
    }

    private fun MockResponse.setCheckoutSessionResponse(
        paymentMethod: PaymentMethod,
        taxRegionUpdated: Boolean = false,
    ) {
        testBodyFromFile("checkout-session-init.json") { json ->
            json.put(
                "customer",
                JSONObject()
                    .put("id", requireNotNull(paymentMethod.customerId))
                    .put("can_detach_payment_method", true)
                    .put("payment_methods", JSONArray().put(paymentMethod.toJsonWithBillingDetails())),
            )
            if (taxRegionUpdated) {
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
            }
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
        updatedPaymentMethod: PaymentMethod,
    ) {
        val selectedPaymentMethod = (result.selection as PaymentSelection.Saved).paymentMethod
        assertThat(selectedPaymentMethod.id).isEqualTo(updatedPaymentMethod.id)
        assertThat(selectedPaymentMethod.billingDetails?.address).isEqualTo(UPDATED_ADDRESS)

        val returnedCustomerPaymentMethod = requireNotNull(result.customerState)
            .paymentMethods
            .single { it.id == updatedPaymentMethod.id }
        assertThat(returnedCustomerPaymentMethod.billingDetails?.address).isEqualTo(UPDATED_ADDRESS)

        val checkoutSessionResponse = requireNotNull(result.checkoutSessionResponse)
        assertThat(checkoutSessionResponse.amount).isEqualTo(5099L)
        assertThat(checkoutSessionResponse.automaticTaxEnabled).isTrue()
        assertThat(checkoutSessionResponse.taxAddressSource)
            .isEqualTo(CheckoutSessionResponse.TaxAddressSource.BILLING)
        assertThat(checkoutSessionResponse.taxMeta?.computationType)
            .isEqualTo(CheckoutSessionResponse.TaxComputationType.AUTOMATIC)
        assertThat(checkoutSessionResponse.taxMeta?.status)
            .isEqualTo(CheckoutSessionResponse.TaxStatus.COMPLETE)
        val checkoutCustomerPaymentMethod = requireNotNull(checkoutSessionResponse.customer)
            .paymentMethods
            .single { it.id == updatedPaymentMethod.id }
        assertThat(checkoutCustomerPaymentMethod.billingDetails?.address).isEqualTo(UPDATED_ADDRESS)
        assertThat(result.shouldInvokeSelectionCallback).isFalse()
    }

    private fun launch(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf()),
            hasCustomerConfiguration = true,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateCardExpiryAndBillingDetails = false,
            customerEphemeralKeySecret = TestApiKeys.EPHEMERAL,
        ),
        paymentMethods: List<PaymentMethod> = defaultPaymentMethods(),
        selection: PaymentSelection? = null,
        block: Scenario.() -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                        .build(),
                    productUsage = setOf("EmbeddedPaymentElement"),
                    paymentElementCallbackIdentifier = "EmbeddedSheetActivityTestCallbackIdentifier",
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

    private fun defaultPaymentMethods(): List<PaymentMethod> {
        return listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            cbcCardDetails.createPaymentMethod(),
        )
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

        fun assertCompletedResultSelection(paymentMethodId: String?) {
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            val savedSelection = (result as EmbeddedActivityResult.Complete).selection as PaymentSelection.Saved?
            assertThat(savedSelection?.paymentMethod?.id)
                .isEqualTo(paymentMethodId)
        }

        fun completedResultPaymentMethods(): List<PaymentMethod> {
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            return (result as EmbeddedActivityResult.Complete).customerState!!.paymentMethods
        }
    }

    private data class ResponseGate(
        val requestReceived: CountDownLatch = CountDownLatch(1),
        val releaseResponse: CountDownLatch = CountDownLatch(1),
    )

    private companion object {
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
