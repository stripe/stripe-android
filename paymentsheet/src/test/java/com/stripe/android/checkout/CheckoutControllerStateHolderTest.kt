package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ece.FakeAvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
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
    fun `checkoutSession projects the paymentOption the factory builds from the committed state`() {
        val expectedOption = PaymentOptionDisplayData(
            imageLoader = { error("not needed for this test") },
            label = "Google Pay",
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

            assertThat(stateHolder.checkoutSession.value?.paymentOptionDisplayData).isSameInstanceAs(expectedOption)
            assertThat(capturedSelection).isEqualTo(PaymentSelection.GooglePay)
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
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    private fun committedState(
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
    ) = CheckoutControllerState(
        key = DEFAULT_KEY,
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(),
        integrationLaunched = false,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        paymentSelection = paymentSelection,
        temporarySelection = temporarySelection,
        previousNewSelections = previousNewSelections,
    )

    private fun testScenario(
        paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory =
            CheckoutPaymentOptionDisplayDataFactory { _, _ -> null },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()
        Scenario(
            stateHolder = CheckoutControllerStateHolder(
                savedStateHandle = SavedStateHandle(),
                errorReporter = errorReporter,
                paymentOptionFactory = paymentOptionFactory,
                availableExpressButtonTypesFactory = FakeAvailableExpressButtonTypesFactory(),
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

    private companion object {
        const val DEFAULT_KEY = "test_key"
    }
}
