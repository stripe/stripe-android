package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stripe.android.uicore.stripeColorScheme
import com.stripe.android.uicore.text.Html
import com.stripe.android.R as StripeR

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement,
    modifier: Modifier = Modifier,
) {
    Html(
        html = stringResource(id = StripeR.string.stripe_au_becs_mandate, element.merchantName ?: ""),
        color = MaterialTheme.stripeColorScheme.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
