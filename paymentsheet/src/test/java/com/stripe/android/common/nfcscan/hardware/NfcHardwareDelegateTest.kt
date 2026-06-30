package com.stripe.android.common.nfcscan.hardware

import android.app.Application
import android.content.pm.PackageManager
import android.nfc.AvailableNfcAntenna
import android.nfc.NfcAdapter
import android.nfc.NfcAntennaInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class NfcHardwareDelegateTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

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

    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @Test
    fun `antenna returns null when device has no NFC hardware`() = test(
        hasNfcFeature = false,
    ) {
        assertThat(delegate.antenna()).isNull()
    }

    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @Test
    fun `antenna returns null when NFC adapter has no antenna info`() = test(
        hasNfcFeature = true,
    ) {
        assertThat(delegate.antenna()).isNull()
    }

    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @Test
    fun `antenna returns antenna info from NFC adapter`() = test(
        hasNfcFeature = true,
        nfcAntennaInfo = NfcAntennaInfo(
            1000,
            2000,
            false,
            listOf(AvailableNfcAntenna(300, 400)),
        ),
    ) {
        assertThat(delegate.antenna()).isEqualTo(nfcAntennaInfo)
    }

    private fun test(
        hasNfcFeature: Boolean = true,
        isNfcEnabled: Boolean = true,
        nfcAntennaInfo: NfcAntennaInfo? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        shadowOf(application.packageManager)
            .setSystemFeature(PackageManager.FEATURE_NFC, hasNfcFeature)

        if (hasNfcFeature) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(application)
            shadowOf(nfcAdapter).setEnabled(isNfcEnabled)
            nfcAntennaInfo?.let { shadowOf(nfcAdapter).setNfcAntennaInfo(it) }
        }

        block(
            Scenario(
                delegate = DefaultNfcHardwareDelegate(application),
                nfcAntennaInfo = nfcAntennaInfo,
            )
        )
    }

    private class Scenario(
        val delegate: NfcHardwareDelegate,
        val nfcAntennaInfo: NfcAntennaInfo?,
    )
}
