package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.coroutines.CoroutineContext

class DefaultEditPaymentMethodViewInteractorTest {
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Test
    fun `on init, view state should be initialized properly`() = runTest {
        val interactor = createInteractor()

        interactor.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentMethodViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    last4 = "4242",
                    canUpdate = false,
                    selectedBrand = CARTES_BANCAIRES_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on selection changed, 'canUpdate' should be true & selected brand should be changed`() = runTest {
        val interactor = createInteractor()

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        interactor.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentMethodViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    last4 = "4242",
                    canUpdate = true,
                    selectedBrand = VISA_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on selection changed & reverted, 'canUpdate' should be false`() = runTest {
        val interactor = createInteractor()

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(CARTES_BANCAIRES_BRAND_CHOICE)
        )

        interactor.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentMethodViewState(
                    status = EditPaymentMethodViewState.Status.Idle,
                    last4 = "4242",
                    canUpdate = false,
                    selectedBrand = CARTES_BANCAIRES_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on remove pressed, should invoke 'onRemove' to begin removal process`() = runTest {
        val removeOperation: (paymentMethod: PaymentMethod) -> Boolean = mock {
            onGeneric { invoke(any()) } doReturn true
        }

        val onRemove: PaymentMethodRemoveOperation = { paymentMethod ->
            delay(100)

            removeOperation(paymentMethod)
        }

        val interactor = createInteractor(onRemove = onRemove, workContext = testDispatcher)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        interactor.handleViewAction(EditPaymentMethodViewAction.OnRemovePressed)

        testDispatcher.scheduler.advanceTimeBy(50)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Removing)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        verify(removeOperation).invoke(CARD_WITH_NETWORKS_PAYMENT_METHOD)
    }

    @Test
    fun `on update pressed with selection change, should invoke 'onUpdate' to begin update process`() = runTest(
        context = testDispatcher
    ) {
        val updatedPaymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD.copy(
            card = CARD_WITH_NETWORKS_PAYMENT_METHOD.card?.copy(
                networks = CARD_WITH_NETWORKS_PAYMENT_METHOD.card?.networks?.copy(
                    available = setOf("cartes_bancaires", "visa"),
                    preferred = "visa"
                )
            )
        )

        val updateOperation: (paymentMethod: PaymentMethod, brand: CardBrand) -> Result<PaymentMethod> = mock {
            onGeneric { invoke(any(), any()) }.thenAnswer {
                Result.success(updatedPaymentMethod)
            }
        }

        val onUpdate: PaymentMethodUpdateOperation = { paymentMethod, brand ->
            delay(100)

            updateOperation(paymentMethod, brand)
        }

        val interactor = createInteractor(onUpdate = onUpdate, workContext = testDispatcher)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        interactor.handleViewAction(EditPaymentMethodViewAction.OnUpdatePressed)

        testDispatcher.scheduler.advanceTimeBy(50)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Updating)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        verify(updateOperation).invoke(CARD_WITH_NETWORKS_PAYMENT_METHOD, CardBrand.Visa)
    }

    private fun createInteractor(
        onRemove: PaymentMethodRemoveOperation = { true },
        onUpdate: PaymentMethodUpdateOperation = { _, _ -> Result.success(CARD_WITH_NETWORKS_PAYMENT_METHOD) },
        workContext: CoroutineContext = UnconfinedTestDispatcher()
    ): DefaultEditPaymentMethodViewInteractor {
        return DefaultEditPaymentMethodViewInteractor(
            initialPaymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD,
            removeExecutor = onRemove,
            updateExecutor = onUpdate,
            viewStateSharingStarted = SharingStarted.Eagerly,
            workContext = workContext
        )
    }

    private fun DefaultEditPaymentMethodViewInteractor.currentStatus(): EditPaymentMethodViewState.Status {
        return viewState.value.status
    }

    private companion object {
        private val CARD_WITH_NETWORKS_PAYMENT_METHOD = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD

        val CARTES_BANCAIRES_BRAND_CHOICE = EditPaymentMethodViewState.CardBrandChoice(
            brand = CardBrand.CartesBancaires
        )

        private val VISA_BRAND_CHOICE = EditPaymentMethodViewState.CardBrandChoice(
            brand = CardBrand.Visa
        )
    }
}
