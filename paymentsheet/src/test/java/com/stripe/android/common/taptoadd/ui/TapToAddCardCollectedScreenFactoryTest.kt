package com.stripe.android.common.taptoadd.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.FakeLinkInlineSignupAvailability
import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddCardCollectedScreenFactoryTest {
    private val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

    @Test
    fun `create returns Confirmation when Complete mode and link signup unavailable`() = runScenario(
        mode = TapToAddMode.Complete,
        availabilityResult = LinkInlineSignupAvailability.Result.Unavailable,
    ) {
        val screen = factory.create(paymentMethod)

        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.Confirmation>()

        val createCall = confirmationFactory.createCalls.awaitItem()

        assertThat(createCall.paymentMethod).isEqualTo(paymentMethod)
        assertThat(createCall.linkInput).isNull()
        assertThat(createCall.withTitle).isTrue()
    }

    @Test
    fun `create returns CardAdded when Complete mode and link signup available`() = runScenario(
        mode = TapToAddMode.Complete,
        availabilityResult = LinkInlineSignupAvailability.Result.Available(TestFactory.LINK_CONFIGURATION),
    ) {
        val screen = factory.create(paymentMethod)

        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.CardAdded>()
        assertThat(cardAddedFactory.createCalls.awaitItem()).isEqualTo(paymentMethod)
        confirmationFactory.createCalls.expectNoEvents()
    }

    @Test
    fun `create returns CardAdded when Continue mode even if link signup unavailable`() = runScenario(
        mode = TapToAddMode.Continue,
        availabilityResult = LinkInlineSignupAvailability.Result.Unavailable,
    ) {
        val screen = factory.create(paymentMethod)

        assertThat(screen).isInstanceOf<TapToAddNavigator.Screen.CardAdded>()
        assertThat(cardAddedFactory.createCalls.awaitItem()).isEqualTo(paymentMethod)
    }

    private fun runScenario(
        mode: TapToAddMode,
        availabilityResult: LinkInlineSignupAvailability.Result,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val cardAddedFactory = FakeTapToAddCardAddedInteractor.Factory()
        val confirmationFactory = FakeTapToAddConfirmationInteractor.Factory()

        val factory = TapToAddCardCollectedScreenFactory(
            tapToAddMode = mode,
            linkInlineSignupAvailability = FakeLinkInlineSignupAvailability(availabilityResult),
            tapToAddCardAddedInteractorFactory = cardAddedFactory,
            tapToAddConfirmationInteractorFactory = confirmationFactory,
        )

        block(
            Scenario(
                factory = factory,
                cardAddedFactory = cardAddedFactory,
                confirmationFactory = confirmationFactory,
            )
        )

        cardAddedFactory.validate()
        confirmationFactory.validate()
    }

    private class Scenario(
        val factory: TapToAddCardCollectedScreenFactory,
        val cardAddedFactory: FakeTapToAddCardAddedInteractor.Factory,
        val confirmationFactory: FakeTapToAddConfirmationInteractor.Factory,
    )
}
