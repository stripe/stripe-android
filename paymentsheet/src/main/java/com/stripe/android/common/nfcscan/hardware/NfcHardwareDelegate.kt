package com.stripe.android.common.nfcscan.hardware

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.NfcAntennaInfo
import android.os.Build
import androidx.annotation.RequiresApi
import javax.inject.Inject

internal interface NfcHardwareDelegate {
    fun isAvailable(): Boolean

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun antenna(): NfcAntennaInfo?
}

internal class DefaultNfcHardwareDelegate @Inject constructor(
    application: Application
) : NfcHardwareDelegate {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(application)

    override fun isAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun antenna(): NfcAntennaInfo? {
        return nfcAdapter?.nfcAntennaInfo
    }
}
