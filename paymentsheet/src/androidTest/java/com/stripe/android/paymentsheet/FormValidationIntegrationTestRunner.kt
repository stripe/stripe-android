package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElementTestRunnerContext
import com.stripe.android.paymentelement.runEmbeddedPaymentElementTest
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest

internal enum class FormValidationIntegrationType(
    val productIntegrationType: ProductIntegrationType?,
) {
    PaymentSheet(ProductIntegrationType.PaymentSheet),
    FlowController(ProductIntegrationType.FlowController),
    Embedded(null);

    internal object Provider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<FormValidationIntegrationType> {
            return entries
        }
    }
}

internal fun runFormValidationIntegrationTest(
    networkRule: NetworkRule,
    composeTestRule: ComposeTestRule,
    integrationType: FormValidationIntegrationType,
    block: suspend FormValidationIntegrationTestRunnerContext.() -> Unit,
) {
    val productIntegrationType = integrationType.productIntegrationType
    if (productIntegrationType != null) {
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = productIntegrationType,
            resultCallback = ::expectNoResult,
        ) { context ->
            FormValidationIntegrationTestRunnerContext.Sheet(
                composeTestRule = composeTestRule,
                context = context,
            ).block()
        }
    } else {
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, _ ->
                error("CreateIntentCallback should not be called for an invalid form.")
            },
            resultCallback = {
                error("ResultCallback should not be called for an invalid form.")
            },
        ) { context ->
            FormValidationIntegrationTestRunnerContext.Embedded(
                composeTestRule = composeTestRule,
                context = context,
            ).block()
        }
    }
}

internal sealed class FormValidationIntegrationTestRunnerContext(
    protected val composeTestRule: ComposeTestRule,
) {
    abstract suspend fun launch()

    abstract fun navigateToFormFor(paymentMethodCode: String)

    abstract fun clickDisabledPrimaryButton()

    abstract fun markTestSucceeded()

    class Sheet(
        composeTestRule: ComposeTestRule,
        private val context: ProductIntegrationTestRunnerContext,
    ) : FormValidationIntegrationTestRunnerContext(composeTestRule) {
        private val page = PaymentSheetPage(composeTestRule)

        override suspend fun launch() {
            context.launch(
                PaymentSheet.Configuration.Builder("Example, Inc.")
                    .billingDetailsCollectionConfiguration(requiredBillingDetailsCollectionConfiguration())
                    .allowsDelayedPaymentMethods(true)
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
            )
        }

        override fun navigateToFormFor(paymentMethodCode: String) {
            page.clickOnLpm(paymentMethodCode, forVerticalMode = true)
        }

        override fun clickDisabledPrimaryButton() {
            composeTestRule.waitUntil(5_000) {
                composeTestRule
                    .onAllNodes(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG).and(isNotEnabled()))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.waitUntil(5_000) {
                composeTestRule
                    .onAllNodes(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG))
                .performScrollTo()
                .performClick()

            composeTestRule.waitForIdle()
        }

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }
    }

    class Embedded(
        composeTestRule: ComposeTestRule,
        private val context: EmbeddedPaymentElementTestRunnerContext,
    ) : FormValidationIntegrationTestRunnerContext(composeTestRule) {
        private val contentPage = EmbeddedContentPage(composeTestRule)
        private val formPage = EmbeddedFormPage(composeTestRule)

        override suspend fun launch() {
            context.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd",
                    )
                ),
                configurationMutator = {
                    billingDetailsCollectionConfiguration(requiredBillingDetailsCollectionConfiguration())
                    allowsDelayedPaymentMethods(true)
                    formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
                }
            )
        }

        override fun navigateToFormFor(paymentMethodCode: String) {
            contentPage.clickOnLpm(paymentMethodCode)
        }

        override fun clickDisabledPrimaryButton() {
            formPage.clickDisabledPrimaryButton()
        }

        override fun markTestSucceeded() {
            context.markTestSucceeded()
        }
    }
}

private fun requiredBillingDetailsCollectionConfiguration() =
    PaymentSheet.BillingDetailsCollectionConfiguration(
        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
    )
