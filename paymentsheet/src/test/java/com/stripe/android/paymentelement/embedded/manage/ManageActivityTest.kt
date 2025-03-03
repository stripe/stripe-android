package com.stripe.android.paymentelement.embedded.manage

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.network.CardPaymentMethodDetails
import com.stripe.android.network.setupPaymentMethodDetachResponse
import com.stripe.android.network.setupPaymentMethodUpdateResponse
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.EditPage
import com.stripe.android.paymentsheet.ManagePage
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ManageActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<ManageActivity>()
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
            ManageActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = ManageContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(ManageResult.Error::class.java)
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
    fun `updating card brand updates it in the list and returns a result with the new card brand`() = launch {
        managePage.waitUntilVisible()
        managePage.assertCardIsVisible(cbcCardId, "cartes_bancaries")
        managePage.clickEdit()
        managePage.clickEdit(cbcCardId)

        networkRule.setupPaymentMethodUpdateResponse(paymentMethodDetails = cbcCardDetails, cardBrand = "visa")
        editPage.waitUntilVisible()
        editPage.setCardBrand("Visa")
        editPage.update()
        managePage.waitUntilVisible()
        managePage.clickDone()
        managePage.assertCardIsVisible(cbcCardId, "visa")
        Espresso.pressBack()
        val updatedCbcCard = completedResultPaymentMethods().first { it.id == cbcCardId }
        assertThat(updatedCbcCard.card?.displayBrand).isEqualTo("visa")
    }

    @Test
    fun `updating card brand returns a result with the new card brand`() = launch(
        paymentMethods = listOf(cbcCardDetails.createPaymentMethod()),
    ) {
        networkRule.setupPaymentMethodUpdateResponse(paymentMethodDetails = cbcCardDetails, cardBrand = "visa")
        editPage.waitUntilVisible()
        editPage.setCardBrand("Visa")
        editPage.update()
        editPage.waitUntilMissing()
        val updatedCbcCard = completedResultPaymentMethods().single()
        assertThat(updatedCbcCard.card?.displayBrand).isEqualTo("visa")
    }

    private fun launch(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf()),
        ),
        paymentMethods: List<PaymentMethod> = defaultPaymentMethods(),
        selection: PaymentSelection? = null,
        block: Scenario.() -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<ManageActivity>(
            ManageContract.createIntent(
                context = applicationContext,
                input = ManageContract.Args(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                        paymentMethods = paymentMethods,
                    ),
                    selection = selection,
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
        val activityScenario: ActivityScenario<ManageActivity>,
    ) {
        fun assertCompletedResultSelection(paymentMethodId: String?) {
            val result = ManageContract.parseResult(0, activityScenario.result.resultData)
            assertThat(((result as ManageResult.Complete).selection as PaymentSelection.Saved?)?.paymentMethod?.id)
                .isEqualTo(paymentMethodId)
        }

        fun completedResultPaymentMethods(): List<PaymentMethod> {
            val result = ManageContract.parseResult(0, activityScenario.result.resultData)
            return (result as ManageResult.Complete).customerState.paymentMethods
        }
    }
}
