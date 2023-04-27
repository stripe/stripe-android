package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement
) {
    Html(
        html = stringResource(id = R.string.stripe_au_becs_mandate, element.merchantName ?: ""),
        color = MaterialTheme.stripeColors.subtitle,
        style = MaterialTheme.typography.body2,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
