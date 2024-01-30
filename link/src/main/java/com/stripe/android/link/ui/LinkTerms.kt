package com.stripe.android.link.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.link.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun LinkTerms(
    isOptional: Boolean,
    isShowingPhoneFirst: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val text = if (isShowingPhoneFirst) {
        stringResource(R.string.stripe_sign_up_terms_alternative_with_phone_number)
    } else if (isOptional) {
        stringResource(R.string.stripe_sign_up_terms_alternative)
    } else {
        stringResource(R.string.stripe_sign_up_terms)
    }

    Html(
        html = text.replaceHyperlinks(),
        color = MaterialTheme.stripeColors.placeholderText,
        style = MaterialTheme.typography.caption.copy(
            textAlign = textAlign,
            fontWeight = FontWeight.Normal,
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

@Preview
@Composable
private fun LinkTermsPreview() {
    StripeTheme {
        Surface {
            LinkTerms(
                isOptional = true,
                isShowingPhoneFirst = false,
            )
        }
    }
}
