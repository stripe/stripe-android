package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultManageScreenInteractorTest {

    @Test
    fun initializeState_nullCurrentSelection() {
        val initialPaymentMethods = PaymentMethodFixtures.createCards(2).plus(
            // This is here because an easy bug to write would be selecting a PM with a null ID when the current
            // selection is also null
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = null)
        )
        runScenario(initialPaymentMethods, currentSelection = null) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(currentSelection).isNull()
                    assertThat(paymentMethods).hasSize(3)
                }
            }
        }
    }

    @Test
    fun initializeState_currentSelectionFoundCorrectly() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0])
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(currentSelection?.paymentMethod).isEqualTo(paymentMethods[0])
                }
            }
        }
    }

    @Test
    fun initializeState_noCurrentSelectionIfEditing() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            isEditing = true,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(currentSelection).isNull()
                }
            }
        }
    }

    @Test
    fun removeLastPaymentMethod_shouldNavigateBack() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val initialPaymentMethods = PaymentMethodFixtures.createCards(1)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = emptyList()

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isTrue()
        }
    }

    @Test
    fun removeSecondToLastPaymentMethod_cantRemoveLastPm_shouldNavigateBack() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val initialPaymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = initialPaymentMethods.minus(initialPaymentMethods[0])

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isTrue()
        }
    }

    @Test
    fun removeSecondToLastPaymentMethod_cantRemoveLastPm_cbcEligible_hidesDeleteButton() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val nonCbcCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val cbcCard = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val initialPaymentMethods = listOf(nonCbcCard, cbcCard)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = listOf(cbcCard)

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()

            interactor.state.test {
                awaitItem().run {
                    assertThat(canDelete).isFalse()
                }
            }
        }
    }

    @Test
    fun removeSecondToLastPaymentMethod_cantRemoveLastPm_cbcEligible_navsBackWhenEditingFinishes() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        var selectedPaymentMethod: DisplayableSavedPaymentMethod? = null
        fun onSelectPaymentMethod(savedPaymentMethod: DisplayableSavedPaymentMethod) {
            selectedPaymentMethod = savedPaymentMethod
        }

        val nonCbcCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val cbcCard = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val initialPaymentMethods = listOf(nonCbcCard, cbcCard)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            onSelectPaymentMethod = ::onSelectPaymentMethod,
            isEditing = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = listOf(cbcCard)

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()

            editingSource.value = false

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isTrue()
            assertThat(selectedPaymentMethod?.paymentMethod).isEqualTo(cbcCard)
        }
    }

    @Test
    fun removeSecondToLastPaymentMethod_canRemoveLastPm_cbcEligible_doesNotNavBackWhenEditingFinishes() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val nonCbcCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val cbcCard = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val initialPaymentMethods = listOf(nonCbcCard, cbcCard)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            onSelectPaymentMethod = {},
            isEditing = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = listOf(nonCbcCard)

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()

            editingSource.value = false

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()
        }
    }

    @Test
    fun removeThirdToLastPaymentMethod_doesNotNavBackWhenEditingFinishes() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val initialPaymentMethods = PaymentMethodFixtures.createCards(3)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            onSelectPaymentMethod = {},
            allowsRemovalOfLastSavedPaymentMethod = false,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = initialPaymentMethods.minus(initialPaymentMethods[0])

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()

            editingSource.value = false

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()
        }
    }

    @Test
    fun removeSecondToLastPaymentMethod_canRemoveLastPm_doesntNavigateBackOrHideButtons() {
        var backPressed = false
        fun handleBackPressed() {
            backPressed = true
        }

        val initialPaymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = initialPaymentMethods.minus(initialPaymentMethods[0])

            dispatcher.scheduler.advanceUntilIdle()
            assertThat(backPressed).isFalse()

            interactor.state.test {
                awaitItem().run {
                    assertThat(canDelete).isTrue()
                }
            }
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        initialPaymentMethods: List<PaymentMethod>?,
        currentSelection: PaymentSelection?,
        isEditing: Boolean = false,
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        handleBackPressed: () -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val paymentMethods = MutableStateFlow(initialPaymentMethods)
        val selection = MutableStateFlow(currentSelection)
        val editing = MutableStateFlow(isEditing)
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        val interactor = DefaultManageScreenInteractor(
            paymentMethods = paymentMethods,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList()),
            ),
            selection = selection,
            editing = editing,
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            providePaymentMethodName = { it ?: "Missing name" },
            onSelectPaymentMethod = onSelectPaymentMethod,
            onDeletePaymentMethod = { notImplemented() },
            onEditPaymentMethod = { notImplemented() },
            navigateBack = handleBackPressed,
            dispatcher = dispatcher,
        )

        TestParams(
            interactor = interactor,
            paymentMethodsSource = paymentMethods,
            editingSource = editing,
            dispatcher = dispatcher
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private data class TestParams(
        val interactor: ManageScreenInteractor,
        val dispatcher: TestDispatcher,
        val paymentMethodsSource: MutableStateFlow<List<PaymentMethod>?>,
        val editingSource: MutableStateFlow<Boolean>,
    )
}
