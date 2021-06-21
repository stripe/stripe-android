package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

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
                PaymentMethod.Card(
                    last4 = "4242"
                )
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
                CardBrand.Visa,
                null,
                false
            ).toString()
        )
    }
}
