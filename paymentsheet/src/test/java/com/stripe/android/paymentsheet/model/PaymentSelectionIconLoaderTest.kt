package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class PaymentSelectionIconLoaderTest {

    private val workingUrl = "working url"
    private val brokenUrl = "broken url"
    private val lightUrl = "light url"
    private val darkUrl = "dark url"
    private val simpleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    private val lightBitmap = Bitmap.createBitmap(11, 11, Bitmap.Config.ARGB_8888)
    private val darkBitmap = Bitmap.createBitmap(22, 22, Bitmap.Config.ARGB_8888)
    private val testDispatcher = StandardTestDispatcher()

    @After
    fun tearDown() {
        StripeTheme.nightModeOverride = null
    }

    @Test
    fun loadPaymentOptionWithIconUrl_usesIconFromUrl() = runScenario(
        iconUrl = workingUrl,
        iconRes = R.drawable.stripe_ic_paymentsheet_link_ref,
    ) {
        assertThat(drawable.current).isInstanceOf<BitmapDrawable>()
        assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(simpleBitmap)
    }

    @Test
    fun loadPaymentOptionWithIconUrl_failsToLoad_usesIconFromRes() = runScenario(
        iconUrl = brokenUrl,
        iconRes = R.drawable.stripe_ic_paymentsheet_link_ref,
    ) {
        assertThat(drawable.current).isInstanceOf<VectorDrawable>()
    }

    @Test
    fun loadPaymentOptionWithIconUrl_failsToLoad_missingIconRes_usesEmptyDrawable() = runScenario(
        iconUrl = brokenUrl,
        iconRes = null,
    ) {
        assertThat(drawable.current).isInstanceOf<ShapeDrawable>()
        assertThat(drawable.current).isEqualTo(PaymentSelection.IconLoader.emptyDrawable)
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_usesIconFromRes() = runScenario(
        iconUrl = null,
        iconRes = R.drawable.stripe_ic_paymentsheet_link_ref,
    ) {
        assertThat(drawable.current).isInstanceOf<VectorDrawable>()
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_missingIconRes_usesEmptyDrawable() = runScenario(
        iconUrl = null,
        iconRes = null,
    ) {
        assertThat(drawable.current).isInstanceOf<ShapeDrawable>()
        assertThat(drawable.current).isEqualTo(PaymentSelection.IconLoader.emptyDrawable)
    }

    @Test
    fun forcedDarkAppearance_usesDarkThemeIconUrl() {
        StripeTheme.nightModeOverride = true

        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(darkBitmap)
        }
    }

    @Test
    fun forcedLightAppearance_usesLightThemeIconUrl() {
        StripeTheme.nightModeOverride = false

        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(lightBitmap)
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun forcedLightAppearance_onDarkDevice_usesLightThemeIconUrl() {
        StripeTheme.nightModeOverride = false

        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(lightBitmap)
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun automaticAppearance_onDarkDevice_usesDarkThemeIconUrl() {
        StripeTheme.nightModeOverride = null

        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(darkBitmap)
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun forcedLightAppearance_onDarkDevice_resolvesResourceDrawable() {
        StripeTheme.nightModeOverride = false

        runScenario(iconUrl = null, darkIconUrl = null, iconRes = LINK_REF) {
            assertThat(drawable.current).isInstanceOf<VectorDrawable>()
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun forcedDarkOverrideLight_onDarkDevice_usesLightThemeIconUrl() {
        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF, forcedDarkOverride = false) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(lightBitmap)
        }
    }

    @Test
    fun forcedDarkOverrideDark_onLightDevice_usesDarkThemeIconUrl() {
        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF, forcedDarkOverride = true) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(darkBitmap)
        }
    }

    @Test
    fun forcedDarkOverride_takesPrecedenceOverGlobal() {
        StripeTheme.nightModeOverride = true

        runScenario(iconUrl = lightUrl, darkIconUrl = darkUrl, iconRes = LINK_REF, forcedDarkOverride = false) {
            assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(lightBitmap)
        }
    }

    private fun runScenario(
        iconUrl: String?,
        iconRes: Int?,
        darkIconUrl: String? = null,
        forcedDarkOverride: Boolean? = null,
        block: Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        val drawable = PaymentSelection.IconLoader(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = FakeStripeImageLoader(
                loadResultByUrl = mapOf(
                    workingUrl to Result.success(simpleBitmap),
                    brokenUrl to Result.failure(Throwable()),
                    lightUrl to Result.success(lightBitmap),
                    darkUrl to Result.success(darkBitmap),
                ),
            ),
        ).load(
            drawableResourceId = iconRes ?: 0,
            drawableResourceIdNight = null,
            lightThemeIconUrl = iconUrl,
            darkThemeIconUrl = darkIconUrl,
            forcedDarkOverride = forcedDarkOverride,
        )
        advanceUntilIdle()
        Scenario(drawable = drawable).apply { block() }
    }

    private class Scenario(
        val drawable: Drawable
    )

    private companion object {
        val LINK_REF = R.drawable.stripe_ic_paymentsheet_link_ref
    }
}
