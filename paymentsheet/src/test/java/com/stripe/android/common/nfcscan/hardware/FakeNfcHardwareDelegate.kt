package com.stripe.android.common.nfcscan.hardware

import android.nfc.NfcAntennaInfo
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.Turbine

internal class FakeNfcHardwareDelegate(
    private val result: Boolean = true,
    private val antennaInfo: NfcAntennaInfo? = null,
) : NfcHardwareDelegate {
    val startCalls = Turbine<StartCall>()

    override fun isAvailable(): Boolean = result

    override fun antenna(): NfcAntennaInfo? = antennaInfo

    override fun start(
        activity: AppCompatActivity,
        onTagDiscovered: (Tag) -> Unit,
    ) {
        startCalls.add(StartCall(activity, onTagDiscovered))
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }

    data class StartCall(
        val activity: AppCompatActivity,
        val onTagDiscovered: (Tag) -> Unit,
    )
}
