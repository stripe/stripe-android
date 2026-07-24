package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.FormPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HorizontalFormEmbeddedSheetActivityTest {
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
    fun `Form launch mode with horizontal layout shows the horizontal payment method tabs`() = launch(
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    ) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).assertIsDisplayed()
        primaryButton.assertExists()
    }

    @Test
    fun `Form launch mode with vertical layout shows the vertical form without tabs`() = launch(
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
    ) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).assertDoesNotExist()
    }

    @Test
    fun `horizontal Form continue returns Complete carrying the Form launch mode`() = launch(
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    ) { scenario ->
        formPage.fillOutCardDetails()
        composeTestRule.waitForIdle()
        primaryButton.assertIsEnabled()
        primaryButton.performScrollTo()
        primaryButton.performClick()

        composeTestRule.waitForIdle()
        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Complete>()
        val complete = result as EmbeddedActivityResult.Complete
        assertThat(complete.hasBeenConfirmed).isFalse()
        assertThat(complete.launchMode).isEqualTo(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"))
    }

    private fun launch(
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        selectedPaymentMethodCode: PaymentMethodCode = "card",
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        ),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                        .paymentMethodLayout(paymentMethodLayout)
                        .build(),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "HorizontalFormTestIdentifier",
                    selection = null,
                    customerState = createCustomerState(paymentMethods = emptyList()),
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = selectedPaymentMethodCode),
                    previousNewSelections = Bundle(),
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
