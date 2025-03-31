package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal sealed class ProductIntegrationType(
    val expectedDeferredSSCResultCallback: PaymentSheetResultCallback,
) {

    abstract fun assertDefaultPaymentMethodsDeferredSSCErrorShown(
        composeTestRule: ComposeTestRule,
        testContext: ProductIntegrationTestRunnerContext,
    )

    data object PaymentSheet : ProductIntegrationType(
        expectedDeferredSSCResultCallback = {
            // We do not expect PaymentSheet to finish in this case.
        }
    ) {
        override fun assertDefaultPaymentMethodsDeferredSSCErrorShown(
            composeTestRule: ComposeTestRule,
            testContext: ProductIntegrationTestRunnerContext
        ) {
            val paymentSheetPage = PaymentSheetPage(composeTestRule)
            paymentSheetPage.assertErrorMessageShown()
            testContext.markTestSucceeded()
        }
    }

    data object FlowController : ProductIntegrationType(
        expectedDeferredSSCResultCallback = { result ->
            val failureResult = result as? PaymentSheetResult.Failed
            assertThat(failureResult?.error?.message).isEqualTo(
                "(Test-mode only error) The default payment methods feature is not yet supported with deferred server-side confirmation. Please contact us if you'd like to use this feature via a Github issue on stripe-android."
            )
        }
    ) {
        override fun assertDefaultPaymentMethodsDeferredSSCErrorShown(
            composeTestRule: ComposeTestRule,
            testContext: ProductIntegrationTestRunnerContext
        ) {
            // Do nothing. The error is not displayed in FlowController, it's returned as part of the result.
        }
    }
}

internal object ProductIntegrationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ProductIntegrationType> {
        return listOf(
            ProductIntegrationType.PaymentSheet,
            ProductIntegrationType.FlowController,
        )
    }
}
