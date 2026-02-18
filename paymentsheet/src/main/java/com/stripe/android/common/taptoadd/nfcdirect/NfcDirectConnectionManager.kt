package com.stripe.android.common.taptoadd.nfcdirect

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * NFC connection manager that uses Android's NFC Reader Mode API directly.
 *
 * This provides a lightweight alternative to the Stripe Terminal SDK for
 * Card Not Present (CNP) card data reading scenarios like "Tap to Add".
 *
 * Key features:
 * - Uses Android's NfcAdapter.enableReaderMode() for exclusive NFC access
 * - Supports IsoDep (ISO 14443-4) for EMV contactless communication
 * - Configures appropriate timeouts for card communication
 *
 * Note: Requires Activity to be set via [NfcDirectActivityHolder.set] before
 * calling [connect]. This is necessary because NFC Reader Mode requires an
 * Activity reference.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class NfcDirectConnectionManager(
    context: Context,
    private val workContext: CoroutineContext,
) : TapToAddConnectionManager, NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private val workScope = CoroutineScope(workContext)

    private var tagDeferred: CompletableDeferred<IsoDep>? = null
    private var connectionTask: CompletableDeferred<Boolean>? = null
    private var currentIsoDep: IsoDep? = null
    private var currentActivity: Activity? = null

    override val isSupported: Boolean
        get() = nfcAdapter?.isEnabled == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    override val isConnected: Boolean
        get() = currentIsoDep?.isConnected == true

    /**
     * Start listening for NFC card taps.
     *
     * Enables Reader Mode which gives us exclusive access to the NFC hardware
     * and prevents the system from dispatching NFC intents to other apps.
     *
     * Note: [NfcDirectActivityHolder.set] must be called before this method.
     */
    override fun connect() {
        // Initialize deferred objects synchronously to avoid race conditions
        // where awaitConnection() or awaitTag() is called before the coroutine runs
        synchronized(this) {
            if (!isSupported || connectionTask?.isActive == true) {
                return
            }
            tagDeferred = CompletableDeferred()
            connectionTask = CompletableDeferred()
        }

        workScope.launch {
            val activity = NfcDirectActivityHolder.get()
            if (activity == null) {
                connectionTask?.completeExceptionally(
                    IllegalStateException(
                        "Activity not available. Call NfcDirectActivityHolder.set() first."
                    )
                )
                return@launch
            }

            currentActivity = activity

            try {
                enableReaderMode(activity)
            } catch (e: Exception) {
                connectionTask?.completeExceptionally(e)
            }
        }
    }

    override suspend fun awaitConnection(): Result<Boolean> {
        return runCatching {
            isConnected || connectionTask?.await() ?: false
        }
    }

    /**
     * Called by NfcAdapter when a tag is discovered.
     *
     * We check for IsoDep technology (ISO 14443-4) which is required for
     * EMV contactless transactions.
     */
    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            // Not an ISO-DEP tag, ignore
            return
        }

        workScope.launch {
            try {
                isoDep.connect()
                // Set extended timeout for slow cards (some contactless cards need this)
                isoDep.timeout = ISODEP_TIMEOUT_MS

                currentIsoDep = isoDep
                tagDeferred?.complete(isoDep)
                connectionTask?.complete(true)
            } catch (e: Exception) {
                tagDeferred?.completeExceptionally(e)
                connectionTask?.completeExceptionally(e)
            }
        }
    }

    /**
     * Wait for a card to be tapped and return the IsoDep interface for communication.
     */
    suspend fun awaitTag(): IsoDep {
        return tagDeferred?.await()
            ?: throw IllegalStateException("connect() must be called before awaitTag()")
    }

    /**
     * Get the currently connected IsoDep interface.
     */
    fun getCurrentIsoDep(): IsoDep? = currentIsoDep

    /**
     * Disable NFC reader mode and clean up.
     */
    fun disconnect() {
        try {
            currentIsoDep?.close()
        } catch (_: Exception) {
            // Ignore close errors
        }
        currentIsoDep = null

        currentActivity?.let { activity ->
            try {
                nfcAdapter?.disableReaderMode(activity)
            } catch (_: Exception) {
                // Ignore disable errors (activity may be finishing)
            }
        }
        currentActivity = null

        // Reset deferred objects for next connection
        tagDeferred = null
        connectionTask = null
    }

    /**
     * Reset for a new card tap (after successful or failed read).
     */
    fun resetForNextTap() {
        disconnect()
        synchronized(this) {
            tagDeferred = CompletableDeferred()
            connectionTask = CompletableDeferred()
        }
        workScope.launch {
            NfcDirectActivityHolder.get()?.let { enableReaderMode(it) }
        }
    }

    private fun enableReaderMode(activity: Activity) {
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        val options = Bundle().apply {
            // Disable presence check to improve performance
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
        }

        nfcAdapter?.enableReaderMode(activity, this, flags, options)
    }

    companion object {
        // Timeout for ISO-DEP communication (milliseconds)
        // Some contactless cards need extended timeout
        private const val ISODEP_TIMEOUT_MS = 5000

        // Delay between presence checks (milliseconds)
        // Longer delay reduces overhead when card is held against device
        private const val PRESENCE_CHECK_DELAY_MS = 500
    }
}
