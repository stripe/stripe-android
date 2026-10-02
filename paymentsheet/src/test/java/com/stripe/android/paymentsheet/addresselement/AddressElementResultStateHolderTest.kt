package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementResultStateHolderTest {
    @Test
    fun `starts idle`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
        }
    }

    @Test
    fun `starting a save prevents another save`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)

            assertThat(resultStateHolder.tryStartSaving()).isTrue()

            assertThat(awaitItem()).isEqualTo(State.Saving)
            assertThat(resultStateHolder.tryStartSaving()).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `user cancellation is rejected during save`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)

            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `failed save returns to idle and allows retry`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)

            resultStateHolder.onSaveFailed()

            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)
        }
    }

    @Test
    fun `save completion finishes with its result`() = runTest {
        val expectedResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)

            assertThat(resultStateHolder.onSaveCompleted(expectedResult)).isTrue()

            assertThat(awaitItem()).isEqualTo(State.Finished(expectedResult))
        }
    }

    @Test
    fun `successful result cannot be overwritten reset or restarted`() = runTest {
        val expectedResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)
            assertThat(resultStateHolder.onSaveCompleted(expectedResult)).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Finished(expectedResult))

            assertThat(resultStateHolder.onSaveCompleted(AddressElementActivityContract.Result.Canceled)).isFalse()
            resultStateHolder.onSaveFailed()
            assertThat(resultStateHolder.tryStartSaving()).isFalse()
            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `save completion is rejected without a pending save`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)

            assertThat(resultStateHolder.onSaveCompleted(result)).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `user cancellation finishes with canceled result when idle`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)

            assertThat(resultStateHolder.onUserCancel()).isTrue()

            assertThat(awaitItem()).isEqualTo(State.Finished(AddressElementActivityContract.Result.Canceled))
        }
    }

    @Test
    fun `canceled result cannot be overwritten reset or restarted`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.onUserCancel()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Finished(AddressElementActivityContract.Result.Canceled))

            assertThat(resultStateHolder.onSaveCompleted(result)).isFalse()
            resultStateHolder.onSaveFailed()
            assertThat(resultStateHolder.tryStartSaving()).isFalse()
            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `user cancellation is allowed after save fails`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.state.test {
            assertThat(awaitItem()).isEqualTo(State.Idle)
            assertThat(resultStateHolder.tryStartSaving()).isTrue()
            assertThat(awaitItem()).isEqualTo(State.Saving)
            resultStateHolder.onSaveFailed()
            assertThat(awaitItem()).isEqualTo(State.Idle)

            assertThat(resultStateHolder.onUserCancel()).isTrue()

            assertThat(awaitItem()).isEqualTo(State.Finished(AddressElementActivityContract.Result.Canceled))
        }
    }
}
