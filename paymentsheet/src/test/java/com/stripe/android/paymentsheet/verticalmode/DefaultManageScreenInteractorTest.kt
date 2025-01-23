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
import com.stripe.android.testing.PaymentMethodFactory
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
        runScenario(initialPaymentMethods, currentSelection = null, handleBackPressed = {}) {
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
    fun cannotRemoveOrEdit_multiplePaymentsMethods_shouldNotNavigateBack() {
        var backPressed = false
        fun handleBackPressed(withDelay: Boolean) {
            assertThat(withDelay).isTrue()
            assertThat(backPressed).isFalse()
            backPressed = true
        }

        val initialPaymentMethods = PaymentMethodFixtures.createCards(2)
        runScenario(
            initialPaymentMethods = initialPaymentMethods,
            currentSelection = PaymentSelection.Saved(initialPaymentMethods[0]),
            isEditing = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            canRemoveSource.value = false
            canEditSource.value = false

            assertThat(backPressed).isFalse()
        }
    }

    @Test
    fun cannotRemoveOrEdit_removesAllButLastPaymentMethod_navsBackWhenEditingFinishes() {
        var backPressed = false
        fun handleBackPressed(withDelay: Boolean) {
            assertThat(withDelay).isTrue()
            assertThat(backPressed).isFalse()
            backPressed = true
        }

        var selectedPaymentMethod: DisplayableSavedPaymentMethod? = null
        fun onSelectPaymentMethod(savedPaymentMethod: DisplayableSavedPaymentMethod) {
            selectedPaymentMethod = savedPaymentMethod
        }

        val paymentMethods = PaymentMethodFactory.cards(3)

        runScenario(
            initialPaymentMethods = paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            onSelectPaymentMethod = ::onSelectPaymentMethod,
            isEditing = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            val lastPaymentMethod = paymentMethods[2]

            paymentMethodsSource.value = listOf(paymentMethods[2])
            canRemoveSource.value = false
            canEditSource.value = false

            assertThat(backPressed).isFalse()

            editingSource.value = false

            assertThat(backPressed).isTrue()
            assertThat(selectedPaymentMethod?.paymentMethod).isEqualTo(lastPaymentMethod)
        }
    }

    @Test
    fun canRemove_doesNotNavBackWhenEditingFinishes() {
        var backPressed = false
        fun handleBackPressed(withDelay: Boolean) {
            assertThat(withDelay).isTrue()
            assertThat(backPressed).isFalse()
            backPressed = true
        }

        val paymentMethods = PaymentMethodFactory.cards(2)
        runScenario(
            initialPaymentMethods = paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            onSelectPaymentMethod = {},
            isEditing = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = listOf(paymentMethods[1])

            assertThat(backPressed).isFalse()

            editingSource.value = false

            assertThat(backPressed).isFalse()
        }
    }

    @Test
    fun `removing the last payment methods navigates back without delay`() {
        var backPressed = false
        fun handleBackPressed(withDelay: Boolean) {
            assertThat(withDelay).isFalse()
            assertThat(backPressed).isFalse()
            backPressed = true
        }

        val paymentMethods = PaymentMethodFactory.cards(2)
        runScenario(
            initialPaymentMethods = paymentMethods,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            onSelectPaymentMethod = {},
            isEditing = true,
            handleBackPressed = ::handleBackPressed,
        ) {
            assertThat(backPressed).isFalse()

            paymentMethodsSource.value = listOf()

            assertThat(backPressed).isTrue()
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

    @Test
    fun `displayableSavedPaymentMethods updated correctly when defaultPaymentMethodId updated`() {
        val initialPaymentMethods = PaymentMethodFixtures.createCards(3)
        runScenario(initialPaymentMethods, currentSelection = null) {
            interactor.state.test {
                defaultPaymentMethodSource.value = null
                var updatedState = awaitItem()
                assertThat(updatedState.paymentMethods[0].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[1].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[2].shouldShowDefaultBadge).isFalse()

                defaultPaymentMethodSource.value = initialPaymentMethods[0].id
                updatedState = awaitItem()
                assertThat(updatedState.paymentMethods[0].shouldShowDefaultBadge).isTrue()
                assertThat(updatedState.paymentMethods[1].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[2].shouldShowDefaultBadge).isFalse()

                defaultPaymentMethodSource.value = initialPaymentMethods[1].id
                updatedState = awaitItem()
                assertThat(updatedState.paymentMethods[0].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[1].shouldShowDefaultBadge).isTrue()
                assertThat(updatedState.paymentMethods[2].shouldShowDefaultBadge).isFalse()

                defaultPaymentMethodSource.value = null
                updatedState = awaitItem()
                assertThat(updatedState.paymentMethods[0].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[1].shouldShowDefaultBadge).isFalse()
                assertThat(updatedState.paymentMethods[2].shouldShowDefaultBadge).isFalse()
            }
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        initialPaymentMethods: List<PaymentMethod>,
        currentSelection: PaymentSelection?,
        isEditing: Boolean = false,
        toggleEdit: () -> Unit = { notImplemented() },
        onSelectPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        handleBackPressed: (withDelay: Boolean) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val paymentMethods = MutableStateFlow(initialPaymentMethods)
        val selection = MutableStateFlow(currentSelection)
        val editing = MutableStateFlow(isEditing)
        val canEdit = MutableStateFlow(true)
        val canRemove = MutableStateFlow(true)
        val dispatcher = UnconfinedTestDispatcher()
        val defaultPaymentMethodId: MutableStateFlow<String?> = MutableStateFlow(null)

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
            providePaymentMethodName = { (it ?: "Missing name").resolvableString },
            onSelectPaymentMethod = onSelectPaymentMethod,
            onUpdatePaymentMethod = { notImplemented() },
            navigateBack = handleBackPressed,
            defaultPaymentMethodId = defaultPaymentMethodId,
            dispatcher = dispatcher
        )

        TestParams(
            interactor = interactor,
            paymentMethodsSource = paymentMethods,
            editingSource = editing,
            canEditSource = canEdit,
            canRemoveSource = canRemove,
            defaultPaymentMethodSource = defaultPaymentMethodId
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private data class TestParams(
        val interactor: ManageScreenInteractor,
        val paymentMethodsSource: MutableStateFlow<List<PaymentMethod>>,
        val editingSource: MutableStateFlow<Boolean>,
        val canEditSource: MutableStateFlow<Boolean>,
        val canRemoveSource: MutableStateFlow<Boolean>,
        val defaultPaymentMethodSource: MutableStateFlow<String?>,
    )
}
