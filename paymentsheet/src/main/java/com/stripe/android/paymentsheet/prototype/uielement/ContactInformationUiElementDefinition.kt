package com.stripe.android.paymentsheet.prototype.uielement

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.prototype.ContactInformationCollectionMode
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState

internal class ContactInformationUiElementDefinition(
    // TODO: How should we tell it what modes to render?
    requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode>
) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        // TODO: update return based on state.
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return ContactInformationUiRenderer()
    }
}

private class ContactInformationUiRenderer :UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        // TODO:
    }
}
