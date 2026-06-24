package com.stripe.android.common.nfcscan.hardware

import android.content.Context
import android.nfc.NfcAdapter
import javax.inject.Inject

internal interface NfcHardwareDelegate {
    fun isAvailable(): Boolean
}

internal class DefaultNfcHardwareDelegate @Inject constructor(
    context: Context
) : NfcHardwareDelegate {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    override fun isAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
}
