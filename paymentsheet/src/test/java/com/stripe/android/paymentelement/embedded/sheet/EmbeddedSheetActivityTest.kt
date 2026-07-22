package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupPaymentMethodUpdateResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import com.stripe.android.ui.core.R as StripeUiCoreR

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val networkRule = NetworkRule()

    private val managePage = ManagePage(composeTestRule)
    private val editPage = EditPage(composeTestRule)

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

    private fun launch(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf()),
            hasCustomerConfiguration = true,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateCardExpiryAndBillingDetails = false,
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
                    paymentElementCallbackIdentifier = "EmbeddedSheetActivityTestCallbackIdentifier",
                    statusBarColor = null,
                    selection = selection,
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                        paymentMethods = paymentMethods,
                    ),
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.Manage,
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
}
