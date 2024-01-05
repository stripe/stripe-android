package com.stripe.android.paymentsheet.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun Mandate(
    mandateText: String?,
    modifier: Modifier = Modifier,
) {
    mandateText?.let { text ->
        Html(
            html = text,
            color = MaterialTheme.stripeColors.subtitle,
            style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
            modifier = modifier,
        )
    }
}
