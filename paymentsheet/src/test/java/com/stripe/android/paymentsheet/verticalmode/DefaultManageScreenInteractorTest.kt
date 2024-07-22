package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `updating canEdit updates state`() {
        val initialPaymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(initialPaymentMethods, currentSelection = null) {
            interactor.state.test {
                assertThat(awaitItem().canEdit).isTrue()

                canEditSource.value = false
                assertThat(awaitItem().canEdit).isFalse()
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

            assertThat(backPressed).isFalse()

            editingSource.value = false

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

            assertThat(backPressed).isFalse()

            editingSource.value = false

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

            assertThat(backPressed).isFalse()

            editingSource.value = false

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

            assertThat(backPressed).isFalse()

            interactor.state.test {
                awaitItem().run {
                    assertThat(canDelete).isTrue()
                }
            }
        }
    }

    @Test
    fun `handleViewAction ToggleEdit calls toggleEdit`() {
        var hasCalledToggleEdit = false
        val initialPaymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = null,
            toggleEdit = { hasCalledToggleEdit = true },
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.ToggleEdit)
            assertThat(hasCalledToggleEdit).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        initialPaymentMethods: List<PaymentMethod>?,
        currentSelection: PaymentSelection?,
        isEditing: Boolean = false,
        toggleEdit: () -> Unit = { notImplemented() },
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        handleBackPressed: () -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val paymentMethods = MutableStateFlow(initialPaymentMethods)
        val selection = MutableStateFlow(currentSelection)
        val editing = MutableStateFlow(isEditing)
        val canEdit = MutableStateFlow(true)
        val dispatcher = UnconfinedTestDispatcher()

        val interactor = DefaultManageScreenInteractor(
            paymentMethods = paymentMethods,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList()),
            ),
            selection = selection,
            editing = editing,
            canEdit = canEdit,
            toggleEdit = toggleEdit,
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            providePaymentMethodName = { (it ?: "Missing name").resolvableString },
            onSelectPaymentMethod = onSelectPaymentMethod,
            onDeletePaymentMethod = { notImplemented() },
            onEditPaymentMethod = { notImplemented() },
            navigateBack = handleBackPressed,
            dispatcher = dispatcher,
            isLiveMode = true,
        )

        TestParams(
            interactor = interactor,
            paymentMethodsSource = paymentMethods,
            editingSource = editing,
            canEditSource = canEdit,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private data class TestParams(
        val interactor: ManageScreenInteractor,
        val paymentMethodsSource: MutableStateFlow<List<PaymentMethod>?>,
        val editingSource: MutableStateFlow<Boolean>,
        val canEditSource: MutableStateFlow<Boolean>,
    )
}
