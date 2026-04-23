package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R

internal enum class LinkLogoStyle {
    Primary,
    InlineKnockout,
    TermsKnockoutBlack,
    TermsKnockoutWhite,
}

@DrawableRes
internal fun LinkBrand.logoRes(style: LinkLogoStyle): Int = when (this) {
    LinkBrand.Link -> when (style) {
        LinkLogoStyle.Primary -> R.drawable.stripe_link_logo
        LinkLogoStyle.InlineKnockout -> R.drawable.stripe_link_logo_knockout
        LinkLogoStyle.TermsKnockoutBlack -> R.drawable.stripe_link_logo_knockout_black
        LinkLogoStyle.TermsKnockoutWhite -> R.drawable.stripe_link_logo_knockout_white
    }
    LinkBrand.Notlink -> when (style) {
        LinkLogoStyle.Primary -> R.drawable.stripe_notlink_logo
        LinkLogoStyle.InlineKnockout -> R.drawable.stripe_notlink_logo_knockout
        LinkLogoStyle.TermsKnockoutBlack -> R.drawable.stripe_notlink_logo_knockout_black
        LinkLogoStyle.TermsKnockoutWhite -> R.drawable.stripe_notlink_logo_knockout_white
    }
}
