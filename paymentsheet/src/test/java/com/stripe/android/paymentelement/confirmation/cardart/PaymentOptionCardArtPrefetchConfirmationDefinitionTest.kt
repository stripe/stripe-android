package com.stripe.android.paymentelement.confirmation.cardart

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import com.stripe.android.paymentsheet.PaymentOptionCardArtProvider
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentOptionCardArtPrefetchConfirmationDefinitionTest {

    @Test
    fun `key is CardArtPrefetch`() = runScenario {
        assertThat(definition.key).isEqualTo("CardArtPrefetch")
    }

    @Test
    fun `option always returns null`() = runScenario {
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `canConfirm always returns false`() = runScenario {
        val result = definition.canConfirm(
            confirmationOption = FakeConfirmationOption(),
            confirmationArgs = confirmationArgs,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `action reports error and returns Fail`() = runScenario {
        val result = definition.action(
            confirmationOption = FakeConfirmationOption(),
            confirmationArgs = confirmationArgs,
        )

        val call = fakeErrorReporter.awaitCall()
        assertThat(call.errorEvent)
            .isEqualTo(UnexpectedErrorEvent.CARD_ART_PREFETCH_INVOKED_FOR_CONFIRMATION)

        assertThat(result).isInstanceOf(ConfirmationDefinition.Action.Fail::class.java)
        val fail = result as ConfirmationDefinition.Action.Fail
        assertThat(fail.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(fail.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `bootstrap does nothing when cardArts is empty`() = runScenario {
        val metadata = PaymentMethodMetadataFactory.create(cardArts = emptyList())
        definition.bootstrap(metadata)
    }

    @Test
    fun `bootstrap loads images for each card art`() = runScenario {
        val metadata = PaymentMethodMetadataFactory.create(
            cardArts = listOf(
                cardArt(url = "https://example.com/art1.png"),
                cardArt(url = "https://example.com/art2.png"),
            ),
        )

        definition.bootstrap(metadata)

        assertThat(fakeImageLoader.awaitLoadCall().url).isEqualTo(OPTIMIZED_URL)
        assertThat(fakeImageLoader.awaitLoadCall().url).isEqualTo(OPTIMIZED_URL)
    }

    @Test
    fun `bootstrap skips card arts when provider returns null`() = runScenario(
        cardArtProviderResult = null,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            cardArts = listOf(cardArt(url = "https://example.com/art1.png")),
        )

        definition.bootstrap(metadata)
    }

    private fun cardArt(url: String) = PaymentMethod.Card.CardArt(
        artImage = PaymentMethod.Card.CardArt.ArtImage(format = "image/png", url = url),
        programName = null,
    )

    private fun runScenario(
        cardArtProviderResult: String? = OPTIMIZED_URL,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val fakeErrorReporter = FakeErrorReporter()
        val fakeImageLoader = FakeStripeImageLoader()
        val fakeCardArtProvider = FakePaymentOptionCardArtProvider(cardArtProviderResult)

        val definition = PaymentOptionCardArtPrefetchConfirmationDefinition(
            imageLoader = fakeImageLoader,
            coroutineScope = this,
            paymentOptionCardArtProvider = fakeCardArtProvider,
            workContext = testDispatcher,
            errorReporter = fakeErrorReporter,
        )

        Scenario(
            definition = definition,
            fakeErrorReporter = fakeErrorReporter,
            fakeImageLoader = fakeImageLoader,
            confirmationArgs = ConfirmationHandler.Args(
                confirmationOption = FakeConfirmationOption(),
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            ),
        ).apply { block() }

        fakeErrorReporter.ensureAllEventsConsumed()
        fakeImageLoader.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val definition: PaymentOptionCardArtPrefetchConfirmationDefinition,
        val fakeErrorReporter: FakeErrorReporter,
        val fakeImageLoader: FakeStripeImageLoader,
        val confirmationArgs: ConfirmationHandler.Args,
    )

    private class FakePaymentOptionCardArtProvider(
        private val result: String?,
    ) : PaymentOptionCardArtProvider {
        override fun invoke(cardArt: PaymentMethod.Card.CardArt): String? = result
    }

    private companion object {
        const val OPTIMIZED_URL = "https://example.com/optimized.png"
    }
}
