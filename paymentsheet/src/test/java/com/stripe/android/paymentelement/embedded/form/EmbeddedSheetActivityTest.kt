package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.BillingDetailsPage
import com.stripe.paymentelementtestpages.FormPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.paymentsheet.R as PaymentSheetR

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val networkRule = NetworkRule()

    private val formPage = FormPage(composeTestRule)
    private val billingDetailsPage = BillingDetailsPage(composeTestRule)
    private val primaryButton = composeTestRule.onNode(
        hasTestTag(PRIMARY_BUTTON_TEST_TAG)
            .and(hasParent(hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG)))
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `when launched without args should finish with cancelled result`() {
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
    fun `primary button is enabled when form is filled out`() = launch {
        primaryButton.assertIsNotEnabled()
        formPage.fillOutCardDetails()
        primaryButton.assertIsEnabled()
    }

    @Test
    fun `processing shows spinner and blocks back on form`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.updateProcessing(true)
        }
        composeTestRule.waitForIdle()
        primaryButton.performScrollTo()

        composeTestRule.onNodeWithText(
            applicationContext.getString(PaymentSheetR.string.stripe_paymentsheet_primary_button_processing)
        ).assertIsDisplayed()
        pressBack()
        onIdle()
        scenario.onActivity { activity ->
            assertThat(activity.embeddedNavigator.screen.value)
                .isInstanceOf<EmbeddedNavigator.Screen.Form>()
        }
    }

    @Test
    fun `checkout Continue displays error and keeps form open`() {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        launch(paymentMethodMetadata = checkoutPaymentMethodMetadata()) { scenario ->
            val expectedError = applicationContext.getString(PaymentSheetR.string.stripe_something_went_wrong)
            fillOutCheckoutCard()
            primaryButton.performScrollTo().assertIsEnabled().performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodes(hasText(expectedError))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            primaryButton.assertIsEnabled()
            scenario.onActivity { activity ->
                assertThat(activity.embeddedNavigator.screen.value)
                    .isInstanceOf<EmbeddedNavigator.Screen.Form>()
            }
        }
    }

    @Test
    fun `non-checkout Continue immediately returns without checkout response`() = launch { scenario ->
        formPage.fillOutCardDetails()
        primaryButton.performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

        onIdle()
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val parsedResult = EmbeddedSheetContract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        )
        assertThat(parsedResult).isInstanceOf<EmbeddedActivityResult.Complete>()
        val result = parsedResult as EmbeddedActivityResult.Complete
        assertThat(result.checkoutSessionResponse).isNull()
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    @Test
    fun `Primary button label is correctly applied`() = launch(
        configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .primaryButtonLabel("Hi mom")
            .build()
    ) {
        primaryButton.assert(hasText("Hi mom"))
    }

    private fun fillOutCheckoutCard() {
        formPage.waitUntilVisible()
        formPage.fillOutCardDetails()
        billingDetailsPage.line1.performTextReplacement("510 Townsend St")
        billingDetailsPage.city.performTextReplacement("San Francisco")
        billingDetailsPage.state.performScrollTo().performClick()
        composeTestRule.onNodeWithText("California").performClick()
        billingDetailsPage.zipCode.performTextReplacement("94103")
    }

    private fun checkoutPaymentMethodMetadata(): PaymentMethodMetadata {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        return PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = response.id,
                instancesKey = "test_instances_key",
                checkoutSessionResponse = response,
            ),
        )
    }

    @Test
    fun `When SheetActivityStateHolder has result, activity finishes with that result`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.setResult(
                EmbeddedActivityResult.Complete(
                    previousNewSelections = Bundle(),
                    selection = null,
                    hasBeenConfirmed = true,
                    customerState = null,
                    checkoutSessionResponse = null,
                    shouldInvokeSelectionCallback = false,
                    launchMode = EmbeddedLaunchMode.Form(
                        selectedPaymentMethodCode = "card",
                    ),
                )
            )
        }

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Complete>()
    }

    @Test
    fun `when dismissed, finishes with cancelled result preserving the form launch mode`() = launch(
        selectedPaymentMethodCode = "card",
    ) { scenario ->
        pressBack()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        assertThat((result as EmbeddedActivityResult.Cancelled).launchMode)
            .isEqualTo(
                EmbeddedLaunchMode.Form(
                    selectedPaymentMethodCode = "card",
                )
            )
    }

    @Test
    fun `when dismissed, finishes with cancelled result preserving a non-card form launch mode`() = launch(
        selectedPaymentMethodCode = "cashapp",
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        ),
    ) { scenario ->
        pressBack()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        assertThat((result as EmbeddedActivityResult.Cancelled).launchMode)
            .isEqualTo(
                EmbeddedLaunchMode.Form(
                    selectedPaymentMethodCode = "cashapp",
                )
            )
    }

    private fun launch(
        selectedPaymentMethodCode: PaymentMethodCode = "card",
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = configuration,
                    productUsage = setOf("EmbeddedPaymentElement"),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                    selection = null,
                    previousNewSelections = Bundle(),
                    customerState = createCustomerState(paymentMethods = emptyList()),
                    promotions = emptyList(),
                    launchMode = EmbeddedLaunchMode.Form(
                        selectedPaymentMethodCode = selectedPaymentMethodCode,
                    ),
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
