package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletButtonsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on render and un-render, should call view action as expected`() = FakeWalletButtonsInteractor.test {
        val content = WalletButtonsContent(
            interactor = interactor,
        )

        val shouldRender = mutableStateOf(false)

        composeTestRule.setContent {
            val shouldRenderContent by shouldRender

            if (shouldRenderContent) {
                content.Content()
            }
        }

        shouldRender.value = true
        composeTestRule.waitForIdle()

        assertThat(viewActionCalls.awaitItem()).isEqualTo(
            WalletButtonsInteractor.ViewAction.OnShown
        )

        shouldRender.value = false
        composeTestRule.waitForIdle()

        assertThat(viewActionCalls.awaitItem()).isEqualTo(
            WalletButtonsInteractor.ViewAction.OnHidden
        )
    }

    private class FakeWalletButtonsInteractor private constructor(
        override val state: StateFlow<WalletButtonsInteractor.State>,
    ) : WalletButtonsInteractor {
        private val viewActionCalls = Turbine<WalletButtonsInteractor.ViewAction>()

        override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
            viewActionCalls.add(action)
        }

        class Scenario(
            val interactor: WalletButtonsInteractor,
            val viewActionCalls: ReceiveTurbine<WalletButtonsInteractor.ViewAction>
        )

        companion object {
            fun test(
                state: StateFlow<WalletButtonsInteractor.State> = stateFlowOf(
                    WalletButtonsInteractor.State(
                        walletButtons = emptyList(),
                        buttonsEnabled = false
                    )
                ),
                test: suspend Scenario.() -> Unit
            ) = runTest {
                val interactor = FakeWalletButtonsInteractor(state)

                test(
                    Scenario(
                        interactor = interactor,
                        viewActionCalls = interactor.viewActionCalls
                    )
                )

                interactor.viewActionCalls.ensureAllEventsConsumed()
            }
        }
    }
}
