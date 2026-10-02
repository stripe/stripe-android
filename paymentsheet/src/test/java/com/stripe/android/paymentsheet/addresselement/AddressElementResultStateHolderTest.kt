package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementResultStateHolderTest {
    @Test
    fun `first terminal result is retained`() {
        val expectedResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        assertThat(resultStateHolder.setResult(expectedResult)).isTrue()
        assertThat(resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)).isFalse()

        assertThat(resultStateHolder.result.value).isEqualTo(expectedResult)
    }

    @Test
    fun `form starts enabled and follows enabled state changes`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.formEnabled.test {
            assertThat(awaitItem()).isTrue()

            resultStateHolder.setFormEnabled(false)

            assertThat(awaitItem()).isFalse()

            resultStateHolder.setFormEnabled(true)

            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `user cancellation returns canceled when form is enabled`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            assertThat(resultStateHolder.onUserCancel()).isTrue()

            assertThat(awaitItem()).isEqualTo(AddressElementActivityContract.Result.Canceled)

            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `user cancellation is ignored when form is disabled`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()
        resultStateHolder.setFormEnabled(false)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()
        }
    }

    @Test
    fun `user cancellation is allowed after form is enabled again`() = runTest {
        val resultStateHolder = AddressElementResultStateHolder()
        resultStateHolder.setFormEnabled(false)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            assertThat(resultStateHolder.onUserCancel()).isFalse()

            expectNoEvents()

            resultStateHolder.setFormEnabled(true)
            assertThat(resultStateHolder.onUserCancel()).isTrue()

            assertThat(awaitItem()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        }
    }

    @Test
    fun `success result is accepted when form is disabled`() = runTest {
        val expectedResult = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()
        resultStateHolder.setFormEnabled(false)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isNull()

            resultStateHolder.setResult(expectedResult)

            assertThat(awaitItem()).isEqualTo(expectedResult)
        }
    }
}
