package com.stripe.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter
import com.stripe.android.ui.core.analytics.ReportablePaymentsUi

fun createPaymentsUiCoreComposeRule() = PaymentsUiCoreComposeRule(createComposeRule())

class PaymentsUiCoreComposeContext(
    val eventReporter: PaymentsUiEventReporter = ComposePaymentsUiEventReporter
)

class PaymentsUiCoreComposeRule(
    private val testRule: ComposeContentTestRule
) : ComposeContentTestRule by testRule {
    override fun setContent(composable: @Composable () -> Unit) {
        setContent(PaymentsUiCoreComposeContext(), composable)
    }

    fun setContent(
        context: PaymentsUiCoreComposeContext = PaymentsUiCoreComposeContext(),
        composable: @Composable () -> Unit
    ) {
        testRule.setContent {
            ReportablePaymentsUi(context.eventReporter) {
                composable()
            }
        }
    }
}

private object ComposePaymentsUiEventReporter : PaymentsUiEventReporter {
    override fun onCardNumberCompleted() {
        // No-op
    }
    override fun onAutofillEvent(type: String) {
        // No-op
    }
    override fun onFieldInteracted() {
        // No-op
    }
}
