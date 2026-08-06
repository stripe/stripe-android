package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationStateUpdaterTest {

    @Test
    fun `provided response reloads state without fetching`() = runScenario {
        val response = response(id = "cs_confirmed")

        updater.update(response)

        assertThat(reloadCalls.awaitItem().checkoutSessionResponse).isEqualTo(response)
        fetchCalls.expectNoEvents()
    }

    @Test
    fun `missing response fetches session and reloads state`() {
        val response = response(id = "cs_refreshed")

        runScenario(fetchResult = Result.success(response)) {
            updater.update(checkoutSessionResponse = null)

            val initialState = requireNotNull(initialState)
            assertThat(fetchCalls.awaitItem()).isEqualTo(
                FetchCall(
                    sessionId = initialState.checkoutSessionResponse.id,
                    adaptivePricingAllowed = initialState.configuration.adaptivePricingAllowed,
                )
            )
            assertThat(reloadCalls.awaitItem().checkoutSessionResponse).isEqualTo(response)
        }
    }

    @Test
    fun `failed refresh leaves state unchanged`() {
        val error = IllegalStateException("Failed to refresh")

        runScenario(fetchResult = Result.failure(error)) {
            updater.update(checkoutSessionResponse = null)

            val initialState = requireNotNull(initialState)
            assertThat(fetchCalls.awaitItem().sessionId).isEqualTo(initialState.checkoutSessionResponse.id)
            reloadCalls.expectNoEvents()
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `provided response swallows reload failure and leaves state unchanged`() {
        val error = IllegalStateException("Failed to reload")

        runScenario(reloadError = error) {
            val initialState = requireNotNull(initialState)

            updater.update(response(id = "cs_confirmed"))

            // Reload was attempted with the confirmed response but threw; update must swallow it so a
            // completed payment is never turned into a failed result.
            assertThat(reloadCalls.awaitItem().checkoutSessionResponse.id).isEqualTo("cs_confirmed")
            fetchCalls.expectNoEvents()
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `fetched response swallows reload failure and leaves state unchanged`() {
        val fetchedResponse = response(id = "cs_refreshed")
        val error = IllegalStateException("Failed to reload")

        runScenario(fetchResult = Result.success(fetchedResponse), reloadError = error) {
            val initialState = requireNotNull(initialState)

            updater.update(checkoutSessionResponse = null)

            assertThat(fetchCalls.awaitItem().sessionId).isEqualTo(initialState.checkoutSessionResponse.id)
            assertThat(reloadCalls.awaitItem().checkoutSessionResponse).isEqualTo(fetchedResponse)
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `missing state does not fetch or reload`() = runScenario(state = null) {
        updater.update(response(id = "cs_confirmed"))

        fetchCalls.expectNoEvents()
        reloadCalls.expectNoEvents()
    }

    private fun runScenario(
        state: CheckoutControllerState? = CheckoutControllerStateFactory.create(),
        fetchResult: Result<CheckoutSessionResponse> = Result.failure(
            IllegalStateException("Unexpected fetch")
        ),
        reloadError: Throwable? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fetchCalls = Turbine<FetchCall>()
        val reloadCalls = Turbine<CheckoutControllerState>()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        stateHolder.state = state
        val updater = CheckoutConfirmationStateUpdater(
            stateHolder = stateHolder,
            fetchResponse = { sessionId, adaptivePricingAllowed ->
                fetchCalls.add(FetchCall(sessionId, adaptivePricingAllowed))
                fetchResult
            },
            reloadState = { updatedState ->
                reloadCalls.add(updatedState)
                reloadError?.let { throw it }
                stateHolder.state = updatedState
            },
        )

        Scenario(
            updater = updater,
            initialState = state,
            stateHolder = stateHolder,
            fetchCalls = fetchCalls,
            reloadCalls = reloadCalls,
        ).block()

        fetchCalls.ensureAllEventsConsumed()
        reloadCalls.ensureAllEventsConsumed()
    }

    private fun response(id: String): CheckoutSessionResponse {
        return CheckoutSessionResponseFactory.create(id = id)
    }

    private class Scenario(
        val updater: CheckoutConfirmationStateUpdater,
        val initialState: CheckoutControllerState?,
        val stateHolder: CheckoutControllerStateHolder,
        val fetchCalls: Turbine<FetchCall>,
        val reloadCalls: Turbine<CheckoutControllerState>,
    )

    private data class FetchCall(
        val sessionId: String,
        val adaptivePricingAllowed: Boolean,
    )
}
