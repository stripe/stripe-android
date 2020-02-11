package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.PaymentMethod
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestFactoryTest {

    private val logger: Logger = mock()

    @Test
    fun create_shouldLogRequest() {
        AnalyticsRequestFactory(logger).create(
            AnalyticsDataFactory(ApplicationProvider.getApplicationContext())
                .createPaymentMethodCreationParams(
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    "pm_12345",
                    PaymentMethod.Type.Card,
                    emptySet()
                ),
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        verify(logger).info(
            "Event: stripe_android.payment_method_creation"
        )
    }
}
