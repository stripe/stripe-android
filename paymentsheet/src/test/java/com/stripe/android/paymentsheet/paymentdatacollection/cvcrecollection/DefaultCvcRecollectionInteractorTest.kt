package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.TextFieldStateConstants
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
    fun `view model state initialized properly on init`() {
        val interactor = createInteractor()

        assertThat(interactor.viewState.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(interactor.viewState.lastFour).isEqualTo("4242")
        assertThat(interactor.viewState.cvc).isEqualTo(null)
        assertThat(interactor.viewState.isTestMode).isEqualTo(true)
        assertThat(interactor.viewState.controller.fieldState.value)
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `on confirm pressed interactor emits confirmed result`() = runTest {
        val interactor = createInteractor()

        interactor.cvcCompletionState.test {
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Incomplete)

            interactor.viewState.controller.onRawValueChange("555")
//            delay(100)
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Completed("555"))

            interactor.viewState.controller.onRawValueChange("55")
//            delay(100)
            assertThat(awaitItem()).isEqualTo(CvcCompletionState.Incomplete)
        }
    }
}
