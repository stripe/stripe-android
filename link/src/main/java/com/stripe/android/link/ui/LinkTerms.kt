package com.stripe.android.link.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.link.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Preview
@Composable
internal fun LinkTerms(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Html(
        html = stringResource(R.string.stripe_sign_up_terms).replaceHyperlinks(),
        color = MaterialTheme.stripeColors.placeholderText,
        style = MaterialTheme.typography.subtitle1.copy(
            textAlign = textAlign,
        ),
        modifier = modifier,
        urlSpanStyle = SpanStyle(
            color = MaterialTheme.colors.primary
        )
    )
}

private fun String.replaceHyperlinks() = this.replace(
    "<terms>",
    "<a href=\"https://link.co/terms\">"
).replace("</terms>", "</a>").replace(
    "<privacy>",
    "<a href=\"https://link.co/privacy\">"
).replace("</privacy>", "</a>")
