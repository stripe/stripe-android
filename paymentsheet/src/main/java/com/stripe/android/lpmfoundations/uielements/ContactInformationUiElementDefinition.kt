package com.stripe.android.lpmfoundations.uielements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.UiElementDefinition
import com.stripe.android.lpmfoundations.UiRenderer
import com.stripe.android.lpmfoundations.UiState

internal class ContactInformationUiElementDefinition(
    val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode>
) : UiElementDefinition {
    override fun renderer(uiState: UiState): UiRenderer {
        return ContactInformationUiRenderer()
    }
}

private class ContactInformationUiRenderer : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        // TODO(jaynewstrom): Create UI.
    }
}
