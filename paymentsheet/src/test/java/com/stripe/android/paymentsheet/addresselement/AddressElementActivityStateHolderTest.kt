package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementActivityStateHolderTest {
    @Test
    fun `processing rejects duplicate saves`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)

            assertThat(stateHolder.tryStartProcessing()).isTrue()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Processing)

            assertThat(stateHolder.tryStartProcessing()).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `processing failure returns to idle`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
            stateHolder.tryStartProcessing()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Processing)

            assertThat(stateHolder.finishProcessing()).isTrue()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
        }
    }

    @Test
    fun `cancellation transitions from idle`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)

            assertThat(stateHolder.tryCancel()).isTrue()
            assertThat(awaitItem()).isEqualTo(
                AddressElementActivityStateHolder.State.Completed(
                    AddressElementActivityContract.Result.Canceled,
                )
            )
        }
    }

    @Test
    fun `cancellation is rejected while processing without emitting state`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
            stateHolder.tryStartProcessing()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Processing)

            assertThat(stateHolder.tryCancel()).isFalse()
            assertThat(stateHolder.state.value).isEqualTo(AddressElementActivityStateHolder.State.Processing)
            expectNoEvents()
        }
    }

    @Test
    fun `checkout completion transitions from processing`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            updatedResponse = CheckoutSessionResponseFactory.create(),
        )

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
            stateHolder.tryStartProcessing()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Processing)

            assertThat(stateHolder.complete(result)).isTrue()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Completed(result))
        }
    }

    @Test
    fun `standalone completion transitions from idle`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)

            assertThat(stateHolder.complete(result)).isTrue()
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Completed(result))
        }
    }

    @Test
    fun `first completed result wins`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()
        val firstResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        val secondResult = AddressElementActivityContract.Result.StandaloneSucceeded(
            AddressDetails(name = "Second"),
        )

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
            stateHolder.complete(firstResult)
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Completed(firstResult))

            assertThat(stateHolder.complete(secondResult)).isFalse()
            expectNoEvents()
            assertThat(stateHolder.state.value)
                .isEqualTo(AddressElementActivityStateHolder.State.Completed(firstResult))
        }
    }

    @Test
    fun `cancellation is rejected after completion without emitting state`() = runTest {
        val stateHolder = AddressElementActivityStateHolder()
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        stateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Idle)
            stateHolder.complete(result)
            assertThat(awaitItem()).isEqualTo(AddressElementActivityStateHolder.State.Completed(result))

            assertThat(stateHolder.tryCancel()).isFalse()
            assertThat(stateHolder.state.value)
                .isEqualTo(AddressElementActivityStateHolder.State.Completed(result))
            expectNoEvents()
        }
    }
}
