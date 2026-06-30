package com.stripe.android.common.nfcscan.hardware

import android.nfc.NfcAntennaInfo

internal class FakeNfcHardwareDelegate(
    private val result: Boolean = true,
    private val antennaInfo: NfcAntennaInfo? = null,
) : NfcHardwareDelegate {
    override fun isAvailable(): Boolean = result

    override fun antenna(): NfcAntennaInfo? = antennaInfo
}
