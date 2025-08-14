package com.stripe.android.link.model

import android.os.Parcelable
import com.stripe.android.model.ConsentUi
import kotlinx.parcelize.Parcelize

internal sealed interface ConsentPresentation : Parcelable {
    @Parcelize
    data class Inline(
        val consentSection: ConsentUi.ConsentSection
    ) : ConsentPresentation

    @Parcelize
    data class FullScreen(
        val consentPane: ConsentUi.ConsentPane
    ) : ConsentPresentation
}

internal fun ConsentUi.toPresentation(): ConsentPresentation? =
    consentPane?.let { ConsentPresentation.FullScreen(it) }
        ?: consentSection?.let { ConsentPresentation.Inline(it) }
