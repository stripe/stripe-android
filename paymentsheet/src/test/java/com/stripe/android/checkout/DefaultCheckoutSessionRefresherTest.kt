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
    fun `refresh with response commits it without fetching`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_confirmed")

        refresher.refresh(response)

        assertThat(refreshActions.reloadCalls.awaitItem())
            .isEqualTo(initialState.copy(checkoutSessionResponse = response))
    }

    @Test
    fun `refresh without response fetches and commits latest session`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(id = "cs_fetched")
        val stateAtCommit = initialState.copy(temporarySelection = "card")
        refreshActions.enqueueFetchResponse {
            stateHolder.state = stateAtCommit
            Result.success(response)
        }

        refresher.refresh()

        assertThat(refreshActions.fetchCalls.awaitItem()).isEqualTo(
            FakeCheckoutSessionRefreshActions.FetchCall(
                sessionId = initialState.checkoutSessionResponse.id,
                adaptivePricingAllowed = false,
            )
        )
        assertThat(refreshActions.reloadCalls.awaitItem())
            .isEqualTo(stateAtCommit.copy(checkoutSessionResponse = response))
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        val initialState = CheckoutControllerStateFactory.create()
        stateHolder.state = initialState
        val refreshActions = FakeCheckoutSessionRefreshActions()
        val refresher = DefaultCheckoutSessionRefresher(
            stateHolder = stateHolder,
            fetchResponse = refreshActions::fetch,
            reloadState = refreshActions::reload,
        )

        Scenario(
            refresher = refresher,
            stateHolder = stateHolder,
            initialState = initialState,
            refreshActions = refreshActions,
        ).block()

        refreshActions.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val refresher: DefaultCheckoutSessionRefresher,
        val stateHolder: CheckoutControllerStateHolder,
        val initialState: CheckoutControllerState,
        val refreshActions: FakeCheckoutSessionRefreshActions,
    )
}
