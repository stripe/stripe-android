package com.stripe.android.common.nfcscan.hardware

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcAntennaInfo
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import jakarta.inject.Inject

internal interface NfcHardwareDelegate {
    fun isAvailable(): Boolean

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun antenna(): NfcAntennaInfo?

    fun read(
        activity: AppCompatActivity,
    )
}

internal class DefaultNfcHardwareDelegate @Inject constructor(
    applicationContext: Context
) : NfcHardwareDelegate {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(applicationContext)

    override fun isAvailable(): Boolean {
        return nfcAdapter != null && nfcAdapter.isEnabled
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun antenna(): NfcAntennaInfo? {
        return nfcAdapter?.nfcAntennaInfo
    }

    override fun read(activity: AppCompatActivity) {
        if (nfcAdapter == null || activity.lifecycle.currentState != Lifecycle.State.STARTED) {
            return
        }

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                tag
            },
            SUPPORTED_NFC_TYPES,
            Bundle.EMPTY
        )

        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onPause(owner: LifecycleOwner) {
                    nfcAdapter.disableReaderMode(activity)

                    super.onDestroy(owner)
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
    }
}
