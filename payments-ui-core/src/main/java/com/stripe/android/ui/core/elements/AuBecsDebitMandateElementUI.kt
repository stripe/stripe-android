package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.paymentsColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement
) {
    Html(
        html = stringResource(id = R.string.au_becs_mandate, element.merchantName ?: ""),
        imageGetter = emptyMap(),
        color = MaterialTheme.paymentsColors.subtitle,
        style = MaterialTheme.typography.body2,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
