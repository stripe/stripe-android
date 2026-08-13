package com.stripe.android.paymentelement.nfcscan

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.common.nfcscan.NfcScanningContract
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class NfcScanningTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(
        networkRule = networkRule,
    ) {
        around(NfcHardwareDelegateTestRule())
            .around(FeatureFlagTestRule(FeatureFlags.enableNfcScanning, isEnabled = true))
            .around(FeatureFlagTestRule(FeatureFlags.disableNfcScanningSecurity, isEnabled = true))
            .around(IntentsRule())
    }

    private val composeTestRule = testRules.compose
    private val nfcScanningCardFormPage = NfcScanningCardFormPage(composeTestRule)

    @Test
    fun success(
        @TestParameter(valuesProvider = NfcScanningIntegrationType.Provider::class)
        integrationType: NfcScanningIntegrationType,
    ) = runNfcScanningIntegrationTest(
        integrationType = integrationType,
        composeTestRule = composeTestRule,
        networkRule = networkRule,
    ) {
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-nfc.json")
        }

        NfcScanningIntentsHelper.intendingNfcScanningToComplete(
            result = NfcScanningContract.Result.Complete(
                cardNumber = SCANNED_CARD_NUMBER,
                expirationMonth = SCANNED_EXPIRATION_MONTH,
                expirationYear = SCANNED_EXPIRATION_YEAR,
            )
        )

        launchFlow()
        openCardForm()

        nfcScanningCardFormPage.clickOnNfcScan()

        NfcScanningIntentsHelper.intendedNfcScanningToBeLaunched(composeTestRule)

        nfcScanningCardFormPage.assertScannedCardShown(
            lastFourDigits = SCANNED_CARD_LAST_FOUR,
        )

        nfcScanningCardFormPage.fillRemainingCardDetails()

        enqueueConfirmRequests()

        clickPrimaryButton()

        completeCheckout(cardLastFour = SCANNED_CARD_LAST_FOUR)
    }

    private fun enqueueConfirmRequests() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private companion object {
        const val SCANNED_CARD_NUMBER = "4111111111111111"
        const val SCANNED_CARD_LAST_FOUR = "1111"
        const val SCANNED_EXPIRATION_MONTH = 9
        const val SCANNED_EXPIRATION_YEAR = 2030
    }
}
