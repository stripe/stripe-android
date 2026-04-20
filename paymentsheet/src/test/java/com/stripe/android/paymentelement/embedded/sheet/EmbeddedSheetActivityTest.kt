package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.EmbeddedNavigator
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupPaymentMethodUpdateResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.ManagePage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val networkRule = NetworkRule()

    private val formPage = FormPage(composeTestRule)
    private val managePage = ManagePage(composeTestRule)
    private val editPage = EditPage(composeTestRule)
    private val primaryButton = composeTestRule.onNode(
        hasTestTag(PRIMARY_BUTTON_TEST_TAG)
            .and(hasParent(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)))
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
        .around(RetryRule(3))
        .around(CheckoutInstancesTestRule())

    // region Form mode tests

    @Test
    fun `form - when launched without args should finish with error result`() {
        ActivityScenario.launchActivityForResult(
            EmbeddedSheetActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(EmbeddedSheetResult.Error::class.java)
        }
    }

    @Test
    fun `form - primary button is enabled when form is filled out`() = launchForm {
        primaryButton.assertIsNotEnabled()
        formPage.fillOutCardDetails()
        primaryButton.assertIsEnabled()
    }

    @Test
    fun `form - primary button label is correctly applied`() = launchForm(
        configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .primaryButtonLabel("Hi mom")
            .build()
    ) {
        primaryButton.assert(hasText("Hi mom"))
    }

    @Test
    fun `form - when FormActivityStateHelper has result, activity finishes with that result`() {
        launchForm { scenario ->
            scenario.onActivity { activity ->
                val formScreen = activity.embeddedNavigator.screen.value as EmbeddedNavigator.Screen.Form
                formScreen.setResult(
                    FormResult.Complete(
                        selection = null,
                        hasBeenConfirmed = true,
                        customerState = null,
                    )
                )
            }

            onIdle()

            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
            val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
            assertThat(result).isInstanceOf<EmbeddedSheetResult.Complete>()
        }
    }

    @Test
    fun `form - onDestroy clears checkout integration launched flag`() {
        val checkout = CheckoutStateFactory.createCheckout(applicationContext)
        CheckoutInstances.markIntegrationLaunched(CheckoutStateFactory.DEFAULT_KEY)

        launchForm(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = "cs_test",
                    instancesKey = CheckoutStateFactory.DEFAULT_KEY,
                ),
            ),
        ) { scenario ->
            scenario.onActivity { it.finish() }
        }

        // Enqueue a response so the mutation attempt doesn't fail due to missing network stub.
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        // If markIntegrationDismissed was not called, this would fail with
        // "Cannot mutate checkout session while a payment flow is presented."
        val result = runBlocking { checkout.applyPromotionCode("code") }
        assertThat(result.isSuccess).isTrue()
    }

    // endregion

    // region Manage mode tests

    @Test
    fun `manage - when a selection is passed in it is displayed as selected`() = launchManage(
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        managePage.assertLpmIsSelected(PaymentMethodFixtures.CARD_ID)
    }

    @Test
    fun `manage - selecting a payment method returns updated selection`() = launchManage {
        managePage.assertLpmIsNotSelected(PaymentMethodFixtures.CARD_ID)
        managePage.selectPaymentMethod(PaymentMethodFixtures.CARD_ID)
        assertCompletedResultSelection(PaymentMethodFixtures.CARD_ID)
    }

    @Test
    fun `manage - removing a payment method updates state when the user clicks back`() = launchManage {
        managePage.clickEdit()
        managePage.clickEdit(PaymentMethodFixtures.CARD_ID)
        editPage.waitUntilVisible()
        networkRule.setupPaymentMethodDetachResponse(PaymentMethodFixtures.CARD_ID)
        editPage.clickRemove()
        managePage.waitUntilVisible()
        managePage.waitUntilGone(PaymentMethodFixtures.CARD_ID)
        managePage.clickDone()
        Espresso.pressBack() // Close sheet.
        assertThat(completedResultPaymentMethods()).hasSize(defaultManagePaymentMethods().size - 1)
    }

    @Test
    fun `manage - removing last payment method closes the sheet`() = launchManage(
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    ) {
        networkRule.setupPaymentMethodDetachResponse(PaymentMethodFixtures.CARD_ID)
        editPage.clickRemove()
        assertThat(completedResultPaymentMethods()).isEmpty()
    }

    @Test
    fun `manage - removing last 2 payment methods closes the sheet`() = launchManage(
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
    fun `manage - updating card brand updates in list and returns a result with the new card brand`() = launchManage {
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
    fun `manage - updating card brand prevents sheet from being closed`() = launchManage {
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
    fun `manage - updating card brand returns a result with the new card brand`() {
        launchManage(
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
    fun `manage - onDestroy clears checkout integration launched flag`() {
        val checkout = CheckoutStateFactory.createCheckout(applicationContext)
        CheckoutInstances.markIntegrationLaunched(CheckoutStateFactory.DEFAULT_KEY)

        launchManage(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf()),
                hasCustomerConfiguration = true,
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
                canRemoveLastPaymentMethod = true,
                canUpdateFullPaymentMethodDetails = false,
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = "cs_test",
                    instancesKey = CheckoutStateFactory.DEFAULT_KEY,
                ),
            ),
        ) {
            Espresso.pressBack()
        }

        // Enqueue a response so the mutation attempt doesn't fail due to missing network stub.
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        // If markIntegrationDismissed was not called, this would fail with
        // "Cannot mutate checkout session while a payment flow is presented."
        val result = runBlocking { checkout.applyPromotionCode("code") }
        assertThat(result.isSuccess).isTrue()
    }

    // endregion

    // region Helpers

    private fun launchForm(
        selectedPaymentMethodCode: PaymentMethodCode = "card",
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        hasSavedPaymentMethods: Boolean = false,
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedSheetContract.Args(
                    mode = EmbeddedSheetContract.Mode.Form,
                    selectedPaymentMethodCode = selectedPaymentMethodCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hasSavedPaymentMethods = hasSavedPaymentMethods,
                    configuration = configuration,
                    paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                    statusBarColor = null,
                    selection = null,
                    customerState = createCustomerState(paymentMethods = emptyList()),
                    promotion = null,
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }

    private fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf()),
            hasCustomerConfiguration = true,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = false,
        ),
        paymentMethods: List<PaymentMethod> = defaultManagePaymentMethods(),
        selection: PaymentSelection? = null,
        block: ManageScenario.() -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedSheetContract.Args(
                    mode = EmbeddedSheetContract.Mode.Manage,
                    selectedPaymentMethodCode = selection?.paymentMethodType ?: "",
                    paymentMethodMetadata = paymentMethodMetadata,
                    hasSavedPaymentMethods = paymentMethods.isNotEmpty(),
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                    paymentElementCallbackIdentifier = "ManageActivityTestCallbackIdentifier",
                    statusBarColor = null,
                    selection = selection,
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                        paymentMethods = paymentMethods,
                    ),
                    promotion = null,
                ),
            )
        ).use { scenario ->
            ManageScenario(activityScenario = scenario).block()
        }
    }

    private fun defaultManagePaymentMethods(): List<PaymentMethod> {
        return listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            cbcCardDetails.createPaymentMethod(),
        )
    }

    private class ManageScenario(
        val activityScenario: ActivityScenario<EmbeddedSheetActivity>,
    ) {
        fun assertCompletedResultSelection(paymentMethodId: String?) {
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            val complete = result as EmbeddedSheetResult.Complete
            assertThat((complete.selection as PaymentSelection.Saved?)?.paymentMethod?.id)
                .isEqualTo(paymentMethodId)
        }

        fun completedResultPaymentMethods(): List<PaymentMethod> {
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            return (result as EmbeddedSheetResult.Complete).customerState!!.paymentMethods
        }
    }

    // endregion
}
