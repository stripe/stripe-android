package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class AddressElementResultStateHolderTest {
    @Test
    fun `success result is sticky for a late collector`() = runTest {
        val expectedResult = AddressLauncherResult.Succeeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.setResult(expectedResult)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isEqualTo(expectedResult)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `canceled result is sticky for a late collector`() = runTest {
        val expectedResult = AddressLauncherResult.Canceled()
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.setResult(expectedResult)

        resultStateHolder.result.test {
            assertThat(awaitItem()).isEqualTo(expectedResult)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `first terminal result is retained`() {
        val expectedResult = AddressLauncherResult.Succeeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.setResult(expectedResult)
        resultStateHolder.setResult(AddressLauncherResult.Canceled())

        assertThat(resultStateHolder.result.value).isEqualTo(expectedResult)
    }
}
