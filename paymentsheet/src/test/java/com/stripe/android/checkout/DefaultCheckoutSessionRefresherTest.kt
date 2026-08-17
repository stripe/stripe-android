package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultCheckoutSessionRefresherTest {

    @Test
    fun `refresh with response reloads it without fetching`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_confirmed")

        refresher.refresh(response)

        assertThat(refreshActions.reloadCalls.awaitItem()).isEqualTo(response)
    }

    @Test
    fun `refresh with response does nothing without loaded state`() = runScenario(
        stateIsLoaded = false,
    ) {
        refresher.refresh(CheckoutSessionResponseFactory.create(id = "cs_confirmed"))

        refreshActions.reloadCalls.expectNoEvents()
    }

    @Test
    fun `refresh without response fetches and reloads latest session`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_fetched")
        refreshActions.enqueueFetchResponse { Result.success(response) }

        refresher.refresh()

        assertThat(refreshActions.fetchCalls.awaitItem()).isEqualTo(
            FakeCheckoutSessionRefreshActions.FetchCall(
                sessionId = initialState.checkoutSessionResponse.id,
                adaptivePricingAllowed = initialState.configuration.adaptivePricingAllowed,
            )
        )
        assertThat(refreshActions.reloadCalls.awaitItem()).isEqualTo(response)
    }

    private fun runScenario(
        stateIsLoaded: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        val initialState = CheckoutControllerStateFactory.create()
        stateHolder.state = initialState.takeIf { stateIsLoaded }
        val refreshActions = FakeCheckoutSessionRefreshActions()
        val refresher = DefaultCheckoutSessionRefresher(
            stateHolder = stateHolder,
            fetchResponse = refreshActions::fetch,
            reloadResponse = refreshActions::reload,
        )

        Scenario(
            refresher = refresher,
            initialState = initialState,
            refreshActions = refreshActions,
        ).block()

        refreshActions.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val refresher: DefaultCheckoutSessionRefresher,
        val initialState: CheckoutControllerState,
        val refreshActions: FakeCheckoutSessionRefreshActions,
    )
}
