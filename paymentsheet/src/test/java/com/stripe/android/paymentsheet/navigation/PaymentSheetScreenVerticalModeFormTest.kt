package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenVerticalModeFormTest {
    @Test
    fun `title returns null`() = runTest {
        val interactor = FakeVerticalModeFormInteractor()
        PaymentSheetScreen.VerticalModeForm(interactor).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `topBarState reflects live mode`() = runTest {
        val trueInteractor = FakeVerticalModeFormInteractor(isLiveMode = true)
        PaymentSheetScreen.VerticalModeForm(trueInteractor).topBarState().test {
            awaitItem()!!.apply {
                assertThat(showTestModeLabel).isFalse()
            }
        }

        val falseInteractor = FakeVerticalModeFormInteractor(isLiveMode = false)
        PaymentSheetScreen.VerticalModeForm(falseInteractor).topBarState().test {
            awaitItem()!!.apply {
                assertThat(showTestModeLabel).isTrue()
            }
        }
    }

    @Test
    fun `screen close delegates to interactor close`() = runTest {
        var hasCalledOnClose = false
        val interactor = FakeVerticalModeFormInteractor(onClose = { hasCalledOnClose = true })
        PaymentSheetScreen.VerticalModeForm(interactor).close()
        assertThat(hasCalledOnClose).isTrue()
    }

    private class FakeVerticalModeFormInteractor(
        override val state: StateFlow<VerticalModeFormInteractor.State> = stateFlowOf(mock()),
        override val isLiveMode: Boolean = false,
        private val onClose: () -> Unit = {},
    ) : VerticalModeFormInteractor {
        override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
            throw AssertionError("Not implemented")
        }

        override fun close() {
            onClose()
        }
    }
}
