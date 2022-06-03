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
fun AffirmElementUI() {
    Html(
        html = stringResource(id = R.string.affirm_buy_now_pay_later),
        imageGetter = mapOf(
            "affirm" to EmbeddableImage(
                R.drawable.stripe_ic_affirm_logo,
                R.string.stripe_paymentsheet_payment_method_affirm
            )
        ),
        color = MaterialTheme.paymentsColors.subtitle,
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
