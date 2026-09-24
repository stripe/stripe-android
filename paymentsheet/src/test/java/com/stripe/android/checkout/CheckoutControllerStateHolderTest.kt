package com.stripe.android.checkout

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.elements.ece.FakeAvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.simulateProcessDeath
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerStateHolderTest {
    @Test
    fun `selection projects paymentSelection from the committed state`() = testScenario {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        assertThat(stateHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `session projects the paymentOption the factory builds from the committed state`() {
        val expectedOption = PaymentOptionDisplayData(
            imageLoader = { error("not needed for this test") },
            label = "Google Pay",
            billingDetails = null,
            paymentMethodType = "google_pay",
            mandateText = null,
        )
        var capturedSelection: PaymentSelection? = null
        val factory = CheckoutPaymentOptionDisplayDataFactory { selection, _ ->
            capturedSelection = selection
            expectedOption
        }

        testScenario(paymentOptionFactory = factory) {
            stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

            assertThat(stateHolder.session.value?.paymentOption).isSameInstanceAs(expectedOption)
            assertThat(capturedSelection).isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `session builds payment element and express checkout element data with their respective metadata`() {
        val paymentElementMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodOrder = listOf("card"),
        )
        val expressCheckoutElementMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodOrder = listOf("link"),
        )

        testScenario(
            paymentOptionFactory = { _, metadata ->
                assertThat(metadata).isSameInstanceAs(paymentElementMetadata)
                null
            },
            availableExpressButtonTypesFactory = { metadata, _, requiresShippingAddress ->
                assertThat(metadata).isSameInstanceAs(expressCheckoutElementMetadata)
                assertThat(requiresShippingAddress).isTrue()
                emptyList()
            },
        ) {
            stateHolder.state = committedState(
                paymentMethodMetadata = paymentElementMetadata,
                expressCheckoutElementPaymentMethodMetadata = expressCheckoutElementMetadata,
                requiresShippingAddress = true,
            )

            assertThat(stateHolder.session.value).isNotNull()
        }
    }

    @Test
    fun `saved selection state guards pending operations`() = testScenario {
        stateHolder.state = committedState()

        stateHolder.savedPaymentMethodSelectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            assertThat(stateHolder.tryBeginSavedSelection()).isTrue()
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Pending)

            stateHolder.clearErrorMessages()
            assertThat(stateHolder.savedPaymentMethodSelectionState.value)
                .isEqualTo(SavedPaymentMethodSelectionState.Pending)
            expectNoEvents()

            assertThat(stateHolder.tryBeginSavedSelection()).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `session does not emit when saved selection becomes pending`() = testScenario {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        stateHolder.session.test {
            assertThat(awaitItem()).isNotNull()

            assertThat(stateHolder.tryBeginSavedSelection()).isTrue()

            expectNoEvents()
        }
    }

    @Test
    fun `restored pending saved selection is reset to idle and can be retried`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.state = committedState().copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending,
        )

        val restoredStateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle.simulateProcessDeath(),
        )

        assertThat(restoredStateHolder.state?.savedPaymentMethodSelectionState)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
        assertThat(restoredStateHolder.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
        assertThat(restoredStateHolder.tryBeginSavedSelection()).isTrue()
    }

    @Test
    fun `finishing a failed saved selection preserves the failure until it is cleared`() = testScenario {
        stateHolder.state = committedState()
        val error = IllegalStateException("Selection failed")
        stateHolder.tryBeginSavedSelection()
        stateHolder.failSavedSelection(error)

        stateHolder.finishSavedSelection()

        assertThat(stateHolder.selectionError.value).isEqualTo(error.stripeErrorMessage())
        assertThat(stateHolder.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)

        stateHolder.clearErrorMessages()

        assertThat(stateHolder.selectionError.value).isNull()
        assertThat(stateHolder.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
    }

    @Test
    fun `state replacement preserves failure for equal selection and clears it for changed selection`() = testScenario {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)
        val error = IllegalStateException("Selection failed")
        stateHolder.failSavedSelection(error)

        stateHolder.state = requireNotNull(stateHolder.state).copy()
        assertThat(stateHolder.selectionError.value).isEqualTo(error.stripeErrorMessage())

        stateHolder.state = requireNotNull(stateHolder.state).copy(
            paymentSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
        )
        assertThat(stateHolder.selectionError.value).isNull()
    }

    @Test
    fun `explicit equal selection clears failure`() = testScenario {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)
        stateHolder.failSavedSelection(IllegalStateException("Selection failed"))

        stateHolder.setSelection(PaymentSelection.GooglePay)

        assertThat(stateHolder.selectionError.value).isNull()
    }

    @Test
    fun `failed selection error survives parcelled saved state restoration`() = runTest {
        val handle = SavedStateHandle()
        val holder = CheckoutControllerStateFactory.createStateHolder(handle)
        holder.state = committedState(paymentSelection = PaymentSelection.GooglePay)
        holder.tryBeginSavedSelection()
        val error = APIConnectionException()
        holder.failSavedSelection(error)

        val restored = CheckoutControllerStateFactory.createStateHolder(handle.parcelledRestore())

        restored.selectionError.test {
            assertThat(awaitItem()).isEqualTo(error.stripeErrorMessage())
        }
        assertThat(restored.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(restored.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
        assertThat(restored.state).isNotSameInstanceAs(holder.state)
    }

    @Test
    fun `pending retry restores idle without the cleared error`() = runTest {
        val handle = SavedStateHandle()
        val holder = CheckoutControllerStateFactory.createStateHolder(handle)
        holder.state = committedState(paymentSelection = PaymentSelection.GooglePay)
        holder.failSavedSelection(IllegalStateException("Selection failed"))
        assertThat(holder.selectionError.value).isNotNull()

        assertThat(holder.tryBeginSavedSelection()).isTrue()
        assertThat(holder.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Pending)

        val restored = CheckoutControllerStateFactory.createStateHolder(handle.simulateProcessDeath())

        assertThat(restored.selectionError.value).isNull()
        assertThat(restored.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(restored.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
        assertThat(restored.tryBeginSavedSelection()).isTrue()
    }

    @Suppress("RestrictedApi")
    private fun SavedStateHandle.parcelledRestore(): SavedStateHandle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedStateProvider().saveState())
            parcel.setDataPosition(0)
            SavedStateHandle.createHandle(
                requireNotNull(parcel.readBundle(CheckoutControllerState::class.java.classLoader)),
                null,
            )
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `setSelection updates paymentSelection on the state and emits`() = testScenario {
        stateHolder.state = committedState()

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setSelection(PaymentSelection.GooglePay)
            assertThat(awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        }

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `setSelection returns a pending saved selection to idle`() = testScenario {
        stateHolder.state = committedState().copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending,
        )

        stateHolder.savedPaymentMethodSelectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Pending)

            stateHolder.setSelection(PaymentSelection.GooglePay)

            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
        }
    }

    @Test
    fun `setSelection acknowledges the SEPA mandate`() = testScenario {
        stateHolder.state = committedState()
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)

        stateHolder.setSelection(selection)

        assertThat(stateHolder.state?.paymentSelection?.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `setSelection with a new selection emits and stashes it into previousNewSelections`() = testScenario {
        val originalPreviousNewSelections = Bundle()
        stateHolder.state = committedState(previousNewSelections = originalPreviousNewSelections)

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(stateHolder.state?.previousNewSelections).isNotSameInstanceAs(originalPreviousNewSelections)
        assertThat(originalPreviousNewSelections.isEmpty).isTrue()
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `setTemporarySelection updates temporarySelection on the state and emits`() = testScenario {
        stateHolder.state = committedState()

        stateHolder.temporarySelection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setTemporarySelection("card")
            assertThat(awaitItem()).isEqualTo("card")
        }

        assertThat(stateHolder.state?.temporarySelection).isEqualTo("card")
    }

    @Test
    fun `setPreviousNewSelections merges into the existing previousNewSelections rather than replacing`() =
        testScenario {
            val originalPreviousNewSelections = Bundle().apply {
                putParcelable("card", PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            }
            stateHolder.state = committedState(
                previousNewSelections = originalPreviousNewSelections,
            )

            val bundle = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            }
            stateHolder.setPreviousNewSelections(bundle)

            assertThat(stateHolder.state?.previousNewSelections).isNotSameInstanceAs(originalPreviousNewSelections)
            assertThat(stateHolder.getPreviousNewSelection("card"))
                .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(stateHolder.getPreviousNewSelection("cashapp"))
                .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            assertThat(originalPreviousNewSelections.previousNewSelection("cashapp")).isNull()
        }

    @Test
    fun `selection setters no-op before the state is committed`() = testScenario {
        stateHolder.setSelection(PaymentSelection.GooglePay)
        assertSetBeforeLoadError(operation = "setSelection")

        stateHolder.setTemporarySelection("card")
        assertSetBeforeLoadError(operation = "setTemporarySelection")

        stateHolder.setPreviousNewSelections(
            Bundle().apply { putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION) },
        )
        assertSetBeforeLoadError(operation = "setPreviousNewSelections")

        assertThat(stateHolder.state).isNull()
        assertThat(stateHolder.selection.value).isNull()
        assertThat(stateHolder.temporarySelection.value).isNull()
        assertThat(stateHolder.getPreviousNewSelection("cashapp")).isNull()
    }

    @Test
    fun `projects selection, temporarySelection and previousNewSelections from a restored state`() = runTest {
        // Simulates process-death restore: a committed state is read back from SavedStateHandle by a
        // freshly constructed holder, and every selection projection must reflect it.
        val restored = committedState(
            paymentSelection = PaymentSelection.GooglePay,
            temporarySelection = "card",
            previousNewSelections = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            },
        )
        val stateHolder = CheckoutControllerStateHolder(
            savedStateHandle = SavedStateHandle(mapOf(CheckoutControllerStateHolder.STATE_KEY to restored)),
            errorReporter = FakeErrorReporter(),
            paymentOptionFactory = { _, _ -> null },
            availableExpressButtonTypesFactory = FakeAvailableExpressButtonTypesFactory(),
        )

        assertThat(stateHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(stateHolder.temporarySelection.value).isEqualTo("card")
        assertThat(stateHolder.savedPaymentMethodSelectionState.value)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    private fun committedState(
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        expressCheckoutElementPaymentMethodMetadata: PaymentMethodMetadata? = PaymentMethodMetadataFactory.create(),
        requiresShippingAddress: Boolean = false,
    ) = CheckoutControllerState(
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            requiresShippingAddress = requiresShippingAddress,
        ),
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(email = null),
        paymentMethodMetadata = paymentMethodMetadata,
        expressCheckoutElementPaymentMethodMetadata = expressCheckoutElementPaymentMethodMetadata,
        embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        paymentSelection = paymentSelection,
        savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
        selectionError = null,
        temporarySelection = temporarySelection,
        previousNewSelections = previousNewSelections,
        linkEagerPresentationSuppressed = false,
    )

    private fun testScenario(
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory =
            CheckoutPaymentOptionDisplayDataFactory { _, _ -> null },
        availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory =
            FakeAvailableExpressButtonTypesFactory(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()
        Scenario(
            stateHolder = CheckoutControllerStateHolder(
                savedStateHandle = SavedStateHandle(),
                errorReporter = errorReporter,
                paymentOptionFactory = paymentOptionFactory,
                availableExpressButtonTypesFactory = availableExpressButtonTypesFactory,
            ),
            errorReporter = errorReporter,
        ).block()
        errorReporter.ensureAllEventsConsumed()
    }

    private suspend fun Scenario.assertSetBeforeLoadError(operation: String) {
        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SELECTION_SET_BEFORE_LOAD)
        assertThat(call.additionalNonPiiParams).isEqualTo(mapOf("operation" to operation))
    }

    private class Scenario(
        val stateHolder: CheckoutControllerStateHolder,
        val errorReporter: FakeErrorReporter,
    )
}
