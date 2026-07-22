package com.stripe.android.checkout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
class CurrencySelectorElementAppearanceTest {

    @Test
    fun `default build produces expected defaults`() {
        val state = CurrencySelectorElement.Appearance().build()

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
        assertThat(state.labelContent)
            .isEqualTo(CurrencySelectorElement.Appearance.LabelContent.AUTOMATIC)
    }

    @Test
    fun `all properties can be set via builder`() {
        val state = CurrencySelectorElement.Appearance()
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
            .labelContent(CurrencySelectorElement.Appearance.LabelContent.CURRENCY_CODE)
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
        assertThat(state.labelContent)
            .isEqualTo(CurrencySelectorElement.Appearance.LabelContent.CURRENCY_CODE)
    }

    @Test
    fun `textSecondaryColor alpha is clamped to 0_5 minimum`() {
        val state = CurrencySelectorElement.Appearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.2f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.5f)
        assertThat(resultColor.red).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun `textSecondaryColor alpha above 0_5 is preserved`() {
        val state = CurrencySelectorElement.Appearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.8f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.8f)
    }

    @Test
    fun `textSecondaryColor alpha exactly 0_5 is preserved`() {
        val state = CurrencySelectorElement.Appearance()
            .textSecondaryColor(Color.Red.copy(alpha = 0.5f))
            .build()

        val resultColor = Color(state.textSecondaryColor!!)
        assertThat(resultColor.alpha).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun `sizeScaleFactor of zero throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .sizeScaleFactor(0f)
        }
    }

    @Test
    fun `sizeScaleFactor of negative value throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .sizeScaleFactor(-1f)
        }
    }

    @Test
    fun `sizeScaleFactor of infinite value throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .sizeScaleFactor(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative contentVerticalPaddingDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .contentVerticalPaddingDp(-1f)
        }
    }

    @Test
    fun `infinite contentVerticalPaddingDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .contentVerticalPaddingDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative cornerRadiusDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .cornerRadiusDp(-1f)
        }
    }

    @Test
    fun `infinite cornerRadiusDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .cornerRadiusDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative borderWidthDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .borderWidthDp(-1f)
        }
    }

    @Test
    fun `infinite borderWidthDp throws`() {
        assertFailsWith<IllegalArgumentException> {
            CurrencySelectorElement.Appearance()
                .borderWidthDp(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `builder methods are chainable`() {
        val appearance = CurrencySelectorElement.Appearance()
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
        val state = CurrencySelectorElement.Appearance()
            .fontResId(123)
            .fontResId(null)
            .build()

        assertThat(state.fontResId).isNull()
    }

    @Test
    fun `configuration default build uses default appearance`() {
        val state = CurrencySelectorElement.Configuration().build()

        assertThat(state.appearance).isEqualTo(CurrencySelectorElement.Appearance().build())
    }

    @Test
    fun `configuration carries the supplied appearance`() {
        val appearance = CurrencySelectorElement.Appearance()
            .labelContent(CurrencySelectorElement.Appearance.LabelContent.CURRENCY_CODE)
            .cornerRadiusDp(8f)

        val state = CurrencySelectorElement.Configuration()
            .appearance(appearance)
            .build()

        assertThat(state.appearance).isEqualTo(appearance.build())
    }
}
