package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.elements.ece.FakeAvailableExpressButtonTypesFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
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
    fun `session does not emit when temporary selection changes`() = testScenario(
        paymentOptionFactory = freshPaymentOptionFactory(),
    ) {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        stateHolder.session.test {
            val initialPaymentOption = awaitItem()?.paymentOption
            assertThat(initialPaymentOption?.label).isEqualTo("Google Pay")

            stateHolder.setTemporarySelection("card")

            assertThat(stateHolder.session.value?.paymentOption).isSameInstanceAs(initialPaymentOption)
            expectNoEvents()
        }
    }

    @Test
    fun `session value reuses the payment option without a collector`() = testScenario(
        paymentOptionFactory = freshPaymentOptionFactory(),
    ) {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        val initialPaymentOption = stateHolder.session.value?.paymentOption

        assertThat(stateHolder.session.value?.paymentOption).isSameInstanceAs(initialPaymentOption)

        stateHolder.session.test {
            assertThat(awaitItem()?.paymentOption).isSameInstanceAs(initialPaymentOption)
        }
    }

    @Test
    fun `clearing session state does not reuse the previous payment option`() = testScenario(
        paymentOptionFactory = freshPaymentOptionFactory(),
    ) {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)
        val originalPaymentOption = stateHolder.session.value?.paymentOption

        stateHolder.state = null
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        val newPaymentOption = stateHolder.session.value?.paymentOption
        assertThat(newPaymentOption).isNotSameInstanceAs(originalPaymentOption)
    }

    @Test
    fun `session emits when metadata changes even if payment option content stays the same`() = testScenario(
        paymentOptionFactory = freshPaymentOptionFactory(),
    ) {
        val initialMetadata = PaymentMethodMetadataFactory.create()
        stateHolder.state = committedState(
            paymentSelection = PaymentSelection.GooglePay,
            paymentMethodMetadata = initialMetadata,
        )

        stateHolder.session.test {
            val initialSession = awaitItem()
            assertThat(initialSession?.paymentOption?.label).isEqualTo("Google Pay")

            val changedMetadata = PaymentMethodMetadataFactory.create(
                paymentMethodOrder = listOf("card"),
            )
            assertThat(changedMetadata).isNotEqualTo(initialMetadata)
            stateHolder.state = requireNotNull(stateHolder.state).copy(
                paymentMethodMetadata = changedMetadata,
            )

            val updatedSession = awaitItem()
            assertThat(updatedSession?.paymentOption?.label)
                .isEqualTo(initialSession?.paymentOption?.label)
            assertThat(updatedSession?.paymentOption)
                .isNotSameInstanceAs(initialSession?.paymentOption)
        }
    }

    @Test
    fun `session emits when payment option label changes for the same selection`() = run {
        var label = "Google Pay"
        val paymentOptionFactory = CheckoutPaymentOptionDisplayDataFactory { selection, _ ->
            selection?.let { selected ->
                PaymentOptionDisplayData(
                    imageLoader = { error("Not expected to load an image for $selected") },
                    label = label,
                    billingDetails = null,
                    paymentMethodType = "google_pay",
                    mandateText = null,
                )
            }
        }

        testScenario(paymentOptionFactory = paymentOptionFactory) {
            stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

            stateHolder.session.test {
                assertThat(awaitItem()?.paymentOption?.label).isEqualTo("Google Pay")

                label = "Updated Google Pay"
                stateHolder.setTemporarySelection("card")

                assertThat(awaitItem()?.paymentOption?.label).isEqualTo("Updated Google Pay")
            }
        }
    }

    @Test
    fun `session emits the new payment option when selection changes`() = testScenario(
        paymentOptionFactory = freshPaymentOptionFactory(),
    ) {
        stateHolder.state = committedState(paymentSelection = PaymentSelection.GooglePay)

        stateHolder.session.test {
            assertThat(awaitItem()?.paymentOption?.label).isEqualTo("Google Pay")

            stateHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

            val updatedSession = awaitItem()
            assertThat(updatedSession?.paymentOption?.label).isEqualTo("Cash App")
            assertThat(updatedSession?.paymentOption?.paymentMethodType).isEqualTo("cashapp")
        }
    }

    @Test
    fun `session emits when card brand changes even though its label stays the same`() = testScenario(
        paymentOptionFactory = CheckoutPaymentOptionDisplayDataFactory { selection, _ ->
            (selection as? PaymentSelection.New.Card)?.let { card ->
                PaymentOptionDisplayData(
                    imageLoader = { error("Not expected to load an image for ${card.brand}") },
                    label = "···· ${card.last4}",
                    billingDetails = null,
                    paymentMethodType = "card",
                    mandateText = null,
                )
            }
        },
    ) {
        val visaSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(brand = CardBrand.Visa)
        val amexSelection = visaSelection.copy(brand = CardBrand.AmericanExpress)
        stateHolder.state = committedState(paymentSelection = visaSelection)

        stateHolder.session.test {
            val initialSession = awaitItem()
            val initialLabel = initialSession?.paymentOption?.label
            assertThat(initialLabel).isEqualTo("···· ${visaSelection.last4}")

            stateHolder.setSelection(amexSelection)

            val updatedSession = awaitItem()
            assertThat(updatedSession?.paymentOption?.label).isEqualTo(initialLabel)
            assertThat(updatedSession?.paymentOption)
                .isNotSameInstanceAs(initialSession?.paymentOption)
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
        temporarySelection = temporarySelection,
        previousNewSelections = previousNewSelections,
        linkEagerPresentationSuppressed = false,
    )

    private fun freshPaymentOptionFactory() = CheckoutPaymentOptionDisplayDataFactory { selection, _ ->
        selection?.let { selected ->
            val isGooglePay = selected == PaymentSelection.GooglePay
            PaymentOptionDisplayData(
                imageLoader = { error("Not expected to load an image for $selected") },
                label = if (isGooglePay) "Google Pay" else "Cash App",
                billingDetails = null,
                paymentMethodType = if (isGooglePay) "google_pay" else "cashapp",
                mandateText = null,
            )
        }
    }

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
