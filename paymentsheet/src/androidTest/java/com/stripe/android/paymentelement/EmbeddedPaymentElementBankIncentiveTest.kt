package com.stripe.android.paymentelement

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.UsBankAccountFormTestUtils
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.paymentelementtestpages.FormPage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementBankIncentiveTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(FeatureFlagTestRule(FeatureFlags.instantDebitsIncentives, isEnabled = true))
            .around(IntentsRule())
    }

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val embeddedFormPage = EmbeddedFormPage(testRules.compose)
    private val formPage = FormPage(testRules.compose)

    @Test
    fun testIneligibleLinkedBankAccountRemovesHeaderIncentive() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ -> CreateIntentResult.Success("pi_example_secret_12345") },
        resultCallback = {},
    ) { testContext ->
        UsBankAccountFormTestUtils.setupSuccessfulCompletionOfInstantDebitsForm(
            eligibleForIncentive = false,
        )
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json") { json ->
                json.getJSONObject("link_settings")
                    .put("link_mode", "LINK_PAYMENT_METHOD")
                    .put(
                        "link_consumer_incentive",
                        JSONObject()
                            .put(
                                "incentive_params",
                                JSONObject().put("payment_method", "link_instant_debits")
                            )
                            .put("incentive_display_text", "$5")
                    )
                    .put(
                        "link_supported_payment_methods_onboarding_enabled",
                        JSONArray().put("INSTANT_DEBITS")
                    )
            }
        }

        testContext.configure()

        embeddedContentPage.clickOnLpm("link_instant_debits")
        embeddedFormPage.assertHeaderPromoBadgeIsDisplayed("$5")

        formPage.fillOutEmail()
        embeddedFormPage.clickPrimaryButtonWithoutWaitingForDismissal()

        embeddedFormPage.waitUntilHeaderPromoBadgeIsMissing()
        testContext.markTestSucceeded()
    }
}
