package com.stripe.android.paymentsheet.model

import android.content.res.Configuration
import android.graphics.drawable.ShapeDrawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `iconPainter uses new drawable when system theme changes`() = runTest {
        val imageLoaderCalls = Turbine<Boolean?>()
        val lightDrawable = ShapeDrawable().apply {
            setIntrinsicWidth(10)
            setIntrinsicHeight(20)
        }
        val darkDrawable = ShapeDrawable().apply {
            setIntrinsicWidth(30)
            setIntrinsicHeight(40)
        }
        val paymentOption = PaymentOption(
            drawableResourceId = 0,
            label = "Visa",
            paymentMethodType = "card",
            billingDetails = null,
            _shippingDetails = null,
            _labels = PaymentOption.Labels(label = "Visa"),
            imageLoader = { isSystemDark ->
                imageLoaderCalls.add(isSystemDark)
                if (isSystemDark == true) darkDrawable else lightDrawable
            },
        )
        var configuration by mutableStateOf(configuration(Configuration.UI_MODE_NIGHT_NO))
        var currentPainter: Painter? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                currentPainter = paymentOption.iconPainter
            }
        }

        composeRule.waitForIdle()
        val lightPainter = requireNotNull(currentPainter)
        assertThat(imageLoaderCalls.awaitItem()).isFalse()
        assertThat(lightPainter.intrinsicSize).isEqualTo(Size(10f, 20f))

        composeRule.runOnIdle {
            configuration = configuration(Configuration.UI_MODE_NIGHT_YES)
        }
        composeRule.waitForIdle()

        val darkPainter = requireNotNull(currentPainter)
        assertThat(imageLoaderCalls.awaitItem()).isTrue()
        assertThat(darkPainter).isNotSameInstanceAs(lightPainter)
        assertThat(darkPainter.intrinsicSize).isEqualTo(Size(30f, 40f))
        imageLoaderCalls.ensureAllEventsConsumed()
    }

    private fun configuration(uiMode: Int): Configuration {
        return Configuration().apply {
            this.uiMode = uiMode
        }
    }
}
