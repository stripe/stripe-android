package com.stripe.android.checkout

import android.os.Bundle
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Session.PaymentOptionDisplayData
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
import com.stripe.android.paymentsheet.model.ErrorPaymentOptionResource
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.simulateProcessDeath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutElementsControllerStateHolderTest {
    @Test
    fun `loaded state projects from the controller state`() = testScenario {
        val committed = committedState()

        stateHolder.loadedStateFlow.test {
            assertThat(awaitItem()).isNull()

            stateHolder.state = CheckoutElementsControllerState.Loaded(committed)

            assertThat(awaitItem()).isSameInstanceAs(committed)
            assertThat(stateHolder.loadedState).isSameInstanceAs(committed)
        }
    }

    @Test
    fun `background state reads reflect committed and cleared state`() = testScenario {
        suspend fun readState() = withContext(Dispatchers.IO) {
            assertThat(Thread.currentThread()).isNotSameInstanceAs(Looper.getMainLooper().thread)
            stateHolder.loadedState
        }

        assertThat(readState()).isNull()

        val committed = committedState(paymentSelection = PaymentSelection.GooglePay)
        stateHolder.loadedState = committed
        assertThat(readState()).isEqualTo(committed)

        stateHolder.loadedState = null
        assertThat(readState()).isNull()
    }

    @Test
    fun `selection projects paymentSelection from the committed state`() = testScenario {
        stateHolder.loadedState = committedState(paymentSelection = PaymentSelection.GooglePay)

        assertThat(stateHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `session projects the paymentOption the factory builds from the committed state`() {
        val expectedOption = PaymentOptionDisplayData(
            paymentOptionResource = ErrorPaymentOptionResource,
            label = "Visa",
            sublabel = "···· 4242",
            billingDetails = null,
            paymentMethodType = "card",
            mandateTextProvider = { null },
        )
        var capturedSelection: PaymentSelection? = null
        val factory = CheckoutPaymentOptionDisplayDataFactory { selection, _, _ ->
            capturedSelection = selection
            expectedOption
        }

        testScenario(paymentOptionFactory = factory) {
            stateHolder.loadedState = committedState(
                paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            )

            assertThat(stateHolder.session.value?.paymentOption).isSameInstanceAs(expectedOption)
            assertThat(stateHolder.session.value?.paymentOption?.label).isEqualTo("Visa")
            assertThat(stateHolder.session.value?.paymentOption?.sublabel).isEqualTo("···· 4242")
            assertThat(capturedSelection).isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        }
    }

    @Test
    fun `session has no payment option when there is no selection`() = testScenario(
        paymentOptionFactory = { selection, _, _ ->
            assertThat(selection).isNull()
            null
        },
    ) {
        stateHolder.loadedState = committedState(paymentSelection = null)

        assertThat(stateHolder.session.value).isNotNull()
        assertThat(stateHolder.session.value?.paymentOption).isNull()
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
            paymentOptionFactory = { _, metadata, _ ->
                assertThat(metadata).isSameInstanceAs(paymentElementMetadata)
                null
            },
            availableExpressButtonTypesFactory = { metadata, _, requiresShippingAddress ->
                assertThat(metadata).isSameInstanceAs(expressCheckoutElementMetadata)
                assertThat(requiresShippingAddress).isTrue()
                emptyList()
            },
        ) {
            stateHolder.loadedState = committedState(
                paymentMethodMetadata = paymentElementMetadata,
                expressCheckoutElementPaymentMethodMetadata = expressCheckoutElementMetadata,
                requiresShippingAddress = true,
            )

            assertThat(stateHolder.session.value).isNotNull()
        }
    }

    @Test
    fun `configured default phone survives process death`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutElementsControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.loadedState = committedState().copy(
            configuration = CheckoutElementsController.Configuration()
                .defaults(CheckoutElementsController.Configuration.Defaults().phone("+15555551234"))
                .build(),
        )

        val restoredStateHolder = CheckoutElementsControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle.simulateProcessDeath(),
        )

        val restoredState = requireNotNull(restoredStateHolder.loadedState)
        assertThat(restoredState.configuration.defaults.phone).isEqualTo("+15555551234")
    }

    @Test
    fun `null default phone survives process death`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutElementsControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.loadedState = committedState().copy(
            configuration = CheckoutElementsController.Configuration()
                .defaults(CheckoutElementsController.Configuration.Defaults().phone(null))
                .build(),
        )

        val restoredStateHolder = CheckoutElementsControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle.simulateProcessDeath(),
        )

        val restoredState = requireNotNull(restoredStateHolder.loadedState)
        assertThat(restoredState.configuration.defaults.phone).isNull()
    }

    @Test
    fun `restored pending saved selection is reset to idle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutElementsControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.loadedState = committedState().copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending(
                "pm_restored",
            ),
        )

        val restoredStateHolder = CheckoutElementsControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle.simulateProcessDeath(),
        )

        assertThat(restoredStateHolder.loadedState?.savedPaymentMethodSelectionState)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
    }

    @Test
    fun `setSelection with the current selection keeps a saved selection failure`() = testScenario {
        val failure = SavedPaymentMethodSelectionState.Failed(
            IllegalStateException("Selection failed").stripeErrorMessage(),
        )
        stateHolder.loadedState = committedState(paymentSelection = PaymentSelection.GooglePay).copy(
            savedPaymentMethodSelectionState = failure,
        )

        stateHolder.setSelection(PaymentSelection.GooglePay)

        assertThat(stateHolder.loadedState?.savedPaymentMethodSelectionState).isEqualTo(failure)
    }

    @Test
    fun `setSelection with a different selection resets a saved selection failure to idle`() = testScenario {
        stateHolder.loadedState = committedState(paymentSelection = PaymentSelection.GooglePay).copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Failed(
                IllegalStateException("Selection failed").stripeErrorMessage(),
            ),
        )

        stateHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(stateHolder.loadedState?.paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(stateHolder.loadedState?.savedPaymentMethodSelectionState)
            .isEqualTo(SavedPaymentMethodSelectionState.Idle)
    }

    @Test
    fun `failed selection error survives saved state restoration`() = runTest {
        val handle = SavedStateHandle()
        val holder = CheckoutElementsControllerStateFactory.createStateHolder(handle)
        val error = APIConnectionException()
        holder.loadedState = committedState(paymentSelection = PaymentSelection.GooglePay).copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Failed(
                error.stripeErrorMessage(),
            ),
        )

        val restored = CheckoutElementsControllerStateFactory.createStateHolder(handle.simulateProcessDeath())

        assertThat(restored.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(restored.loadedState?.savedPaymentMethodSelectionState)
            .isEqualTo(SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()))
    }

    @Test
    fun `setSelection updates paymentSelection on the state and emits`() = testScenario {
        stateHolder.loadedState = committedState()

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setSelection(PaymentSelection.GooglePay)
            assertThat(awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        }

        assertThat(stateHolder.loadedState?.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `setSelection with a new selection emits and stashes it into previousNewSelections`() = testScenario {
        val originalPreviousNewSelections = Bundle()
        stateHolder.loadedState = committedState(previousNewSelections = originalPreviousNewSelections)

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }

        assertThat(stateHolder.loadedState?.paymentSelection).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(stateHolder.loadedState?.previousNewSelections).isNotSameInstanceAs(originalPreviousNewSelections)
        assertThat(originalPreviousNewSelections.isEmpty).isTrue()
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `setTemporarySelection updates temporarySelection on the state and emits`() = testScenario {
        stateHolder.loadedState = committedState()

        stateHolder.temporarySelection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setTemporarySelection("card")
            assertThat(awaitItem()).isEqualTo("card")
        }

        assertThat(stateHolder.loadedState?.temporarySelection).isEqualTo("card")
    }

    @Test
    fun `setPreviousNewSelections merges into the existing previousNewSelections rather than replacing`() =
        testScenario {
            val originalPreviousNewSelections = Bundle().apply {
                putParcelable("card", PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            }
            stateHolder.loadedState = committedState(
                previousNewSelections = originalPreviousNewSelections,
            )

            val bundle = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            }
            stateHolder.setPreviousNewSelections(bundle)

            assertThat(stateHolder.loadedState?.previousNewSelections)
                .isNotSameInstanceAs(originalPreviousNewSelections)
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

        assertThat(stateHolder.loadedState).isNull()
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
        val stateHolder = CheckoutElementsControllerStateHolder(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    CheckoutElementsControllerStateHolder.STATE_KEY to CheckoutElementsControllerState.Loaded(restored)
                )
            ),
            errorReporter = FakeErrorReporter(),
            paymentOptionFactory = { _, _, _ -> null },
            availableExpressButtonTypesFactory = FakeAvailableExpressButtonTypesFactory(),
        )

        assertThat(stateHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(stateHolder.temporarySelection.value).isEqualTo("card")
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
    ) = CheckoutElementsControllerLoadedState(
        configuration = CheckoutElementsController.Configuration().build(),
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
        temporarySelection = temporarySelection,
        previousNewSelections = previousNewSelections,
        linkEagerPresentationSuppressed = false,
        mandateState = CheckoutMandateState.initial(),
    )

    private fun testScenario(
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory =
            CheckoutPaymentOptionDisplayDataFactory { _, _, _ -> null },
        availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory =
            FakeAvailableExpressButtonTypesFactory(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()
        Scenario(
            stateHolder = CheckoutElementsControllerStateHolder(
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
        val stateHolder: CheckoutElementsControllerStateHolder,
        val errorReporter: FakeErrorReporter,
    )
}
