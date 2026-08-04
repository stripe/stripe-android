package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.assertCompleted as assertEmbeddedCompleted
import com.stripe.android.paymentelement.runEmbeddedPaymentElementTest
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest

internal fun runNfcScanningIntegrationTest(
    networkRule: NetworkRule,
    composeTestRule: ComposeTestRule,
    integrationType: NfcScanningIntegrationType,
    block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
) {
    integrationType.runner.run(
        networkRule = networkRule,
        composeTestRule = composeTestRule,
        block = block,
    )
}

internal sealed class NfcScanningIntegrationTestRunner {
    fun run(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
    ) {
        runTest(
            networkRule = networkRule,
            composeTestRule = composeTestRule,
            block = block,
        )
    }

    protected abstract fun runTest(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
    )

    object PaymentSheetRunner : NfcScanningIntegrationTestRunner() {
        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
        ) {
            runPaymentSheetTest(
                networkRule = networkRule,
                builder = {
                    createIntentCallback(NfcScanningIntegrationTestRunner.createIntentCallback)
                },
                resultCallback = ::assertCompleted,
            ) { context ->
                block(
                    NfcScanningIntegrationTestRunnerContext.Sheet.PaymentSheet(
                        composeTestRule = composeTestRule,
                        context = context,
                    )
                )
            }
        }
    }

    object FlowControllerRunner : NfcScanningIntegrationTestRunner() {
        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
        ) {
            runFlowControllerTest(
                networkRule = networkRule,
                builder = {
                    createIntentCallback(NfcScanningIntegrationTestRunner.createIntentCallback)
                },
                resultCallback = ::assertCompleted,
            ) { context ->
                block(
                    NfcScanningIntegrationTestRunnerContext.Sheet.FlowController(
                        composeTestRule = composeTestRule,
                        context = context,
                    )
                )
            }
        }
    }

    object EmbeddedRunner : NfcScanningIntegrationTestRunner() {
        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            block: suspend NfcScanningIntegrationTestRunnerContext.() -> Unit,
        ) {
            runEmbeddedPaymentElementTest(
                networkRule = networkRule,
                createIntentCallback = createIntentCallback,
                resultCallback = ::assertEmbeddedCompleted,
            ) { context ->
                block(
                    NfcScanningIntegrationTestRunnerContext.Embedded(
                        composeTestRule = composeTestRule,
                        context = context,
                    )
                )
            }
        }
    }

    companion object {
        val createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_example_secret_example")
        }
    }
}
