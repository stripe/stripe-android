package com.stripe.android.link.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import org.junit.Test

internal class LinkLogoResourcesTest {
    @Test
    fun `Link primary logo maps to Link asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.Primary)).isEqualTo(R.drawable.stripe_link_logo)
    }

    @Test
    fun `Notlink primary logo maps to Notlink asset`() {
        assertThat(LinkBrand.Notlink.logoRes(LinkLogoStyle.Primary)).isEqualTo(R.drawable.stripe_notlink_logo)
    }

    @Test
    fun `Link inline logo maps to Link knockout asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.InlineKnockout)).isEqualTo(R.drawable.stripe_link_logo_knockout)
    }

    @Test
    fun `Notlink inline logo maps to Notlink knockout asset`() {
        assertThat(LinkBrand.Notlink.logoRes(LinkLogoStyle.InlineKnockout)).isEqualTo(R.drawable.stripe_notlink_logo_knockout)
    }

    @Test
    fun `Link terms black logo maps to Link black asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.TermsKnockoutBlack)).isEqualTo(R.drawable.stripe_link_logo_knockout_black)
    }

    @Test
    fun `Notlink terms black logo maps to Notlink black asset`() {
        assertThat(LinkBrand.Notlink.logoRes(LinkLogoStyle.TermsKnockoutBlack)).isEqualTo(R.drawable.stripe_notlink_logo_knockout_black)
    }

    @Test
    fun `Link terms white logo maps to Link white asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.TermsKnockoutWhite)).isEqualTo(R.drawable.stripe_link_logo_knockout_white)
    }

    @Test
    fun `Notlink terms white logo maps to Notlink white asset`() {
        assertThat(LinkBrand.Notlink.logoRes(LinkLogoStyle.TermsKnockoutWhite)).isEqualTo(R.drawable.stripe_notlink_logo_knockout_white)
    }
}
