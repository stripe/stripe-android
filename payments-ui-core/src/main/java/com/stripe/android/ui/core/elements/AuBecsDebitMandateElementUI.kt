package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.R

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement
) {
    Html(
        html = stringResource(id = R.string.au_becs_mandate, element.merchantName ?: ""),
        imageGetter = emptyMap(),
        color = PaymentsTheme.colors.subtitle,
        style = PaymentsTheme.typography.body2,
    )
}
