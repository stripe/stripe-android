package com.stripe.android.link.ui.inline.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter
import com.stripe.android.ui.core.analytics.ReportablePaymentsUi
import org.junit.rules.TestRule

inline fun <reified A : ComponentActivity> createLinkAndroidComposeRule() =
    LinkAndroidComposeRule(createAndroidComposeRule<A>())

class LinkAndroidComposeContext(
    val eventReporter: PaymentsUiEventReporter = ComposePaymentsUiEventReporter
)

class LinkAndroidComposeRule<R : TestRule, A : ComponentActivity>(
    private val testRule: AndroidComposeTestRule<R, A>
) : ComposeContentTestRule by testRule {
    val activity: A
        get() = testRule.activity

    val activityRule: R
        get() = testRule.activityRule

    override fun setContent(composable: @Composable () -> Unit) {
        setContent(LinkAndroidComposeContext(), composable)
    }

    fun setContent(
        context: LinkAndroidComposeContext = LinkAndroidComposeContext(),
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
