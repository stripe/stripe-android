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
    val runner = when (integrationType) {
        TapToAddIntegrationType.Continue.Embedded -> {
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                networkRule = networkRule,
                composeTestRule = composeTestRule,
                createIntentCallback = createIntentCallback,
                createCardPresentSetupIntentCallback = createCardPresentCallback,
                tapToAddTestResultCallback = resultCallback,
                mode = TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Continue,
            )
        }
        TapToAddIntegrationType.Continue.FlowController -> {
            TapToAddIntegrationTestRunner.FlowControllerRunner(
                networkRule = networkRule,
                composeTestRule = composeTestRule,
                createIntentCallback = createIntentCallback,
                createCardPresentSetupIntentCallback = createCardPresentCallback,
                tapToAddTestResultCallback = resultCallback,
            )
        }
        TapToAddIntegrationType.Complete.PaymentSheet -> {
            TapToAddIntegrationTestRunner.PaymentSheetRunner(
                networkRule = networkRule,
                composeTestRule = composeTestRule,
                createIntentCallback = createIntentCallback,
                createCardPresentSetupIntentCallback = createCardPresentCallback,
                tapToAddTestResultCallback = resultCallback,
            )
        }
        TapToAddIntegrationType.Complete.Embedded -> {
            TapToAddIntegrationTestRunner.EmbeddedRunner(
                networkRule = networkRule,
                composeTestRule = composeTestRule,
                createIntentCallback = createIntentCallback,
                createCardPresentSetupIntentCallback = createCardPresentCallback,
                tapToAddTestResultCallback = resultCallback,
                mode = TapToAddIntegrationTestRunner.EmbeddedRunner.Mode.Confirm,
            )
        }
    }

    runner.run(block)
}

@OptIn(TapToAddPreview::class)
private sealed class TapToAddIntegrationTestRunner(
    protected val networkRule: NetworkRule,
    protected val composeTestRule: ComposeTestRule,
    protected val createIntentCallback: CreateIntentCallback,
    private val createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
    protected val tapToAddTestResultCallback: TapToAddResultTestCallback,
) {
    protected val integrationBuilder = TapToAddIntegrationBuilder()
        .createIntentCallback(createIntentCallback)
        .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)

    abstract fun run(block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit)

    class PaymentSheetRunner(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        createIntentCallback: CreateIntentCallback,
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
        tapToAddTestResultCallback: TapToAddResultTestCallback,
    ) : TapToAddIntegrationTestRunner(
        networkRule,
        composeTestRule,
        createIntentCallback,
        createCardPresentSetupIntentCallback,
        tapToAddTestResultCallback
    ) {
        override fun run(block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit) {
            runPaymentSheetTest(
                networkRule = networkRule,
                builder = {
                    integrationBuilder.applyToPaymentSheetBuilder(this)
                },
                resultCallback = { result ->
                    tapToAddTestResultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(TapToAddIntegrationTestRunnerContext.Sheet.PaymentSheet(composeTestRule, context))
            }
        }
    }

    class FlowControllerRunner(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        createIntentCallback: CreateIntentCallback,
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
        tapToAddTestResultCallback: TapToAddResultTestCallback,
    ) : TapToAddIntegrationTestRunner(
        networkRule,
        composeTestRule,
        createIntentCallback,
        createCardPresentSetupIntentCallback,
        tapToAddTestResultCallback
    ) {
        override fun run(block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit) {
            runFlowControllerTest(
                networkRule = networkRule,
                callConfirmOnPaymentOptionCallback = false,
                builder = {
                    integrationBuilder.applyToFlowControllerBuilder(this)
                },
                resultCallback = { result ->
                    tapToAddTestResultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(TapToAddIntegrationTestRunnerContext.Sheet.FlowController(composeTestRule, context))
            }
        }
    }

    class EmbeddedRunner(
        networkRule: NetworkRule,
        composeTestRule: ComposeTestRule,
        createIntentCallback: CreateIntentCallback,
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
        tapToAddTestResultCallback: TapToAddResultTestCallback,
        private val mode: Mode,
    ) : TapToAddIntegrationTestRunner(
        networkRule,
        composeTestRule,
        createIntentCallback,
        createCardPresentSetupIntentCallback,
        tapToAddTestResultCallback
    ) {
        enum class Mode {
            Confirm,
            Continue,
        }

        override fun run(block: suspend TapToAddIntegrationTestRunnerContext.() -> Unit) {
            runEmbeddedPaymentElementTest(
                networkRule = networkRule,
                builder = {
                    integrationBuilder.applyToEmbeddedBuilder(this)
                },
                createIntentCallback = createIntentCallback,
                resultCallback = { result ->
                    tapToAddTestResultCallback.onResult(TapToAddTestResult.from(result))
                },
            ) { context ->
                block(createTapToAddTestContext(context))
            }
        }

        private fun createTapToAddTestContext(
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
