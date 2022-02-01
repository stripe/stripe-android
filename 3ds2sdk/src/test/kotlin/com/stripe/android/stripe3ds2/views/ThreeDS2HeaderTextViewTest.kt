package com.stripe.android.stripe3ds2.views

import android.graphics.Color
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThreeDS2HeaderTextViewTest {
    private val threeDS2HeaderTextView = ThreeDS2HeaderTextView(
        ApplicationProvider.getApplicationContext()
    )

    private val labelCustomization = StripeLabelCustomization().apply {
        setHeadingTextColor("#FFFFFF")
        setTextColor("#000000")
        setHeadingTextFontName("serif")
        setTextFontName("sans-serif-thin")
        textFontSize = 16
        headingTextFontSize = 32
    }

    @Test
    fun setText_nullCustomization_textSet() {
        val labelCustomization: LabelCustomization? = null
        threeDS2HeaderTextView.setText("HEADER", labelCustomization)
        assertEquals("HEADER", threeDS2HeaderTextView.text.toString())
    }

    @Test
    fun setHeaderText_withCustomization_textSetWithHeaderCustomization() {
        val text = "HEADER"
        threeDS2HeaderTextView.setText(text, labelCustomization)
        assertEquals(text, threeDS2HeaderTextView.text.toString())
        assertEquals(Color.WHITE.toLong(), threeDS2HeaderTextView.currentTextColor.toLong())
        assertEquals(32f, threeDS2HeaderTextView.textSize)
        assertEquals(
            Typeface.create("serif", Typeface.NORMAL),
            threeDS2HeaderTextView.typeface
        )
    }
}
