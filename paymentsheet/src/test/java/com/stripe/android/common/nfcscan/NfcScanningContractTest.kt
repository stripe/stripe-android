package com.stripe.android.common.nfcscan

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NfcScanningContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `createIntent attaches Args so merchant name can be read back`() {
        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(merchantName = "River Market")
        )

        val argsFromIntent = NfcScanningContract.Args.fromIntent(intent)

        assertThat(argsFromIntent?.merchantName).isEqualTo("River Market")
    }

    @Test
    fun `parseResult returns Complete when intent carries Complete result`() {
        val complete = NfcScanningContract.Result.Complete(
            cardNumber = "4242424242424242",
            expirationMonth = 3,
            expirationYear = 2031,
        )

        val intent = Intent().apply { putExtras(complete.toBundle()) }

        assertThat(NfcScanningContract.parseResult(Activity.RESULT_OK, intent))
            .isEqualTo(complete)
    }

    @Test
    fun `parseResult returns Canceled when intent is null`() {
        assertThat(NfcScanningContract.parseResult(Activity.RESULT_OK, null))
            .isEqualTo(NfcScanningContract.Result.Canceled)
    }
}
