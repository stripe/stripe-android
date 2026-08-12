package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpdatedTaxAmountContractTest {
    private val contract = UpdatedTaxAmountContract()

    @Test
    fun `createIntent includes args`() {
        val args = UpdatedTaxAmountContract.Args(
            displayItems = emptyList(),
            currency = "usd",
            appearance = PaymentSheet.Appearance(),
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        val actualArgs = intent.extras?.let { bundle ->
            BundleCompat.getParcelable(
                bundle,
                UpdatedTaxAmountContract.EXTRA_ARGS,
                UpdatedTaxAmountContract.Args::class.java,
            )
        }
        assertThat(actualArgs).isEqualTo(args)
    }

    @Test
    fun `parseResult returns confirmed result`() {
        val intent = Intent().putExtra(
            UpdatedTaxAmountResult.EXTRA_RESULT,
            UpdatedTaxAmountResult.Confirmed,
        )

        assertThat(contract.parseResult(0, intent)).isEqualTo(UpdatedTaxAmountResult.Confirmed)
    }

    @Test
    fun `parseResult without result returns cancelled`() {
        assertThat(contract.parseResult(0, Intent())).isEqualTo(UpdatedTaxAmountResult.Cancelled)
    }
}
