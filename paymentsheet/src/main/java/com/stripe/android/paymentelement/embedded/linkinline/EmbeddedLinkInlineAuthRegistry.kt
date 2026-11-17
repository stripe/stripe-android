package com.stripe.android.paymentelement.embedded.linkinline

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.IneligibleReason
import java.lang.ref.WeakReference

/**
 * Remembers inline Link verification across Embedded Payment Element instances for the rest of the process: which
 * emails dismissed the card, when a code was last sent automatically, and which element shows the card.
 */
internal class EmbeddedLinkInlineAuthRegistry(
    private val now: () -> Long,
) {
    private val dismissedEmails = mutableSetOf<String>()
    private val autoSendTimes = mutableMapOf<String, Long>()
    private var owner: WeakReference<Any>? = null

    /**
     * Why [owner] can't show the card for [email], or null if it can.
     */
    @Synchronized
    fun ineligibleReason(email: String, owner: Any): IneligibleReason? {
        val key = email.lowercase()
        val currentOwner = this.owner?.get()
        val lastAutoSend = autoSendTimes[key]
        return when {
            key in dismissedEmails -> IneligibleReason.DismissedThisSession
            currentOwner != null && currentOwner !== owner -> IneligibleReason.ShownElsewhere
            lastAutoSend != null && now() - lastAutoSend < AUTO_SEND_COOLDOWN_MILLIS ->
                IneligibleReason.CodeRecentlySent
            else -> null
        }
    }

    /**
     * Marks [owner] as the element showing the card, which makes every other element ineligible.
     */
    @Synchronized
    fun claim(owner: Any) {
        this.owner = WeakReference(owner)
    }

    @Synchronized
    fun release(owner: Any) {
        if (this.owner?.get() === owner) {
            this.owner = null
        }
    }

    @Synchronized
    fun recordDismissal(email: String) {
        dismissedEmails.add(email.lowercase())
    }

    @Synchronized
    fun recordAutoSend(email: String) {
        autoSendTimes[email.lowercase()] = now()
    }

    @VisibleForTesting
    @Synchronized
    fun reset() {
        dismissedEmails.clear()
        autoSendTimes.clear()
        owner = null
    }

    companion object {
        /**
         * Roughly how long a code stays valid.
         */
        const val AUTO_SEND_COOLDOWN_MILLIS = 10 * 60 * 1000L

        val shared = EmbeddedLinkInlineAuthRegistry(now = SystemClock::elapsedRealtime)
    }
}
