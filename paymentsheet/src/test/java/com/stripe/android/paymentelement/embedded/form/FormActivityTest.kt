package com.stripe.android.paymentelement.embedded.form

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
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.InternalState
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetArgs
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetContract
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetResult
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetScreen
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetViewModel
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.FormPage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class FormActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val networkRule = NetworkRule()

    private val formPage = FormPage(composeTestRule)
    private val primaryButton = composeTestRule.onNode(
        hasTestTag(PRIMARY_BUTTON_TEST_TAG)
            .and(hasParent(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)))
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `when launched without args should finish with cancelled result`() {
        ActivityScenario.launchActivityForResult(
            EmbeddedSheetActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = EmbeddedSheetContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(EmbeddedSheetResult.Form::class.java)
            assertThat((result as EmbeddedSheetResult.Form).formResult).isInstanceOf(FormResult.Cancelled::class.java)
        }
    }

    @Test
    fun `primary button is enabled when form is filled out`() = launch {
        primaryButton.assertIsNotEnabled()
        formPage.fillOutCardDetails()
        primaryButton.assertIsEnabled()
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

    @Test
    fun `When FormActivityStateHelper has result, activity finishes with that result`() = launch { scenario ->
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[EmbeddedSheetViewModel::class.java]
            val formScreen = viewModel.navigator.currentScreen.value as EmbeddedSheetScreen.Form
            formScreen.stateHelper.setResult(
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
        assertThat(result).isInstanceOf<EmbeddedSheetResult.Form>()
        assertThat((result as EmbeddedSheetResult.Form).formResult).isInstanceOf<FormResult.Complete>()
    }

    @Test
    fun `onDestroy clears checkout integration launched flag`() {
        val instancesKey = "test-checkout-key"
        val checkout = Checkout.createWithState(
            applicationContext,
            Checkout.State(
                InternalState(
                    key = instancesKey,
                    checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
                ),
            ),
        )
        CheckoutInstances.markIntegrationLaunched(instancesKey)

        launch(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = "cs_test",
                    instancesKey = instancesKey,
                ),
            ),
        ) { scenario ->
            scenario.onActivity { it.finish() }
        }

        // Enqueue a response so the mutation attempt doesn't fail due to missing network stub.
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_pages/cs_test_abc123"),
        ) { response ->
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }

        // If markIntegrationDismissed was not called, this would fail with
        // "Cannot mutate checkout session while a payment flow is presented."
        val result = runBlocking { checkout.applyPromotionCode("code") }
        assertThat(result.isSuccess).isTrue()
    }

    private fun launch(
        selectedPaymentMethodCode: PaymentMethodCode = "card",
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        hasSavedPaymentMethods: Boolean = false,
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        val formArgs = FormContract.Args(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            configuration = configuration,
            statusBarColor = null,
            paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
            paymentSelection = null,
            customerState = createCustomerState(paymentMethods = emptyList()),
        )
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedSheetArgs.Form(formArgs),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
