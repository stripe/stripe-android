package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.hasError
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.needsFallback
import com.stripe.android.identity.networking.models.networkedIdentityRoute

/** Attempts belong to a presentation, not the VerificationSession or saved instance state. */
internal class NetworkedIdentityEntryPolicy(private val previewEnabled: Boolean) {
    private var attemptedSessionId: String? = null

    fun enter(page: VerificationPage, update: VerificationPageData?): NetworkedIdentityEntry? {
        if (!previewEnabled || attemptedSessionId == page.id) return null
        if (!isWritable(page, update)) return null

        val missing = update?.requirements?.missings ?: page.requirements.missing
        val entry = when (page.networkedIdentityRoute) {
            NetworkedIdentityRoute.ResumeReuse, NetworkedIdentityRoute.ResumeSave -> {
                if (update == null && missing.isEmpty()) NetworkedIdentityEntry.Submit else null
            }
            NetworkedIdentityRoute.Reuse -> {
                if (canReuse(page, missing)) NetworkedIdentityEntry.Reuse else null
            }
            NetworkedIdentityRoute.OrdinaryIdentity, NetworkedIdentityRoute.Save -> null
        }
        if (entry != null) attemptedSessionId = page.id
        return entry
    }

    private fun isWritable(page: VerificationPage, update: VerificationPageData?): Boolean {
        if (update != null && (update.id != page.id || update.requirements.missings == null || update.hasError())) {
            return false
        }
        val requiresInput = update?.let { it.status == VerificationPageData.Status.REQUIRESINPUT }
            ?: (page.status == VerificationPage.Status.REQUIRESINPUT)
        val submitted = update?.submitted ?: page.submitted
        return requiresInput && update?.closed != true && (!submitted || update?.needsFallback() == true)
    }

    private fun canReuse(page: VerificationPage, missing: List<Requirement>): Boolean =
        Requirement.BIOMETRICCONSENT !in missing && !page.merchantPublishableKey.isNullOrBlank() &&
            missing.any { it == Requirement.IDDOCUMENTFRONT || it == Requirement.IDDOCUMENTBACK }
}

internal enum class NetworkedIdentityEntry { Reuse, Submit }

internal sealed interface NetworkedIdentityHostEvent {
    data object Cancelled : NetworkedIdentityHostEvent
    data object Fallback : NetworkedIdentityHostEvent
    data class Completed(val result: Result<VerificationPageData>) : NetworkedIdentityHostEvent
}
