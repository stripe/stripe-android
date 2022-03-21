package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AffirmElementUI() {
    HtmlText(
        html = stringResource(id = R.string.affirm_buy_now_pay_later),
        imageGetter = mapOf(
            "affirm" to R.drawable.stripe_ic_affirm_logo
        ),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
