package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultCvcRecollectionInteractorTest {

    private val scope = TestScope()
    private fun createInteractor(): DefaultCvcRecollectionInteractor {
        return DefaultCvcRecollectionInteractor(
            args = Args(
                lastFour = "4242",
                cardBrand = CardBrand.Visa,
                cvc = null,
                isTestMode = true
            ),
            scope = scope
        )
    }

    @Test
    fun `view model state initialized properly on init`() {
        val interactor = createInteractor()

        assertThat(interactor.viewState.value.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(interactor.viewState.value.lastFour).isEqualTo("4242")
        assertThat(interactor.viewState.value.cvc).isEqualTo(null)
        assertThat(interactor.viewState.value.isTestMode).isEqualTo(true)
        assertThat(interactor.viewState.value.element.controller.fieldState.value)
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `on confirm pressed interactor emits confirmed result`() = runTest {
        val interactor = createInteractor()

        interactor.cvcCompletionState.test {
            assertThat(awaitItem()).isEqualTo(CvcState())

            interactor.viewState.value.element.controller.onRawValueChange("55")

            scope.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(CvcState("55", isComplete = false))

            interactor.viewState.value.element.controller.onRawValueChange("555")

            scope.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(CvcState("555", isComplete = false))

            scope.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(CvcState("555", isComplete = true))
        }
    }
}
