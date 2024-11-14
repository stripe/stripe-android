package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultUpdatePaymentMethodInteractorTest {

    @Test
    fun removeViewAction_removesPmAndNavigatesBack() {
        val paymentMethod = PaymentMethodFixtures.displayableCard()

        var removedPm: PaymentMethod? = null
        fun onRemovePaymentMethod(paymentMethod: PaymentMethod) {
            removedPm = paymentMethod
        }

        var navigatedBack = false
        fun navigateBack() {
            navigatedBack = true
        }

        runScenario(
            canRemove = true,
            displayableSavedPaymentMethod = paymentMethod,
            updateablePaymentMethod = UpdateablePaymentMethod.Card(paymentMethod.paymentMethod.card!!),
            onRemovePaymentMethod = ::onRemovePaymentMethod,
            navigateBack = ::navigateBack,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod)

            assertThat(removedPm).isEqualTo(paymentMethod.paymentMethod)
            assertThat(navigatedBack).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        canRemove: Boolean = false,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        updateablePaymentMethod: UpdateablePaymentMethod,
        onRemovePaymentMethod: (PaymentMethod) -> Unit = { notImplemented() },
        navigateBack: () -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = false,
            canRemove = canRemove,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            paymentMethod = updateablePaymentMethod,
            onRemovePaymentMethod = onRemovePaymentMethod,
            navigateBack = navigateBack,
        )

        TestParams(interactor).apply { runTest { testBlock() } }
    }

    private data class TestParams(
        val interactor: UpdatePaymentMethodInteractor,
    )
}
