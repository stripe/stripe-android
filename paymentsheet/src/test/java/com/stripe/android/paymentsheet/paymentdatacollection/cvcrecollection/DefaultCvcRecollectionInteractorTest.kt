package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultCvcRecollectionInteractorTest {

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
