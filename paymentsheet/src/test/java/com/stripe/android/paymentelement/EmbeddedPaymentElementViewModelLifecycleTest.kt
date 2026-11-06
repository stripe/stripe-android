package com.stripe.android.paymentelement

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedPaymentElementViewModelLifecycleTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun viewModelIsRemovedFromHostViewModelStoreWhenUnmounted() {
        var isMounted by mutableStateOf(true)

        composeTestRule.setContent {
            if (isMounted) {
                rememberEmbeddedPaymentElement(
                    EmbeddedPaymentElement.Builder(
                        createIntentCallback = { _, _ -> error("Not used in this test.") },
                        resultCallback = {},
                    )
                )
            }
        }

        repeat(20) {
            isMounted = false
            composeTestRule.waitForIdle()

            isMounted = true
            composeTestRule.waitForIdle()
        }

        isMounted = false
        composeTestRule.waitForIdle()

        val leakedEmbeddedViewModelKeys = composeTestRule.activity.viewModelStore.keys().filter {
            it.contains("EmbeddedPaymentElementViewModel")
        }

        assertThat(leakedEmbeddedViewModelKeys).isEmpty()
    }
}
