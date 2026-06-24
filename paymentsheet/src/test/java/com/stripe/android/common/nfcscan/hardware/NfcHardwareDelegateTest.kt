package com.stripe.android.common.nfcscan.hardware

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class NfcHardwareDelegateTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `isAvailable returns false when device has no NFC hardware`() = test(
        hasNfcFeature = false,
    ) {
        assertThat(delegate.isAvailable()).isFalse()
    }

    @Test
    fun `isAvailable returns false when NFC adapter is available but disabled`() = test(
        hasNfcFeature = true,
        isNfcEnabled = false,
    ) {
        assertThat(delegate.isAvailable()).isFalse()
    }

    @Test
    fun `isAvailable returns true when NFC adapter is available & enabled`() = test(
        hasNfcFeature = true,
        isNfcEnabled = true,
    ) {
        assertThat(delegate.isAvailable()).isTrue()
    }

    private fun test(
        hasNfcFeature: Boolean = true,
        isNfcEnabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_NFC, hasNfcFeature)

        if (hasNfcFeature) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            shadowOf(nfcAdapter).setEnabled(isNfcEnabled)
        }

        block(Scenario(DefaultNfcHardwareDelegate(context)))
    }

    private class Scenario(
        val delegate: NfcHardwareDelegate,
    )
}
