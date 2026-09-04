package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AddressElementResultStateHolderTest {
    @Test
    fun `first terminal result is retained`() {
        val expectedResult = AddressLauncherResult.Succeeded(AddressDetails())
        val resultStateHolder = AddressElementResultStateHolder()

        resultStateHolder.setResult(expectedResult)
        resultStateHolder.setResult(AddressLauncherResult.Canceled())

        assertThat(resultStateHolder.result.value).isEqualTo(expectedResult)
    }
}
