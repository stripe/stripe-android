package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.LinkBrand
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
    linkBrand: LinkBrand,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val text = linkTermsText(type, linkBrand)

    val imageLoader = buildMap {
        if (type != LinkTermsType.Full) {
            put(
                "link_logo",
                EmbeddableImage.Drawable(
                    id = if (MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()) {
                        linkBrand.logoRes(LinkLogoStyle.TermsKnockoutBlack)
                    } else {
                        linkBrand.logoRes(LinkLogoStyle.TermsKnockoutWhite)
                    },
                    contentDescription = when (linkBrand) {
                        LinkBrand.Link -> com.stripe.android.R.string.stripe_link
                        LinkBrand.Onelink -> com.stripe.android.R.string.stripe_onelink
                    },
                )
            )
        }
    }

    Mandate(
        mandateText = text.replaceHyperlinks(linkBrand),
        modifier = modifier,
        textAlign = textAlign,
        imageAlign = PlaceholderVerticalAlign.TextCenter,
        imageLoader = imageLoader,
    )
}

@Composable
private fun linkTermsText(type: LinkTermsType, linkBrand: LinkBrand): String {
    val brandName = linkBrand.brandName()
    return when (type) {
        LinkTermsType.InlineOptionalWithPhoneFirst -> {
            val terms = if (linkBrand == LinkBrand.Link) {
                stringResource(R.string.stripe_sign_up_terms_alternative_with_phone_number)
            } else {
                stringResource(R.string.stripe_sign_up_terms_alternative_with_phone_number_branded, brandName)
            }
            terms.withLeadingLogo()
        }
        LinkTermsType.InlineOptional -> {
            val terms = if (linkBrand == LinkBrand.Link) {
                stringResource(R.string.stripe_sign_up_terms_alternative)
            } else {
                stringResource(R.string.stripe_sign_up_terms_alternative_branded, brandName)
            }
            terms.withLeadingLogo()
        }
        LinkTermsType.Inline -> {
            val terms = if (linkBrand == LinkBrand.Link) {
                stringResource(R.string.stripe_sign_up_terms)
            } else {
                stringResource(R.string.stripe_sign_up_terms_branded, brandName)
            }
            terms.withLeadingLogo()
        }
        LinkTermsType.InlineWithDefaultOptIn -> {
            val terms = if (linkBrand == LinkBrand.Link) {
                stringResource(R.string.stripe_sign_up_terms_default_opt_in)
            } else {
                stringResource(R.string.stripe_sign_up_terms_default_opt_in_branded, brandName)
            }
            terms.withLeadingLogo()
        }
        LinkTermsType.Full -> stringResource(R.string.stripe_link_sign_up_terms)
    }
}

private fun String.withLeadingLogo() = "<img src=\"link_logo\"> • $this"

internal fun String.replaceHyperlinks(linkBrand: LinkBrand) = this.replace(
    "<terms>",
    "<a href=\"${linkBrand.termsUrl()}\">"
).replace("</terms>", "</a>").replace(
    "<privacy>",
    "<a href=\"${linkBrand.privacyUrl()}\">"
).replace("</privacy>", "</a>").replace(
    "<link>",
    "<a href=\"${linkBrand.baseUrl()}\">"
).replace("</link>", "</a>")

@Preview(name = "All types - Link")
@Composable
private fun LinkTermsAllTypesLinkPreview() {
    StripeTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                LinkTermsType.entries.forEach { type ->
                    LinkTerms(
                        type = type,
                        linkBrand = LinkBrand.Link,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "All types - Onelink")
@Composable
private fun LinkTermsAllTypesOnelinkPreview() {
    StripeTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                LinkTermsType.entries.forEach { type ->
                    LinkTerms(
                        type = type,
                        linkBrand = LinkBrand.Onelink,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}
