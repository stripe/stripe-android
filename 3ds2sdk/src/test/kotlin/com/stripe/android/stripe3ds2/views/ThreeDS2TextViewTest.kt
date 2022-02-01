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
class ThreeDS2TextViewTest {

    private val threeDS2TextView = ThreeDS2TextView(ApplicationProvider.getApplicationContext())

    private val labelCustomization: StripeLabelCustomization =
        StripeLabelCustomization().apply {
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
        threeDS2TextView.setText("TEXT", labelCustomization)
        assertEquals("TEXT", threeDS2TextView.text.toString())
    }

    @Test
    fun setText_withCustomization_textSetWithCustomization() {
        val text = "TEXT"
        threeDS2TextView.setText(text, labelCustomization)
        assertEquals(text, threeDS2TextView.text.toString())
        assertEquals(Color.BLACK.toLong(), threeDS2TextView.currentTextColor.toLong())
        assertEquals(16f, threeDS2TextView.textSize)
        assertEquals(
            Typeface.create("sans-serif-thin", Typeface.NORMAL),
            threeDS2TextView.typeface
        )
    }
}
