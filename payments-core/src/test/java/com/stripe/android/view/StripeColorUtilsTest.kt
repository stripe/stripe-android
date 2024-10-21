package com.stripe.android.view

import android.graphics.Color
import android.os.Looper
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.utils.createTestActivityRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class StripeColorUtilsTest {
    private val activityScenarioFactory = ActivityScenarioFactory(
        ApplicationProvider.getApplicationContext()
    )

    @get:Rule
    internal var testActivityRule = createTestActivityRule<TestActivity>()

    @Test
    fun getThemeAccentColor_getsNonzeroColor() {
        activityScenarioFactory.create<TestActivity>().use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.setTheme(R.style.StripeDefaultTheme)
                assertThat(Color.alpha(StripeColorUtils(activity).colorAccent))
                    .isGreaterThan(0)
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun getThemeColorControlNormal_getsNonzeroColor() {
        activityScenarioFactory.create<TestActivity>().use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.setTheme(R.style.StripeDefaultTheme)
                assertThat(Color.alpha(StripeColorUtils(activity).colorControlNormal))
                    .isGreaterThan(0)
            }
        }
    }

    @Test
    fun isColorTransparent_whenColorIsZero_returnsTrue() {
        assertTrue(StripeColorUtils.isColorTransparent(0))
    }

    @Test
    fun isColorTransparent_whenColorIsNonzeroButHasLowAlpha_returnsTrue() {
        @ColorInt val invisibleBlue = 0x050000ff

        @ColorInt val invisibleRed = 0x0bff0000

        assertTrue(StripeColorUtils.isColorTransparent(invisibleBlue))
        assertTrue(StripeColorUtils.isColorTransparent(invisibleRed))
    }

    @Test
    fun isColorTransparent_whenColorIsNotCloseToTransparent_returnsFalse() {
        @ColorInt val brightWhite = -0x1

        @ColorInt val completelyBlack = -0x1000000

        assertFalse(StripeColorUtils.isColorTransparent(brightWhite))
        assertFalse(StripeColorUtils.isColorTransparent(completelyBlack))
    }

    @Test
    fun isColorDark_forExampleLightColors_returnsFalse() {
        @ColorInt val middleGray = 0x888888

        @ColorInt val offWhite = 0xfaebd7

        @ColorInt val lightCyan = 0x8feffb

        @ColorInt val lightYellow = 0xfcf4b2

        @ColorInt val lightBlue = 0x9cdbff

        assertFalse(StripeColorUtils.isColorDark(middleGray))
        assertFalse(StripeColorUtils.isColorDark(offWhite))
        assertFalse(StripeColorUtils.isColorDark(lightCyan))
        assertFalse(StripeColorUtils.isColorDark(lightYellow))
        assertFalse(StripeColorUtils.isColorDark(lightBlue))
        assertFalse(StripeColorUtils.isColorDark(Color.WHITE))
    }

    @Test
    fun isColorDark_forExampleDarkColors_returnsTrue() {
        @ColorInt val logoBlue = 0x6772e5

        @ColorInt val slate = 0x525f7f

        @ColorInt val darkPurple = 0x6b3791

        @ColorInt val darkishRed = 0x9e2146

        assertTrue(StripeColorUtils.isColorDark(logoBlue))
        assertTrue(StripeColorUtils.isColorDark(slate))
        assertTrue(StripeColorUtils.isColorDark(darkPurple))
        assertTrue(StripeColorUtils.isColorDark(darkishRed))
        assertTrue(StripeColorUtils.isColorDark(Color.BLACK))
    }

    internal class TestActivity : AppCompatActivity()
}
