package com.stripe.android.paymentelement.taptoadd

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.utils.TerminalWrapperTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.tta.testing.TapToAddCardAddedPage
import com.stripe.android.tta.testing.TapToAddCardCollectionTestHelper
import com.stripe.android.tta.testing.TapToAddConfirmationPage
import com.stripe.android.tta.testing.TapToAddLinkTestHelper
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(TapToAddPreview::class)
@RunWith(TestParameterInjector::class)
class TapToAddTest {
    val terminalWrapperTestRule = TerminalWrapperTestRule(enabled = true)

    @get:Rule
    val testRules: TestRules = TestRules.create(
        terminalTestRule = terminalWrapperTestRule,
    ) {
        around(FeatureFlagTestRule(FeatureFlags.enableTapToAdd, true))
    }

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val cardCollectionTestHelper = TapToAddCardCollectionTestHelper(networkRule) {
        terminalWrapperTestRule.delegate
    }

    private val linkHelper = TapToAddLinkTestHelper(composeTestRule, networkRule)
    private val cardAddedPage = TapToAddCardAddedPage(composeTestRule, linkHelper)
    private val confirmationPage = TapToAddConfirmationPage(composeTestRule)
    private val tapToAddCardFormPage = TapToAddCardFormPage(composeTestRule)

    @Test
    fun successWithCompleteMode(
        @TestParameter(valuesProvider = TapToAddIntegrationType.Complete.Provider::class)
        integrationType: TapToAddIntegrationType.Complete
    ) = runTapToAddIntegrationTest(
        integrationType = integrationType,
        composeTestRule = composeTestRule,
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_123_secret_123")
        },
        createCardPresentCallback = {
            CreateIntentResult.Success("seti_123_secret_123")
        },
        resultCallback = {
            assertThat(it).isInstanceOf(TapToAddTestResult.Completed::class.java)
        }
    ) {
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-tta.json")
        }

        cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        cardAddedPage.assertShown()
        cardAddedPage.clickContinue()

        confirmationPage.assertPrimaryButton(isEnabled = true)

        enqueueConfirmRequests()

        confirmationPage.clickPrimaryButton()
        confirmationPage.waitUntilMissing()
    }

    @Ignore
    @Test
    fun successWithContinueMode(
        @TestParameter(valuesProvider = TapToAddIntegrationType.Continue.Provider::class)
        integrationType: TapToAddIntegrationType.Continue
    ) = runTapToAddIntegrationTest(
        integrationType = integrationType,
        composeTestRule = composeTestRule,
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_123_secret_123")
        },
        createCardPresentCallback = {
            CreateIntentResult.Success("seti_123_secret_123")
        },
        resultCallback = {
            assertThat(it).isInstanceOf(TapToAddTestResult.Completed::class.java)
        }
    ) {
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-tta.json")
        }

        cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        cardAddedPage.assertShown()

        enqueueConfirmRequests()

        cardAddedPage.clickContinue()
        cardAddedPage.waitUntilMissing()

        confirm()
    }

    private fun enqueueConfirmRequests() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_123"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_123/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }
}
