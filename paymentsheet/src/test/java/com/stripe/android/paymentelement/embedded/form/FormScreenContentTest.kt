package com.stripe.android.paymentelement.embedded.form

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.verticalmode.FakeVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FormScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val closeInteractorRule = CleanupTestRule(SavedPaymentMethodConfirmInteractor::close)

    @Test
    fun `saved payment method confirm interactor is reused for same selection`() {
        val factory = TrackingSavedPaymentMethodConfirmInteractorFactory(closeInteractorRule)
        val selection = savedSelection("pm_1")
        val state = mutableStateOf(defaultState(savedPaymentSelectionToConfirm = selection))

        setContent(
            stateProvider = { state.value },
            savedPaymentMethodConfirmInteractorFactory = factory,
        )

        composeRule.waitForIdle()

        assertThat(factory.interactors).hasSize(1)

        composeRule.runOnIdle {
            state.value = state.value.copy(isEnabled = !state.value.isEnabled)
        }

        composeRule.waitForIdle()

        assertThat(factory.interactors).hasSize(1)
        assertThat(factory.interactors.single().closeCalls).isEqualTo(0)
    }

    @Test
    fun `saved payment method confirm interactor is closed when confirmation leaves composition`() {
        val factory = TrackingSavedPaymentMethodConfirmInteractorFactory(closeInteractorRule)
        val selection = savedSelection("pm_1")
        val state = mutableStateOf(defaultState(savedPaymentSelectionToConfirm = selection))

        setContent(
            stateProvider = { state.value },
            savedPaymentMethodConfirmInteractorFactory = factory,
        )

        composeRule.waitForIdle()

        val interactor = factory.interactors.single()

        composeRule.runOnIdle {
            state.value = state.value.copy(savedPaymentSelectionToConfirm = null)
        }

        composeRule.waitForIdle()

        assertThat(interactor.closeCalls).isEqualTo(1)
    }

    @Test
    fun `saved payment method confirm interactor is recreated when selection changes`() {
        val factory = TrackingSavedPaymentMethodConfirmInteractorFactory(closeInteractorRule)
        val state = mutableStateOf(defaultState(savedPaymentSelectionToConfirm = savedSelection("pm_1")))

        setContent(
            stateProvider = { state.value },
            savedPaymentMethodConfirmInteractorFactory = factory,
        )

        composeRule.waitForIdle()

        val firstInteractor = factory.interactors.single()

        composeRule.runOnIdle {
            state.value = state.value.copy(savedPaymentSelectionToConfirm = savedSelection("pm_2"))
        }

        composeRule.waitForIdle()

        assertThat(factory.interactors).hasSize(2)
        assertThat(firstInteractor.closeCalls).isEqualTo(1)
        assertThat(factory.interactors.last().closeCalls).isEqualTo(0)
    }

    private fun setContent(
        stateProvider: () -> SheetActivityStateHolder.State,
        savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
    ) {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeVerticalModeFormInteractor.create(
            paymentMethodCode = "card",
            metadata = metadata,
        )

        composeRule.setContent {
            ViewModelStoreOwnerContext {
                FormScreenContent(
                    interactor = interactor,
                    eventReporter = FakeEventReporter(),
                    onClick = {},
                    onProcessingCompleted = {},
                    state = stateProvider(),
                    updateSelection = {},
                    savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
                )
            }
        }
    }

    private fun defaultState(
        savedPaymentSelectionToConfirm: PaymentSelection.Saved?,
    ): SheetActivityStateHolder.State {
        return SheetActivityStateHolder.State(
            primaryButtonLabel = "Continue".resolvableString,
            isEnabled = true,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            shouldDisplayLockIcon = false,
            savedPaymentSelectionToConfirm = savedPaymentSelectionToConfirm,
        )
    }

    private fun savedSelection(id: String): PaymentSelection.Saved {
        return PaymentSelection.Saved(PaymentMethodFactory.card(id = id))
    }

    private class TrackingSavedPaymentMethodConfirmInteractorFactory(
        private val closeInteractorRule: CleanupTestRule<SavedPaymentMethodConfirmInteractor>,
    ) : SavedPaymentMethodConfirmInteractor.Factory {
        val interactors = mutableListOf<TrackingSavedPaymentMethodConfirmInteractor>()

        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit
        ): SavedPaymentMethodConfirmInteractor {
            return TrackingSavedPaymentMethodConfirmInteractor(initialSelection).also {
                closeInteractorRule.track(it)
                interactors.add(it)
            }
        }
    }

    private class TrackingSavedPaymentMethodConfirmInteractor(
        initialSelection: PaymentSelection.Saved,
    ) : SavedPaymentMethodConfirmInteractor {
        var closeCalls = 0
            private set

        override val state = stateFlowOf(
            SavedPaymentMethodConfirmInteractor.State(
                displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = initialSelection.paymentMethod,
                ),
                linkBrand = LinkBrand.Link,
                form = SavedPaymentMethodConfirmInteractor.State.Form(
                    elements = emptyList(),
                    enabled = true,
                ),
            )
        )

        override fun close() {
            closeCalls += 1
        }
    }
}
