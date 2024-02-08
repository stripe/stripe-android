package com.stripe.android.uicore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter

fun createUiCoreComposeTestRule(): UiCoreComposeTestRule =
    UiCoreComposeTestRule(createComposeRule())

class UiCoreComposeTestContext(
    val eventReporter: UiEventReporter = EmptyUiEventReporter
)

class UiCoreComposeTestRule(
    private val testRule: ComposeContentTestRule
) : ComposeContentTestRule by testRule {
    override fun setContent(composable: @Composable () -> Unit) {
        setContent(UiCoreComposeTestContext(), composable)
    }

    fun setContent(
        context: UiCoreComposeTestContext = UiCoreComposeTestContext(),
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

private object EmptyUiEventReporter : UiEventReporter {
    override fun onAutofillEvent(type: String) {
        // No-op
    }
    override fun onFieldInteracted() {
        // No-op
    }
}
