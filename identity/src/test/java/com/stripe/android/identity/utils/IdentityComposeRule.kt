package com.stripe.android.identity.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter

fun createIdentityRule() = IdentityComposeRule(createComposeRule())

class IdentityComposeContext(
    val eventReporter: UiEventReporter = IdentityPaymentsUiEventReporter
)

class IdentityComposeRule(
    private val testRule: ComposeContentTestRule
) : ComposeContentTestRule by testRule {
    override fun setContent(composable: @Composable () -> Unit) {
        setContent(IdentityComposeContext(), composable)
    }

    fun setContent(
        context: IdentityComposeContext = IdentityComposeContext(),
        composable: @Composable () -> Unit
    ) {
        testRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides context.eventReporter
            ) {
                composable()
            }
        }
    }
}

private object IdentityPaymentsUiEventReporter : UiEventReporter {
    override fun onAutofillEvent(type: String) {
        // No-op
    }
    override fun onFieldInteracted() {
        // No-op
    }
}
