package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
class CurrencySelectorContentAppearanceTest {

    @Test
    fun `default build produces expected defaults`() {
        val state = Checkout.CurrencySelectorContentAppearance().build()

        assertThat(state.contentVerticalPaddingDp).isEqualTo(4f)
        assertThat(state.cornerRadiusDp).isNull()
        assertThat(state.borderWidthDp).isNull()
        assertThat(state.borderColor).isNull()
        assertThat(state.background).isNull()
        assertThat(state.selectedBackground).isNull()
        assertThat(state.textColor).isNull()
        assertThat(state.selectedTextColor).isNull()
        assertThat(state.textSecondaryColor).isNull()
        assertThat(state.dangerColor).isNull()
        assertThat(state.fontResId).isNull()
        assertThat(state.sizeScaleFactor).isEqualTo(1.0f)
    }

    @Test
    fun `all properties can be set via builder`() {
        val state = Checkout.CurrencySelectorContentAppearance()
            .contentVerticalPaddingDp(12f)
            .cornerRadiusDp(8f)
            .borderWidthDp(2f)
            .borderColor(Color.Red)
            .background(Color.Blue)
            .selectedBackground(Color.Green)
            .textColor(Color.White)
            .selectedTextColor(Color.Black)
            .textSecondaryColor(Color.Gray)
            .dangerColor(Color.Magenta)
            .fontResId(123)
            .sizeScaleFactor(1.5f)
            .build()

        assertThat(state.contentVerticalPaddingDp).isEqualTo(12f)
        assertThat(state.cornerRadiusDp).isEqualTo(8f)
        assertThat(state.borderWidthDp).isEqualTo(2f)
        assertThat(state.borderColor).isEqualTo(Color.Red.toArgb())
        assertThat(state.background).isEqualTo(Color.Blue.toArgb())
        assertThat(state.selectedBackground).isEqualTo(Color.Green.toArgb())
        assertThat(state.textColor).isEqualTo(Color.White.toArgb())
        assertThat(state.selectedTextColor).isEqualTo(Color.Black.toArgb())
        assertThat(state.textSecondaryColor).isEqualTo(Color.Gray.toArgb())
        assertThat(state.dangerColor).isEqualTo(Color.Magenta.toArgb())
        assertThat(state.fontResId).isEqualTo(123)
        assertThat(state.sizeScaleFactor).isEqualTo(1.5f)
    }

    @Test
    fun `textSecondaryColor alpha is clamped to 0_5 minimum`() {
        val state = Checkout.CurrencySelectorContentAppearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.2f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.5f)
        assertThat(resultColor.red).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun `textSecondaryColor alpha above 0_5 is preserved`() {
        val state = Checkout.CurrencySelectorContentAppearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.8f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.8f)
    }

    @Test
    fun `textSecondaryColor alpha exactly 0_5 is preserved`() {
        val state = Checkout.CurrencySelectorContentAppearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.5f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun `sizeScaleFactor of zero throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .sizeScaleFactor(0f)
        }
    }

    @Test
    fun `sizeScaleFactor of negative value throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .sizeScaleFactor(-1f)
        }
    }

    @Test
    fun `sizeScaleFactor of infinite value throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .sizeScaleFactor(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative contentVerticalPaddingDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .contentVerticalPaddingDp(-1f)
        }
    }

    @Test
    fun `infinite contentVerticalPaddingDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .contentVerticalPaddingDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative cornerRadiusDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .cornerRadiusDp(-1f)
        }
    }

    @Test
    fun `infinite cornerRadiusDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .cornerRadiusDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative borderWidthDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .borderWidthDp(-1f)
        }
    }

    @Test
    fun `infinite borderWidthDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            Checkout.CurrencySelectorContentAppearance()
                .borderWidthDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `builder methods are chainable`() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .contentVerticalPaddingDp(10f)
            .cornerRadiusDp(20f)
            .borderWidthDp(1f)

        val state = appearance.build()
        assertThat(state.contentVerticalPaddingDp).isEqualTo(10f)
        assertThat(state.cornerRadiusDp).isEqualTo(20f)
        assertThat(state.borderWidthDp).isEqualTo(1f)
    }

    @Test
    fun `fontResId null clears font`() {
        val state = Checkout.CurrencySelectorContentAppearance()
            .fontResId(123)
            .fontResId(null)
            .build()

        assertThat(state.fontResId).isNull()
    }
}
