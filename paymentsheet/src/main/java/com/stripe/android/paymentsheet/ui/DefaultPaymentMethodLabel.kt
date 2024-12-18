package com.stripe.android.paymentsheet.ui

import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.preview.DefaultStripeThemePreviewRow
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal fun DefaultPaymentMethodLabel(
    modifier: Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.stripe_paymentsheet_default_payment_method_label),
        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.stripeColors.placeholderText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
private fun DefaultPaymentMethodLabelPreview() {
    DefaultStripeThemePreviewRow {
        DefaultPaymentMethodLabel(
            modifier = Modifier
        )
    }
}
