package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class ManualEntryStates : PreviewParameterProvider<ManualEntryState> {
    override val values = sequenceOf(
        default()
    )

    override val count: Int
        get() = super.count

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun default() = ManualEntryState()
    }
}
