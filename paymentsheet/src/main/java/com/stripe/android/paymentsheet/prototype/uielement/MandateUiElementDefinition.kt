package com.stripe.android.paymentsheet.prototype.uielement

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors

internal class MandateUiElementDefinition(private val text: ResolvableString) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return MandateUiRenderer(text)
    }
}

private class MandateUiRenderer(private val text: ResolvableString) : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        Text(
            text = text.resolve(),
            style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.stripeColors.subtitle,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {} // makes it a separate accessible item
        )
    }
}
