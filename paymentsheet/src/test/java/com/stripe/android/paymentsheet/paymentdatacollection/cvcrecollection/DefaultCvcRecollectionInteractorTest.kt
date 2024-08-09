package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultCvcRecollectionInteractorTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createInteractor(): DefaultCvcRecollectionInteractor {
        return DefaultCvcRecollectionInteractor(
            args = Args(
                lastFour = "4242",
                cardBrand = CardBrand.Visa,
                cvc = null,
                isTestMode = true
            ),
        )
    }

    @Test
    fun `view model state initialized properly on init`() {
        val interactor = createInteractor()

        assertThat(interactor.viewState.value).isEqualTo(
            CvcRecollectionViewState(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                cvc = null,
                isTestMode = true
            )
        )
    }

    @Test
    fun `on confirm pressed viewModel emits confirmed result`() = runTest {
        val interactor = createInteractor()

        interactor.cvcCompletionState.test {
            assertThat(awaitItem()).isEqualTo(CvcState())

            interactor.handleViewAction(
                action = CvcRecollectionViewAction.CvcStateChanged(CvcState("555", isComplete = true))
            )

            assertThat(awaitItem()).isEqualTo(CvcState("555", isComplete = true))
        }
    }
}
