package com.stripe.android.common.nfcscan.hardware

import android.content.Context
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
import jakarta.inject.Inject
import kotlin.time.Duration

internal interface NfcHardwareDelegate {
    fun isAvailable(): Boolean

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun antenna(): NfcAntennaInfo?

    fun start(
        activity: AppCompatActivity,
        onTagDiscovered: (Tag) -> Unit,
    )

    fun ignore(
        tag: Tag,
        debounce: Duration,
    )
}

internal class DefaultNfcHardwareDelegate @Inject constructor(
    applicationContext: Context
) : NfcHardwareDelegate {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(applicationContext)

    @Volatile private var ignoredTagId: ByteArray? = null
    @Volatile private var ignoreUntilMs: Long = 0L

    override fun isAvailable(): Boolean {
        return nfcAdapter != null && nfcAdapter.isEnabled
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun antenna(): NfcAntennaInfo? {
        return nfcAdapter?.nfcAntennaInfo
    }

    override fun start(activity: AppCompatActivity, onTagDiscovered: (Tag) -> Unit) {
        if (nfcAdapter == null || activity.lifecycle.currentState != Lifecycle.State.STARTED) {
            return
        }

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                if (!isManuallyIgnored(tag)) {
                    onTagDiscovered(tag)
                }
            },
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

    override fun ignore(tag: Tag, debounce: Duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nfcAdapter?.ignore(tag, debounce.inWholeMilliseconds.toInt(), null, null)
        } else {
            ignoredTagId = tag.id
            ignoreUntilMs = System.currentTimeMillis() + debounce.inWholeMilliseconds
        }
    }

    private fun isManuallyIgnored(tag: Tag): Boolean {
        val ignored = ignoredTagId ?: return false
        return ignored.contentEquals(tag.id) && System.currentTimeMillis() < ignoreUntilMs
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
