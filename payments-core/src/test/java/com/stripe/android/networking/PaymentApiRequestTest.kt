package com.stripe.android.networking

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CardParamsFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentApiRequestTest {
    @Test
    fun url_withCardData_createsProperQueryString() {
        val url = ApiRequest.Factory().createGet(
            url = StripeApiRepository.sourcesUrl,
            options = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            params = CardParamsFixtures.MINIMUM.toParamMap()
        ).url

        assertThat(Uri.parse(url))
            .isEqualTo(Uri.parse("https://api.stripe.com/v1/sources?card%5Bnumber%5D=4242424242424242&card%5Bexp_month%5D=1&card%5Bcvc%5D=123&card%5Bexp_year%5D=2050"))
    }
}
