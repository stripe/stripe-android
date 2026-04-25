package com.stripe.android.paymentelement.taptoadd

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
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
import com.stripe.android.tta.testing.TapToAddSavedPaymentMethodPage
import com.stripe.android.tta.testing.TerminalTestDelegate
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.VerticalModePage
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(TapToAddPreview::class)
@RunWith(TestParameterInjector::class)
internal class TapToAddTest {
    // The /v1/consumers/sessions/log_out request is launched async from a GlobalScope. We want to make sure
    // it happens, but it's okay if it takes a bit to happen.
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    val terminalWrapperTestRule = TerminalWrapperTestRule(enabled = true)

    @get:Rule
    val testRules: TestRules = TestRules.create(
        networkRule = networkRule,
        terminalTestRule = terminalWrapperTestRule,
    ) {
        around(FeatureFlagTestRule(FeatureFlags.enableTapToAdd, true))
    }

    private val composeTestRule = testRules.compose

    private val cardCollectionTestHelper = TapToAddCardCollectionTestHelper(networkRule) {
        terminalWrapperTestRule.delegate
    }

    private val paymentElementFormPage = FormPage(composeTestRule)
    private val verticalModePage = VerticalModePage(composeTestRule)
    private val linkHelper = TapToAddLinkTestHelper(composeTestRule, networkRule)
    private val cardAddedPage = TapToAddCardAddedPage(composeTestRule, linkHelper)
    private val confirmationPage = TapToAddConfirmationPage(composeTestRule)
    private val savedPaymentMethodPage = TapToAddSavedPaymentMethodPage(composeTestRule, linkHelper)
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
            response.defaultElementsSessions()
        }

        cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        confirmationPage.assertPrimaryButton(isEnabled = true)

        enqueueConfirmRequests()

        confirmationPage.clickPrimaryButton()
        confirmationPage.waitUntilMissing()
    }

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
            response.defaultElementsSessions()
        }

        cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        enqueueConfirmRequests()

        cardAddedPage.assertShown()
        cardAddedPage.advancePastScreen()
        cardAddedPage.waitUntilMissing()

        confirm()
    }

    @Test
    fun canceledDuringCardCollection(
        @TestParameter(valuesProvider = TapToAddIntegrationType.Provider::class)
        integrationType: TapToAddIntegrationType
    ) = runTapToAddIntegrationTest(
        integrationType = integrationType,
        composeTestRule = composeTestRule,
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            throw IllegalStateException("Create intent callback should not be called!")
        },
        createCardPresentCallback = {
            CreateIntentResult.Success("seti_123_secret_123")
        },
        resultCallback = {
            throw IllegalStateException("Result callback should not be called!")
        }
    ) {
        networkRule.elementsSession { response ->
            response.defaultElementsSessions()
        }

        terminalWrapperTestRule.delegate.setScenario(
            TerminalTestDelegate.Scenario(
                collectSetupIntentPaymentMethodResult = TerminalTestDelegate.SetupIntentResult.Failure(
                    exception = TerminalException(
                        errorCode = TerminalErrorCode.CANCELED,
                        errorMessage = "Canceled!"
                    )
                )
            )
        )

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        paymentElementFormPage.waitUntilVisible()

        markTestSucceeded()
    }

    @Test
    fun successAfterCancelAfterCardCollectedWithCompleteMode(
        @TestParameter(valuesProvider = TapToAddIntegrationType.Complete.Provider::class)
        integrationType: TapToAddIntegrationType.Complete,
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
            response.defaultElementsSessions()
        }

        cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        confirmationPage.assertPrimaryButton()
        confirmationPage.clickCloseButton()
        confirmationPage.waitUntilMissing()

        enqueueConfirmRequests()

        verticalModePage.assertHasSelectedSavedPaymentMethod(paymentMethodId = "pm_2")

        when (integrationType) {
            TapToAddIntegrationType.Complete.PaymentSheet -> clickPrimaryButton()
            TapToAddIntegrationType.Complete.Embedded -> confirm()
        }

        verticalModePage.waitUntilMissing()
    }

    @Test
    fun successAfterCancelAfterCardCollectedWithLink(
        @TestParameter(valuesProvider = TapToAddIntegrationType.Provider::class)
        integrationType: TapToAddIntegrationType
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
            response.elementsSessionTtaWithLink()
        }

        enqueueRetrieveCustomerRequest()

        val info = cardCollectionTestHelper.enqueueSuccessfulTapToCollectFlow()

        linkHelper.enqueueLookup()

        launchFlow()
        openCardForm()

        tapToAddCardFormPage.clickOnTapToAdd()

        cardAddedPage.assertShown(withLink = true)
        cardAddedPage.clickCloseButton()
        cardAddedPage.waitUntilMissing()

        savedPaymentMethodPage.assertShown()
        savedPaymentMethodPage.fillLink()

        linkHelper.enqueueSignup(withName = false)
        linkHelper.enqueueCreatePaymentDetailsFromPaymentMethod(info.cardPaymentMethod.id)
        enqueueConfirmRequests()
        enqueueLinkLogout(integrationType)

        clickPrimaryButton()
        savedPaymentMethodPage.waitUntilMissing()

        if (integrationType !is TapToAddIntegrationType.Complete) {
            confirm()
        }
    }

    private fun enqueueRetrieveCustomerRequest() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/customers/cus_123"),
        ) { response ->
            response.testBodyFromFile("tta-customer-get-success.json")
        }
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

    private fun enqueueLinkLogout(integrationType: TapToAddIntegrationType) {
        if (
            integrationType == TapToAddIntegrationType.Complete.PaymentSheet ||
            integrationType == TapToAddIntegrationType.Continue.FlowController
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/log_out"),
            ) { response ->
                response.testBodyFromFile("consumer-session-logout-success.json")
            }
        }
    }

    private fun MockResponse.defaultElementsSessions() {
        testBodyFromFile(
            "elements-sessions-tta.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "PAYMENT_METHOD_TYPES",
                    new = "\"card\", \"cashapp\""
                ),
                ResponseReplacement(
                    original = "LINK_ENABLED_STATE",
                    new = "false"
                ),
            )
        )
    }

    private fun MockResponse.elementsSessionTtaWithLink() {
        testBodyFromFile(
            "elements-sessions-tta.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "PAYMENT_METHOD_TYPES",
                    new = "\"card\", \"cashapp\", \"link\""
                ),
                ResponseReplacement(
                    original = "LINK_ENABLED_STATE",
                    new = "true"
                ),
            )
        )
    }
}
