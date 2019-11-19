package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.SourceOrderParams
import com.stripe.android.model.SourceParams
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceEndToEndTest {
    @Test
    fun createKlarna() {
        val params = SourceParams.createKlarna(
            amount = 1000,
            currency = "eur",
            purchaseCountry = "DE",
            sourceOrderParams = SourceOrderParams(
                items = listOf(
                    SourceOrderParams.Item(
                        amount = 1000,
                        currency = "eur",
                        description = "shoes",
                        quantity = 1
                    )
                )
            ),
            returnUrl = "example://redirect"
        )
        val stripe = createStripe(ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)

        val source = requireNotNull(stripe.createSourceSynchronous(params))
        assertEquals("example://redirect", source.redirect?.returnUrl)
        assertEquals("shoes", source.sourceOrder?.items?.first()?.description)
    }

    private fun createStripe(publishableKey: String): Stripe {
        return Stripe(
            ApplicationProvider.getApplicationContext<Context>(),
            publishableKey
        )
    }
}
