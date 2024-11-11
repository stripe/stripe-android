package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultUpdatePaymentMethodInteractorTest {

    @Test
    fun deleteViewAction_deletesPmAndNavigatesBack() {
        val paymentMethod = PaymentMethodFixtures.displayableCard()

        var deletedPm: PaymentMethod? = null
        fun onDeletePaymentMethod(paymentMethod: PaymentMethod) {
            deletedPm = paymentMethod
        }

        var navigatedBack = false
        fun navigateBack() {
            navigatedBack = true
        }

        runScenario(
            canRemove = true,
            displayableSavedPaymentMethod = paymentMethod,
            onDeletePaymentMethod = ::onDeletePaymentMethod,
            navigateBack = ::navigateBack,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.DeletePaymentMethod)

            assertThat(deletedPm).isEqualTo(paymentMethod.paymentMethod)
            assertThat(navigatedBack).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        canRemove: Boolean = false,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        onDeletePaymentMethod: (PaymentMethod) -> Unit = { notImplemented() },
        navigateBack: () -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = false,
            canRemove = canRemove,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            card = displayableSavedPaymentMethod.paymentMethod.card!!,
            onDeletePaymentMethod = onDeletePaymentMethod,
            navigateBack = navigateBack,
        )

        TestParams(interactor).apply { runTest { testBlock() } }
    }

    private data class TestParams(
        val interactor: UpdatePaymentMethodInteractor,
    )
}
