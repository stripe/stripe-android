package com.stripe.android.view

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.R
import com.stripe.android.databinding.StripeActivityBinding
import com.stripe.android.utils.createTestActivityRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class StripeColorUtilsTest {
    private val activityScenarioFactory = ActivityScenarioFactory(
        ApplicationProvider.getApplicationContext()
    )

    @BeforeTest
    fun setup() {
        CustomerSession.instance = mock()
    }

    @get:Rule
    internal var testActivityRule = createTestActivityRule<TestActivity>()

    @Test
    fun getThemeAccentColor_getsNonzeroColor() {
        activityScenarioFactory.create<TestActivity>().use { activityScenario ->
            activityScenario.onActivity {
                assertThat(Color.alpha(StripeColorUtils(it).colorAccent))
                    .isGreaterThan(0)
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
    }

    // TODO: not passing, need to fix
    @Test
    fun getThemeColorControlNormal_getsNonzeroColor() {
        activityScenarioFactory.create<TestActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity {
                assertThat(Color.alpha(StripeColorUtils(it).colorControlNormal))
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

    internal class TestActivity : Activity() {
        private val viewBinding: StripeActivityBinding by lazy {
            StripeActivityBinding.inflate(layoutInflater)
        }

        override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
            super.onCreate(savedInstanceState, persistentState)

            setTheme(R.style.StripeToolBarStyle)
            setContentView(viewBinding.root)
        }
    }
}
