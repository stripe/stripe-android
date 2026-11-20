@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.link

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LinkTotalUpdaterTest {

    @Test
    fun `sync updates the amount and reports a change`() = runScenario(
        respond = { _, _ -> Result.success(sessionWithAmount(1100L)) },
    ) {
        val changed = updater.sync(billingAddress = usAddress, showsProgress = true)

        assertThat(changed.getOrThrow()).isTrue()
        assertThat(updater.state.value.amount).isEqualTo(1100L)
        assertThat(updater.state.value.isUpdating).isFalse()
        assertThat(updateCalls.awaitItem().address.country).isEqualTo("US")
    }

    @Test
    fun `sync with the same address twice only updates once`() = runScenario(
        respond = { _, _ -> Result.success(sessionWithAmount(1100L)) },
    ) {
        updater.sync(billingAddress = usAddress, showsProgress = true)
        val changed = updater.sync(billingAddress = usAddress, showsProgress = true)

        assertThat(changed.getOrThrow()).isFalse()
        updateCalls.awaitItem()
    }

    @Test
    fun `sync without a country does nothing`() = runScenario {
        val changed = updater.sync(billingAddress = Address(postalCode = "98101"), showsProgress = true)

        assertThat(changed.getOrThrow()).isFalse()
        assertThat(updater.state.value.amount).isEqualTo(1000L)
    }

    @Test
    fun `sync does nothing when the total doesn't depend on the billing address`() = runScenario(
        initialResponse = null,
    ) {
        val changed = updater.sync(billingAddress = usAddress, showsProgress = true)

        assertThat(changed.getOrThrow()).isFalse()
        assertThat(updater.state.value.amount).isNull()
    }

    @Test
    fun `concurrent syncs run in order and apply the latest result`() {
        val usResult = CompletableDeferred<Result<CheckoutSessionResponse>>()
        runScenario(
            respond = { _, address ->
                if (address.country == "US") usResult.await() else Result.success(sessionWithAmount(1200L))
            },
        ) {
            val first = async { updater.sync(billingAddress = usAddress, showsProgress = true) }
            val second = async { updater.sync(billingAddress = caAddress, showsProgress = true) }
            testScope.runCurrent()

            assertThat(updateCalls.awaitItem().address.country).isEqualTo("US")
            updateCalls.expectNoEvents()
            assertThat(updater.state.value.isUpdating).isTrue()

            usResult.complete(Result.success(sessionWithAmount(1100L)))
            first.await()
            second.await()

            assertThat(updateCalls.awaitItem().address.country).isEqualTo("CA")
            assertThat(updater.state.value.amount).isEqualTo(1200L)
            assertThat(updater.state.value.isUpdating).isFalse()
        }
    }

    @Test
    fun `failed sync keeps the amount and allows retrying`() {
        val results = ArrayDeque(
            listOf(
                Result.failure(IllegalStateException("Failed")),
                Result.success(sessionWithAmount(1100L)),
            )
        )
        runScenario(respond = { _, _ -> results.removeFirst() }) {
            val failed = updater.sync(billingAddress = usAddress, showsProgress = true)

            assertThat(failed.isFailure).isTrue()
            assertThat(updater.state.value.amount).isEqualTo(1000L)
            assertThat(updater.state.value.isUpdating).isFalse()
            updateCalls.awaitItem()

            val retried = updater.sync(billingAddress = usAddress, showsProgress = true)

            assertThat(retried.getOrThrow()).isTrue()
            assertThat(updater.state.value.amount).isEqualTo(1100L)
            updateCalls.awaitItem()
        }
    }

    @Test
    fun `sync without progress doesn't report updating`() = runScenario(
        respond = { _, _ -> Result.success(sessionWithAmount(1000L)) },
    ) {
        updater.state.test {
            assertThat(awaitItem().isUpdating).isFalse()

            val changed = updater.sync(billingAddress = usAddress, showsProgress = false)

            assertThat(changed.getOrThrow()).isFalse()
            expectNoEvents()
        }
        updateCalls.awaitItem()
    }

    private fun runScenario(
        initialResponse: CheckoutSessionResponse? = sessionWithAmount(1000L),
        respond: suspend (CheckoutSessionResponse, CheckoutElementsController.Address.State) ->
        Result<CheckoutSessionResponse> = { response, _ -> Result.success(response) },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val updateCalls = Turbine<UpdateCall>()
        val updater = LinkTotalUpdater(
            initialCheckoutSessionResponse = initialResponse,
            updateTaxRegion = { response, address ->
                updateCalls.add(UpdateCall(response, address))
                respond(response, address)
            },
        )

        Scenario(
            updater = updater,
            updateCalls = updateCalls,
            testScope = this,
        ).block()

        updateCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val updater: LinkTotalUpdater,
        val updateCalls: Turbine<UpdateCall>,
        val testScope: TestScope,
    ) {
        fun <T> async(block: suspend () -> T) = testScope.async { block() }
    }

    private data class UpdateCall(
        val response: CheckoutSessionResponse,
        val address: CheckoutElementsController.Address.State,
    )

    private companion object {
        val usAddress = Address(
            line1 = "1 Main St",
            city = "Seattle",
            state = "WA",
            postalCode = "98101",
            country = "US",
        )
        val caAddress = Address(postalCode = "M5V 3L9", country = "CA")

        fun sessionWithAmount(amount: Long): CheckoutSessionResponse {
            return CheckoutSessionResponseFactory.create(
                amount = amount,
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
        }
    }
}
