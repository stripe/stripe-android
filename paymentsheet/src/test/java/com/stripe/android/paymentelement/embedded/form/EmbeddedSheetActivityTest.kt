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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.FormPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityTest {
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
    fun `Primary button label is correctly applied`() = launch(
        configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .primaryButtonLabel("Hi mom")
            .build()
    ) {
        primaryButton.assert(hasText("Hi mom"))
    }

    @Test
    fun `When SheetActivityStateHolder has result, activity finishes with that result`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.setResult(
                EmbeddedActivityResult.Complete(
                    selection = null,
                    hasBeenConfirmed = true,
                    customerState = null,
                    shouldInvokeSelectionCallback = false,
                    launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
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
            .isEqualTo(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"))
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
            .isEqualTo(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "cashapp"))
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
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                    selection = null,
                    customerState = createCustomerState(paymentMethods = emptyList()),
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = selectedPaymentMethodCode),
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
