package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultCvcRecollectionInteractorTest {

    private fun createInteractor(
        processing: StateFlow<Boolean> = stateFlowOf(false)
    ): DefaultCvcRecollectionInteractor {
        return DefaultCvcRecollectionInteractor(
            lastFour = "4242",
            cardBrand = CardBrand.Visa,
            cvc = "",
            isTestMode = true,
            processing = processing,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
    }

    @Test
    fun `view state initialized properly on init`() = runTest {
        val interactor = createInteractor()

        interactor.viewState.test {
            val viewState = awaitItem()
            assertThat(viewState.lastFour).isEqualTo("4242")
            assertThat(viewState.cvcState).isEqualTo(CvcState(cvc = "", cardBrand = CardBrand.Visa))
            assertThat(viewState.isTestMode).isEqualTo(true)
        }
    }

    @Test
    fun `view state updated with CVC onCvcChanged`() = runTest {
        val interactor = createInteractor()

        interactor.viewState.test {
            assertThat(awaitItem().cvcState.cvc).isEqualTo("")
            interactor.onCvcChanged("444")
            assertThat(awaitItem().cvcState.cvc).isEqualTo("444")
        }
    }

    @Test
    fun `view state updated with enabled when processing changes`() = runTest {
        val processingSource = MutableStateFlow(false)
        val interactor = createInteractor(processing = processingSource)

        interactor.viewState.test {
            assertThat(awaitItem().isEnabled).isTrue()
            processingSource.value = true
            assertThat(awaitItem().isEnabled).isFalse()
            processingSource.value = false
            assertThat(awaitItem().isEnabled).isTrue()
        }
    }

    @Test
    fun `on confirm pressed interactor emits confirmed result`() = runTest {
        val interactor = createInteractor()

        interactor.cvcCompletionState.test {
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Incomplete)

            interactor.onCvcChanged("555")
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Completed("555"))

            interactor.onCvcChanged("55")
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Incomplete)
        }
    }
}
