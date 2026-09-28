package com.stripe.android.link.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import org.junit.Test

internal class LinkLogoResourcesTest {
    @Test
    fun `Link primary logo maps to Link asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.Primary))
            .isEqualTo(R.drawable.stripe_link_logo)
    }

    @Test
    fun `Onelink primary logo maps to Onelink asset`() {
        assertThat(LinkBrand.Onelink.logoRes(LinkLogoStyle.Primary))
            .isEqualTo(R.drawable.stripe_onelink_logo)
    }

    @Test
    fun `Link inline logo maps to Link knockout asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.InlineKnockout))
            .isEqualTo(R.drawable.stripe_link_logo_knockout)
    }

    @Test
    fun `Onelink inline logo maps to Onelink knockout asset`() {
        assertThat(LinkBrand.Onelink.logoRes(LinkLogoStyle.InlineKnockout))
            .isEqualTo(R.drawable.stripe_onelink_logo_knockout)
    }

    @Test
    fun `Link terms black logo maps to Link black asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.TermsKnockoutBlack))
            .isEqualTo(R.drawable.stripe_link_logo_knockout_black)
    }

    @Test
    fun `Onelink terms black logo maps to Onelink black asset`() {
        assertThat(LinkBrand.Onelink.logoRes(LinkLogoStyle.TermsKnockoutBlack))
            .isEqualTo(R.drawable.stripe_onelink_logo_knockout_black)
    }

    @Test
    fun `Link terms white logo maps to Link white asset`() {
        assertThat(LinkBrand.Link.logoRes(LinkLogoStyle.TermsKnockoutWhite))
            .isEqualTo(R.drawable.stripe_link_logo_knockout_white)
    }

    @Test
    fun `Onelink terms white logo maps to Onelink white asset`() {
        assertThat(LinkBrand.Onelink.logoRes(LinkLogoStyle.TermsKnockoutWhite))
            .isEqualTo(R.drawable.stripe_onelink_logo_knockout_white)
    }
}
