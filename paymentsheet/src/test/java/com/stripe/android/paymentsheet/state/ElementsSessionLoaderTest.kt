package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class ElementsSessionLoaderTest {

    @Test
    fun `retrieves elements session from repository`() = runScenario {
        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.elementsSession.stripeIntent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        assertThat(result.customerInfo).isNull()
    }

    @Test
    fun `passes savedPaymentMethodSelection to repository`() = runScenario {
        val savedSelection = SavedSelection.PaymentMethod(id = "pm_123")

        loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = savedSelection,
        )

        assertThat(elementsSessionRepository.lastParams?.savedPaymentMethodSelectionId)
            .isEqualTo("pm_123")
    }

    private data class Scenario(
        val loader: ElementsSessionLoader,
        val elementsSessionRepository: FakeElementsSessionRepository,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
        )
        val loader = ElementsSessionLoader(
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = FakeErrorReporter(),
        )
        Scenario(
            loader = loader,
            elementsSessionRepository = elementsSessionRepository,
        ).block()
    }

    private companion object {
        private val DEFAULT_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()
    }
}
