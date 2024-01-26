package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.strings.resolvableString
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
                    ),
                    displayName = "Card",
                )
            )
        }
    }

    @Test
    fun `on selection options shown, should report event specific to showing payment option brands`() = runTest {
        val eventHandler = mock<(EditPaymentMethodViewInteractor.Event) -> Unit>()
        val interactor = createInteractor(eventHandler = eventHandler)

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceOptionsShown
        )

        verify(eventHandler).invoke(
            EditPaymentMethodViewInteractor.Event.ShowBrands(brand = CardBrand.CartesBancaires)
        )
    }

    @Test
    fun `on selection options hidden without change, should report event specific to hiding payment option brands`() =
        runTest {
            val eventHandler = mock<(EditPaymentMethodViewInteractor.Event) -> Unit>()
            val interactor = createInteractor(eventHandler = eventHandler)

            interactor.handleViewAction(
                EditPaymentMethodViewAction.OnBrandChoiceOptionsDismissed
            )

            verify(eventHandler).invoke(EditPaymentMethodViewInteractor.Event.HideBrands(brand = null))
        }

    @Test
    fun `on selection changed, should report event, 'canUpdate' should be true & selected brand should be changed`() =
        runTest {
            val eventHandler = mock<(EditPaymentMethodViewInteractor.Event) -> Unit>()
            val interactor = createInteractor(eventHandler = eventHandler)

            interactor.handleViewAction(
                EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
            )

            verify(eventHandler).invoke(EditPaymentMethodViewInteractor.Event.HideBrands(brand = CardBrand.Visa))

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
                        ),
                        displayName = "Card",
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
                    ),
                    displayName = "Card",
                )
            )
        }
    }

    @Test
    fun `on remove pressed, should invoke 'onRemove' & succeed removal process`() = runTest {
        val removeOperation: (paymentMethod: PaymentMethod) -> Throwable? = mock {
            onGeneric { invoke(any()) }.thenAnswer {
                null
            }
        }

        val onRemove: PaymentMethodRemoveOperation = { paymentMethod ->
            delay(100)

            removeOperation(paymentMethod)
        }

        val interactor = createInteractor(onRemove = onRemove, workContext = testDispatcher)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        interactor.handleViewAction(EditPaymentMethodViewAction.OnRemoveConfirmed)

        testDispatcher.scheduler.advanceTimeBy(50)

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Removing)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(interactor.currentStatus()).isEqualTo(EditPaymentMethodViewState.Status.Idle)

        verify(removeOperation).invoke(CARD_WITH_NETWORKS_PAYMENT_METHOD)
    }

    @Test
    fun `on remove pressed, should fail removal process`() = runTest {
        val onRemove: (paymentMethod: PaymentMethod) -> Throwable? = mock {
            onGeneric { invoke(any()) }.thenAnswer {
                LocalStripeException("Failed to remove", null)
            }
        }

        val interactor = createInteractor(onRemove = onRemove, workContext = testDispatcher)

        interactor.handleViewAction(EditPaymentMethodViewAction.OnRemoveConfirmed)

        testDispatcher.scheduler.advanceUntilIdle()

        interactor.viewState.test {
            val viewState = awaitItem()

            assertThat(viewState.error).isEqualTo(resolvableString("Failed to remove"))
        }
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

    @Test
    fun `on update pressed, should fail update process`() = runTest {
        val onUpdate: (paymentMethod: PaymentMethod, brand: CardBrand) -> Result<PaymentMethod> = mock {
            onGeneric { invoke(any(), any()) }.thenAnswer {
                Result.failure<PaymentMethod>(
                    LocalStripeException("Failed to update", null)
                )
            }
        }

        val interactor = createInteractor(onUpdate = onUpdate, workContext = testDispatcher)

        interactor.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        interactor.handleViewAction(EditPaymentMethodViewAction.OnUpdatePressed)

        testDispatcher.scheduler.advanceUntilIdle()

        interactor.viewState.test {
            val viewState = awaitItem()

            assertThat(viewState.error).isEqualTo(resolvableString("Failed to update"))
        }
    }

    private fun createInteractor(
        eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit = {},
        onRemove: PaymentMethodRemoveOperation = { null },
        onUpdate: PaymentMethodUpdateOperation = { _, _ -> Result.success(CARD_WITH_NETWORKS_PAYMENT_METHOD) },
        workContext: CoroutineContext = UnconfinedTestDispatcher()
    ): DefaultEditPaymentMethodViewInteractor {
        return DefaultEditPaymentMethodViewInteractor(
            initialPaymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD,
            displayName = "Card",
            eventHandler = eventHandler,
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
