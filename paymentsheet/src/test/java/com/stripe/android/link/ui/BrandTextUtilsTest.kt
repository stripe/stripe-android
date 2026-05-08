package com.stripe.android.link.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BrandTextUtilsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `inline signup text replaces Link with inline brand icon`() {
        assertThat(
            context.getString(
                R.string.stripe_inline_sign_up_toggle,
            ).buildBrandIconAnnotatedString(
                brandToken = LinkBrand.Link.brandName(),
                inlineContentId = "brand_icon",
            ).text
        ).isEqualTo("Create an account with [icon] for faster checkout across the web")
    }

    @Test
    fun `inline signup text replaces Notlink with inline brand icon`() {
        assertThat(
            context.getString(
                R.string.stripe_inline_sign_up_toggle_with_brand,
                LinkBrand.Notlink.brandName(),
            ).buildBrandIconAnnotatedString(
                brandToken = LinkBrand.Notlink.brandName(),
                inlineContentId = "brand_icon",
            ).text
        ).isEqualTo("Create an account with [icon] for faster checkout across the web")
    }

    @Test
    fun `pay button text replaces Link with inline brand icon`() {
        assertThat(
            context.getString(
                R.string.stripe_pay_with_link,
            ).buildBrandIconAnnotatedString(
                brandToken = LinkBrand.Link.brandName(),
                inlineContentId = "brand_icon",
            ).text
        ).isEqualTo("Pay with [icon]")
    }

    @Test
    fun `pay button text replaces Notlink with inline brand icon`() {
        assertThat(
            context.getString(
                R.string.stripe_pay_with_link_with_brand,
                LinkBrand.Notlink.brandName(),
            ).buildBrandIconAnnotatedString(
                brandToken = LinkBrand.Notlink.brandName(),
                inlineContentId = "brand_icon",
            ).text
        ).isEqualTo("Pay with [icon]")
    }
}
