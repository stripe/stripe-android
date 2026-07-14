package com.stripe.android.common.nfcscan.hardware

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.NfcAntennaInfo
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

internal interface NfcHardwareDelegate {
    fun isAvailable(): Boolean

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun antenna(): NfcAntennaInfo?

    fun start(
        activity: AppCompatActivity,
        onTagDiscovered: (Tag) -> Unit,
    )
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

    override fun start(
        activity: AppCompatActivity,
        onTagDiscovered: (Tag) -> Unit,
    ) {
        if (nfcAdapter == null || activity.lifecycle.currentState != Lifecycle.State.STARTED) {
            return
        }

        nfcAdapter.enableReaderMode(
            activity,
            onTagDiscovered,
            SUPPORTED_NFC_TYPES,
            Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
            }
        )

        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onPause(owner: LifecycleOwner) {
                    nfcAdapter.disableReaderMode(activity)
                    super.onPause(owner)
                }
            }
        )
    }

    private companion object {
        const val SUPPORTED_NFC_TYPES =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        const val PRESENCE_CHECK_DELAY_MS = 500
    }
}
