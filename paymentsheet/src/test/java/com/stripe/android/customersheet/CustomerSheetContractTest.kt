package com.stripe.android.customersheet

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerSheetContractTest {

    @Test
    fun `parseResult returns null when canceled without a result`() {
        val contract = CustomerSheetContract()

        val result = contract.parseResult(Activity.RESULT_CANCELED, null)

        assertThat(result).isNull()
    }

    @Test
    fun `parseResult returns null when the intent has no result extra`() {
        val contract = CustomerSheetContract()

        val result = contract.parseResult(Activity.RESULT_OK, Intent())

        assertThat(result).isNull()
    }

    @Test
    fun `parseResult parses a canceled result from the intent`() {
        val contract = CustomerSheetContract()
        val expected = InternalCustomerSheetResult.Canceled(paymentSelection = null)
        val intent = Intent().putExtras(expected.toBundle())

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `parseResult parses a selected result from the intent`() {
        val contract = CustomerSheetContract()
        val expected = InternalCustomerSheetResult.Selected(paymentSelection = null)
        val intent = Intent().putExtras(expected.toBundle())

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result).isEqualTo(expected)
    }
}
