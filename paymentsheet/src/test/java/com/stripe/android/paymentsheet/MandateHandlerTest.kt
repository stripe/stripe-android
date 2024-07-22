package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MandateHandlerTest {
    @Test
    fun `updateMandateText ignores showAbove if in vertical mode`() = runScenario(
        isVerticalMode = true,
    ) {
        val mandateText = "Example".resolvableString
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            handler.updateMandateText(mandateText, false)
            awaitItem()!!.apply {
                assertThat(text).isEqualTo(mandateText)
                assertThat(showAbovePrimaryButton).isTrue()
            }

            // No new values emitted since it didn't change.
            handler.updateMandateText(mandateText, true)
        }
    }

    @Test
    fun `updateMandateText uses showAbove when not in vertical mode`() = runScenario(
        isVerticalMode = false,
    ) {
        val mandateText = "Example".resolvableString
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            handler.updateMandateText(mandateText, false)
            awaitItem()!!.apply {
                assertThat(text).isEqualTo(mandateText)
                assertThat(showAbovePrimaryButton).isFalse()
            }

            handler.updateMandateText(mandateText, true)
            awaitItem()!!.apply {
                assertThat(text).isEqualTo(mandateText)
                assertThat(showAbovePrimaryButton).isTrue()
            }
        }
    }

    @Test
    fun `updateMandateText with null mandateText emits null`() = runScenario(
        isVerticalMode = false,
    ) {
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            handler.updateMandateText("Example".resolvableString, false)
            assertThat(awaitItem()).isNotNull()

            handler.updateMandateText(null, true)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `an emission from selection updates mandate`() = runScenario(
        isVerticalMode = false,
    ) {
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            selection.value = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
            awaitItem()!!.apply {
                assertThat(text).isNotNull()
                assertThat(showAbovePrimaryButton).isFalse()
            }

            selection.value = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
            awaitItem()!!.apply {
                assertThat(text).isNotNull()
                assertThat(showAbovePrimaryButton).isTrue()
            }
        }
    }

    @Test
    fun `mandate uses isSetupFlowProvider=true when updating from selection`() = runScenario(
        isVerticalMode = false,
        isSetupFlowProvider = { true },
    ) {
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            selection.value = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
            assertThat(awaitItem()?.text?.resolve(ApplicationProvider.getApplicationContext()))
                .isEqualTo(
                    "By saving your bank account for Example, Inc. you agree to authorize payments pursuant to" +
                        " <a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
                )
        }
    }

    @Test
    fun `mandate uses isSetupFlowProvider=false when updating from selection`() = runScenario(
        isVerticalMode = false,
        isSetupFlowProvider = { false },
    ) {
        handler.mandateText.test {
            assertThat(awaitItem()).isNull()

            selection.value = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
            assertThat(awaitItem()?.text?.resolve(ApplicationProvider.getApplicationContext()))
                .isEqualTo(
                    "By continuing, you agree to authorize payments pursuant to" +
                        " <a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
                )
        }
    }

    private fun runScenario(
        isVerticalMode: Boolean,
        isSetupFlowProvider: () -> Boolean = { false },
        block: suspend Scenario.() -> Unit,
    ) {
        runTest {
            val selection = MutableStateFlow<PaymentSelection?>(null)
            Scenario(
                handler = MandateHandler(
                    coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                    selection = selection,
                    merchantDisplayName = "Example, Inc.",
                    isVerticalMode = isVerticalMode,
                    isSetupFlowProvider = isSetupFlowProvider,
                ),
                selection = selection,
            ).apply {
                block()
            }
        }
    }

    private class Scenario(
        val handler: MandateHandler,
        val selection: MutableStateFlow<PaymentSelection?>,
    )
}
