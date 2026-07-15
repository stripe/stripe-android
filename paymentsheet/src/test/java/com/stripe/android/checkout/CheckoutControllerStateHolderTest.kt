package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.previousNewSelection
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
    fun `set updates paymentSelection on the state and emits`() = testScenario {
        stateHolder.state = committedState()

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.set(PaymentSelection.GooglePay)
            assertThat(awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        }

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `set with a new selection emits on selection and stashes it into previousNewSelections`() = testScenario {
        val originalPreviousNewSelections = Bundle()
        stateHolder.state = committedState(previousNewSelections = originalPreviousNewSelections)

        stateHolder.selection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.set(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(stateHolder.state?.previousNewSelections).isNotSameInstanceAs(originalPreviousNewSelections)
        assertThat(originalPreviousNewSelections.isEmpty).isTrue()
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `setTemporary updates temporarySelection on the state and emits`() = testScenario {
        stateHolder.state = committedState()

        stateHolder.temporarySelection.test {
            assertThat(awaitItem()).isNull()
            stateHolder.setTemporary("card")
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
        stateHolder.set(PaymentSelection.GooglePay)
        stateHolder.setTemporary("card")
        stateHolder.setPreviousNewSelections(
            Bundle().apply { putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION) },
        )

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
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        Scenario(
            stateHolder = CheckoutControllerStateHolder(SavedStateHandle(), FakeErrorReporter()),
        ).block()
    }

    private class Scenario(
        val stateHolder: CheckoutControllerStateHolder,
    )

    private companion object {
        const val DEFAULT_KEY = "test_key"
    }
}
