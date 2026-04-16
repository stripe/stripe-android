package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElementTestRunnerContext
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.runEmbeddedPaymentElementTest
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest

@OptIn(TapToAddPreview::class)
internal fun runTapToAddIntegrationTest(
    networkRule: NetworkRule,
    composeTestRule: ComposeTestRule,
    integrationType: TapToAddIntegrationType,
    createIntentCallback: CreateIntentCallback,
    createCardPresentCallback: CreateCardPresentSetupIntentCallback,
    resultCallback: TapToAddResultTestCallback,
    block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit,
) {
    integrationType.runner.run(
        networkRule = networkRule,
        composeTestRule = composeTestRule,
        createIntentCallback = createIntentCallback,
        createCardPresentSetupIntentCallback = createCardPresentCallback,
        resultCallback = resultCallback,
        block = block
    )
}

@OptIn(TapToAddPreview::class)
internal sealed class TapToAddIntegrationTestRunner {
    fun run(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        createIntentCallback: CreateIntentCallback,
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
        resultCallback: TapToAddResultTestCallback,
        block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit
    ) {
        val integrationBuilder = TapToAddIntegrationBuilder()
            .createIntentCallback(createIntentCallback)
            .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)

        runTest(
            networkRule = networkRule,
            composeTestRule = composeTestRule,
            integrationBuilder = integrationBuilder,
            resultCallback = resultCallback,
            block = block,
        )
    }

    protected abstract fun runTest(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        integrationBuilder: TapToAddIntegrationBuilder,
        resultCallback: TapToAddResultTestCallback,
        block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit
    )

    object PaymentSheetRunner : TapToAddIntegrationTestRunner() {
        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            integrationBuilder: TapToAddIntegrationBuilder,
            resultCallback: TapToAddResultTestCallback,
            block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit
        ) {
            runPaymentSheetTest(
                networkRule = networkRule,
                builder = {
                    integrationBuilder.applyToPaymentSheetBuilder(this)
                },
                resultCallback = { result ->
                    resultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(TapToAddIntegrationTestRunnerContext.Sheet.PaymentSheet(composeTestRule, context))
            }
        }
    }

    object FlowControllerRunner : TapToAddIntegrationTestRunner() {
        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            integrationBuilder: TapToAddIntegrationBuilder,
            resultCallback: TapToAddResultTestCallback,
            block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit
        ) {
            runFlowControllerTest(
                networkRule = networkRule,
                callConfirmOnPaymentOptionCallback = false,
                builder = {
                    integrationBuilder.applyToFlowControllerBuilder(this)
                },
                resultCallback = { result ->
                    resultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(TapToAddIntegrationTestRunnerContext.Sheet.FlowController(composeTestRule, context))
            }
        }
    }

    class EmbeddedRunner(
        private val mode: Mode,
    ) : TapToAddIntegrationTestRunner() {
        enum class Mode {
            Confirm,
            Continue,
        }

        override fun runTest(
            networkRule: NetworkRule,
            composeTestRule: ComposeTestRule,
            integrationBuilder: TapToAddIntegrationBuilder,
            resultCallback: TapToAddResultTestCallback,
            block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit
        ) {
            runEmbeddedPaymentElementTest(
                networkRule = networkRule,
                builder = {
                    integrationBuilder.applyToEmbeddedBuilder(this)
                },
                createIntentCallback = integrationBuilder.createIntentCallback,
                resultCallback = { result ->
                    resultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(createTapToAddTestContext(composeTestRule, context))
            }
        }

        private fun createTapToAddTestContext(
            composeTestRule: ComposeTestRule,
            embeddedContext: EmbeddedPaymentElementTestRunnerContext,
        ): TapToAddIntegrationTestRunnerContext.Embedded {
            return when (mode) {
                Mode.Confirm -> TapToAddIntegrationTestRunnerContext.Embedded.Confirm(
                    composeTestRule = composeTestRule,
                    context = embeddedContext
                )
                Mode.Continue -> TapToAddIntegrationTestRunnerContext.Embedded.Continue(
                    composeTestRule = composeTestRule,
                    context = embeddedContext
                )
            }
        }
    }
}
