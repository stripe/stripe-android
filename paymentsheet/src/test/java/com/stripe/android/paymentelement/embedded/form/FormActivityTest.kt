package com.stripe.android.paymentelement.embedded.form

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
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.manage.ManageActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.FormPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FormActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<ManageActivity>()
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
            FormActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = FormContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(FormResult.Cancelled::class.java)
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

    private fun launch(
        selectedPaymentMethodCode: PaymentMethodCode = "card",
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        hasSavedPaymentMethods: Boolean = false,
        configuration: EmbeddedPaymentElement.Configuration =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        intentConfiguration: PaymentSheet.IntentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 5000,
                currency = "USD",
            ),
        ),
        block: () -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<FormActivity>(
            FormContract.createIntent(
                context = applicationContext,
                input = FormContract.Args(
                    selectedPaymentMethodCode = selectedPaymentMethodCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hasSavedPaymentMethods = hasSavedPaymentMethods,
                    configuration = configuration,
                    initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                    paymentSelection = null,
                ),
            )
        ).use { scenario ->
            block()
        }
    }
}
