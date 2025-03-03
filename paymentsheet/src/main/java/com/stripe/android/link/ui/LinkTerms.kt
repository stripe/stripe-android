package com.stripe.android.link.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.StripeTheme

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

    Mandate(
        mandateText = text.replaceHyperlinks(),
        modifier = modifier,
        textAlign = textAlign,
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
