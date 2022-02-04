package com.stripe.android.view

import android.content.Context
import android.text.ParcelableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.core.text.getSpans
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    @Test
    fun createStyled_withVisaEmptyLast4() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            ThemeConfig(context)
        )
        assertEquals(
            "Visa",
            cardDisplayTextFactory.createStyled(
                CardBrand.Visa,
                "",
                false
            ).toString()
        )
    }

    @Test
    @Config(qualifiers = "ja")
    fun createStyled_ja_withMasterCard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themeConfig = ThemeConfig(context)
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            themeConfig
        )
        val styledString = cardDisplayTextFactory.createStyled(
            CardBrand.MasterCard,
            "4242",
            false
        )

        val spans = styledString.getSpans<ParcelableSpan>()

        // Full string style
        assertEquals(styledString.getSpanStart(spans[0]), 0)
        assertEquals(styledString.getSpanEnd(spans[0]), 21)
        assertEquals(
            (spans[0] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextAlphaColor(false)
        )

        // Brand style
        assertEquals(styledString.getSpanStart(spans[1]), 11)
        assertEquals(styledString.getSpanEnd(spans[1]), 21)
        assertEquals((spans[1] as TypefaceSpan).family, "sans-serif-medium")
        assertEquals(styledString.getSpanStart(spans[2]), 11)
        assertEquals(styledString.getSpanEnd(spans[2]), 21)
        assertEquals(
            (spans[2] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextColor(false)
        )

        // Last 4 style
        assertEquals(styledString.getSpanStart(spans[3]), 4)
        assertEquals(styledString.getSpanEnd(spans[3]), 8)
        assertEquals((spans[3] as TypefaceSpan).family, "sans-serif-medium")
        assertEquals(styledString.getSpanStart(spans[4]), 4)
        assertEquals(styledString.getSpanEnd(spans[4]), 8)
        assertEquals(
            (spans[4] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextColor(false)
        )

        assertEquals(
            "末尾が 4242 の Mastercard",
            styledString.toString()
        )
    }

    @Test
    fun createStyled_en_withVisa() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themeConfig = ThemeConfig(context)
        val cardDisplayTextFactory = CardDisplayTextFactory(
            context.resources,
            themeConfig
        )
        val styledString = cardDisplayTextFactory.createStyled(
            CardBrand.Visa,
            "4242",
            false
        )

        val spans = styledString.getSpans<ParcelableSpan>()

        // Full string style
        assertEquals(styledString.getSpanStart(spans[0]), 0)
        assertEquals(styledString.getSpanEnd(spans[0]), 19)
        assertEquals(
            (spans[0] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextAlphaColor(false)
        )

        // Brand style
        assertEquals(styledString.getSpanStart(spans[1]), 0)
        assertEquals(styledString.getSpanEnd(spans[1]), 4)
        assertEquals((spans[1] as TypefaceSpan).family, "sans-serif-medium")
        assertEquals(styledString.getSpanStart(spans[2]), 0)
        assertEquals(styledString.getSpanEnd(spans[2]), 4)
        assertEquals(
            (spans[2] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextColor(false)
        )

        // Last 4 style
        assertEquals(styledString.getSpanStart(spans[3]), 15)
        assertEquals(styledString.getSpanEnd(spans[3]), 19)
        assertEquals((spans[3] as TypefaceSpan).family, "sans-serif-medium")
        assertEquals(styledString.getSpanStart(spans[4]), 15)
        assertEquals(styledString.getSpanEnd(spans[4]), 19)
        assertEquals(
            (spans[4] as ForegroundColorSpan).foregroundColor,
            themeConfig.getTextColor(false)
        )

        assertEquals(
            "Visa ending in 4242",
            styledString.toString()
        )
    }
}
