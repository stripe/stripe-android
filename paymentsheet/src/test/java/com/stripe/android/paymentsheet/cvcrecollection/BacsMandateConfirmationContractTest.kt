package com.stripe.android.paymentsheet.cvcrecollection

import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract.Args
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BacsMandateConfirmationContractTest {

    private val contract = BacsMandateConfirmationContract()

    @Test
    fun testCreateIntent() {
        val input = Args(
            email = "",
            nameOnAccount = "",
            accountNumber = "",
            appearance = PaymentSheet.Appearance(),
            sortCode = ""
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), input)

        assertThat(intent).isNotNull()
        assertThat(input).isEqualTo(
            intent.extras?.let {
                BundleCompat.getParcelable(
                    it,
                    BacsMandateConfirmationContract.EXTRA_ARGS,
                    Args::class.java
                )
            }
        )
    }

    @Test
    fun testParseResultSuccessForConfirmedResult() {
        val resultIntent = Intent().apply {
            putExtra(BacsMandateConfirmationResult.EXTRA_RESULT, BacsMandateConfirmationResult.Confirmed)
        }

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(BacsMandateConfirmationResult.Confirmed)
    }

    @Test
    fun testParseResultSuccessForCancelledResult() {
        val resultIntent = Intent().apply {
            putExtra(BacsMandateConfirmationResult.EXTRA_RESULT, BacsMandateConfirmationResult.Cancelled)
        }

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(BacsMandateConfirmationResult.Cancelled)
    }

    @Test
    fun testParseResultSuccessForModifyDetailsResult() {
        val resultIntent = Intent().apply {
            putExtra(BacsMandateConfirmationResult.EXTRA_RESULT, BacsMandateConfirmationResult.ModifyDetails)
        }

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(BacsMandateConfirmationResult.ModifyDetails)
    }

    @Test
    fun testParseResultFailure() {
        val resultIntent = Intent()

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(BacsMandateConfirmationResult.Cancelled)
    }
}
