package com.stripe.android.networking

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FraudDetectionDataFixtures
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
                .plus(FRAUD_DETECTION_DATA.params)
        ).url

        assertThat(Uri.parse(url))
            .isEqualTo(Uri.parse("https://api.stripe.com/v1/sources?muid=${FRAUD_DETECTION_DATA.muid}&guid=${FRAUD_DETECTION_DATA.guid}&card%5Bnumber%5D=4242424242424242&card%5Bexp_month%5D=1&card%5Bcvc%5D=123&card%5Bexp_year%5D=2050&sid=${FRAUD_DETECTION_DATA.sid}"))
    }

    private companion object {
        private val FRAUD_DETECTION_DATA = FraudDetectionDataFixtures.create()
    }
}
