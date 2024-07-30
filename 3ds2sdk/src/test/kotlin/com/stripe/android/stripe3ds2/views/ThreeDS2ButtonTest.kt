package com.stripe.android.stripe3ds2.views

import android.graphics.Color
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThreeDS2ButtonTest {

    private lateinit var threeDS2Button: ThreeDS2Button

    @BeforeTest
    fun before() {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
            .create()
            .use {
                it.onActivity { activity ->
                    threeDS2Button = ThreeDS2Button(activity)
                }
            }
    }

    @Test
    fun parseColor_black_shouldParseColor() {
        assertEquals(Color.BLACK.toLong(), threeDS2Button.parseColor("#000000").toLong())
    }

    @Test
    fun setButtonCustomization_buttonIsCustomized() {
        val customization = StripeButtonCustomization()
        customization.setBackgroundColor("#000000")
        customization.setTextColor("#FFFFFF")
        customization.cornerRadius = 60
        customization.textFontSize = 20
        customization.setTextFontName("sans-serif-thin")

        threeDS2Button.setButtonCustomization(customization)

        assertEquals(
            Color.BLACK.toLong(),
            threeDS2Button.backgroundTintList?.defaultColor?.toLong()
        )

        assertEquals(60, threeDS2Button.cornerRadius.toLong())

        assertEquals(Color.WHITE.toLong(), threeDS2Button.textColors.defaultColor.toLong())
        assertEquals(20f, threeDS2Button.textSize)

        assertEquals(
            Typeface.create("sans-serif-thin", Typeface.NORMAL),
            threeDS2Button.typeface
        )
    }
}
