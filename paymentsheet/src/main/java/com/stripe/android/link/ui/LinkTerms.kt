package com.stripe.android.link.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.EmbeddableImage

internal enum class LinkTermsType {
    InlineOptionalWithPhoneFirst,
    InlineOptional,
    Inline,
    InlineWithDefaultOptIn,
    Full,
}

@Composable
internal fun LinkTerms(
    type: LinkTermsType,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val text = when (type) {
        LinkTermsType.InlineOptionalWithPhoneFirst -> {
            stringResource(R.string.stripe_sign_up_terms_alternative_with_phone_number)
        }
        LinkTermsType.InlineOptional -> {
            stringResource(R.string.stripe_sign_up_terms_alternative)
        }
        LinkTermsType.Inline -> {
            stringResource(R.string.stripe_sign_up_terms)
        }
        LinkTermsType.InlineWithDefaultOptIn -> {
            "<img src=\"link_logo\"> • " + stringResource(R.string.stripe_sign_up_terms_default_opt_in)
        }
        LinkTermsType.Full -> {
            stringResource(R.string.stripe_link_sign_up_terms)
        }
    }

    val imageLoader = buildMap {
        if (type == LinkTermsType.InlineWithDefaultOptIn) {
            put(
                "link_logo",
                EmbeddableImage.Drawable(
                    id = if (MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()) {
                        R.drawable.stripe_link_logo_knockout_black
                    } else {
                        R.drawable.stripe_link_logo_knockout_white
                    },
                    contentDescription = com.stripe.android.R.string.stripe_link,
                )
            )
        }
    }

    Mandate(
        mandateText = text.replaceHyperlinks(),
        modifier = modifier,
        textAlign = textAlign,
        imageAlign = PlaceholderVerticalAlign.TextCenter,
        imageLoader = imageLoader,
    )
}

internal fun String.replaceHyperlinks() = this.replace(
    "<terms>",
    "<a href=\"https://link.co/terms\">"
).replace("</terms>", "</a>").replace(
    "<privacy>",
    "<a href=\"https://link.co/privacy\">"
).replace("</privacy>", "</a>").replace(
    "<link-home>",
    "<a href=\"https://link.co\">"
).replace("</link-home>", "</a>")

@Preview
@Composable
private fun LinkTermsPreview() {
    StripeTheme {
        Surface {
            LinkTerms(
                type = LinkTermsType.InlineOptional,
            )
        }
    }
}
