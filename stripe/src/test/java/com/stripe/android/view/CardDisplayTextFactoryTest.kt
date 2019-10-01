package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDisplayTextFactoryTest {

    @Test
    fun createUnstyled_withVisa() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            ThemeConfig(context)
        )
        assertEquals(
            "Visa ending in 4242",
            cardDisplayTextFactory.createUnstyled(PaymentMethodFixtures.CARD)
        )
    }

    @Test
    fun createUnstyled_withUnknown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            ThemeConfig(context)
        )
        assertEquals(
            "Unknown ending in 4242",
            cardDisplayTextFactory.createUnstyled(
                PaymentMethod.Card.Builder()
                    .setLast4("4242")
                    .build()
            )
        )
    }

    @Test
    fun createStyled_withVisaWithoutLast4() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            ThemeConfig(context)
        )
        assertEquals(
            "Visa",
            cardDisplayTextFactory.createStyled(
                PaymentMethod.Card.Brand.VISA,
                null,
                false
            ).toString()
        )
    }
}
