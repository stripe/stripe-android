package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
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
        val response = CheckoutSessionResponseFactory.create(
            id = "cs_confirmed",
            status = CheckoutSessionResponse.Status.COMPLETE,
        )

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

        assertThat(refresher.refresh()).isSameInstanceAs(response)

        assertThat(refreshActions.fetchCalls.awaitItem()).isEqualTo(
            FakeCheckoutSessionRefreshActions.FetchCall(
                clientSecret = initialState.checkoutSessionResponse.clientSecret,
            )
        )
        assertThat(refreshActions.reloadCalls.awaitItem())
            .isEqualTo(stateAtCommit.copy(checkoutSessionResponse = response))
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = CheckoutElementsControllerStateFactory.createStateHolder(SavedStateHandle())
        val initialState = CheckoutElementsControllerStateFactory.create()
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
        val stateHolder: CheckoutElementsControllerStateHolder,
        val initialState: CheckoutElementsControllerState,
        val refreshActions: FakeCheckoutSessionRefreshActions,
    )
}
