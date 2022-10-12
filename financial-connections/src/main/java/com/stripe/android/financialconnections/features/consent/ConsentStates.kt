package com.stripe.android.financialconnections.features.consent

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.model.sampleConsent

internal class ConsentStates : PreviewParameterProvider<ConsentState> {
    override val values = sequenceOf(
        canonical()
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun canonical() = ConsentState(consent = Success(sampleConsent))
        fun manualEntryPlusMicrodeposits() = canonical().copy(
            manualEntryEnabled = true,
            manualEntryShowBusinessDaysNotice = true
        )
    }
}
