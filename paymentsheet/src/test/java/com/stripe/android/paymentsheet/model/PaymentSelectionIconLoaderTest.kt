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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class PaymentSelectionIconLoaderTest {

    private val workingUrl = "working url"
    private val brokenUrl = "broken url"
    private val darkUrl = "dark url"
    private val simpleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    private val darkBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
    private val testDispatcher = StandardTestDispatcher()

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
    fun loadPaymentOptionWithIconUrl_explicitDark_usesDarkIconFromUrl() = runExplicitScenario(
        iconUrl = workingUrl,
        darkIconUrl = darkUrl,
        iconRes = R.drawable.stripe_ic_paymentsheet_card_unknown_day,
        iconResNight = R.drawable.stripe_ic_paymentsheet_card_unknown_night,
        useDarkThemeIcon = true,
    ) {
        assertThat(drawable.current).isInstanceOf<BitmapDrawable>()
        assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(darkBitmap)
        assertThat(loadedUrl).isEqualTo(darkUrl)
    }

    @Test
    fun loadPaymentOptionWithIconUrl_explicitLight_usesLightIconFromUrl() = runExplicitScenario(
        iconUrl = workingUrl,
        darkIconUrl = darkUrl,
        iconRes = R.drawable.stripe_ic_paymentsheet_card_unknown_day,
        iconResNight = R.drawable.stripe_ic_paymentsheet_card_unknown_night,
        useDarkThemeIcon = false,
    ) {
        assertThat(drawable.current).isInstanceOf<BitmapDrawable>()
        assertThat((drawable.current as BitmapDrawable).bitmap).isEqualTo(simpleBitmap)
        assertThat(loadedUrl).isEqualTo(workingUrl)
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_explicitDark_usesNightResource() = runExplicitScenario(
        iconUrl = null,
        darkIconUrl = null,
        iconRes = R.drawable.stripe_ic_paymentsheet_card_unknown_day,
        iconResNight = R.drawable.stripe_ic_paymentsheet_card_unknown_night,
        useDarkThemeIcon = true,
    ) {
        assertThat(shadowOf(drawable.current).createdFromResId)
            .isEqualTo(R.drawable.stripe_ic_paymentsheet_card_unknown_night)
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_explicitLight_usesDayResource() = runExplicitScenario(
        iconUrl = null,
        darkIconUrl = null,
        iconRes = R.drawable.stripe_ic_paymentsheet_card_unknown_day,
        iconResNight = R.drawable.stripe_ic_paymentsheet_card_unknown_night,
        useDarkThemeIcon = false,
    ) {
        assertThat(shadowOf(drawable.current).createdFromResId)
            .isEqualTo(R.drawable.stripe_ic_paymentsheet_card_unknown_day)
    }

    private fun runScenario(
        iconUrl: String?,
        iconRes: Int?,
        block: Scenario.() -> Unit,
    ) = runScenario(
        iconUrl = iconUrl,
        darkIconUrl = null,
        iconRes = iconRes,
        iconResNight = null,
        useDarkThemeIcon = null,
        block = block,
    )

    private fun runExplicitScenario(
        iconUrl: String?,
        darkIconUrl: String?,
        iconRes: Int?,
        iconResNight: Int?,
        useDarkThemeIcon: Boolean,
        block: Scenario.() -> Unit,
    ) = runScenario(
        iconUrl = iconUrl,
        darkIconUrl = darkIconUrl,
        iconRes = iconRes,
        iconResNight = iconResNight,
        useDarkThemeIcon = useDarkThemeIcon,
        block = block,
    )

    private fun runScenario(
        iconUrl: String?,
        darkIconUrl: String?,
        iconRes: Int?,
        iconResNight: Int?,
        useDarkThemeIcon: Boolean?,
        block: Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        val imageLoader = FakeStripeImageLoader(
            loadResultByUrl = mapOf(
                workingUrl to Result.success(simpleBitmap),
                darkUrl to Result.success(darkBitmap),
                brokenUrl to Result.failure(Throwable()),
            ),
        )
        val iconLoader = PaymentSelection.IconLoader(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = imageLoader,
        )
        val drawable = iconLoader.load(
            drawableResourceId = iconRes ?: 0,
            drawableResourceIdNight = iconResNight,
            lightThemeIconUrl = iconUrl,
            darkThemeIconUrl = darkIconUrl,
            useDarkThemeIcon = useDarkThemeIcon,
        )
        advanceUntilIdle()

        val expectedUrl = if (useDarkThemeIcon == true && darkIconUrl != null) darkIconUrl else iconUrl
        val loadedUrl = expectedUrl?.let { imageLoader.awaitLoadCall().url }
        Scenario(drawable = drawable, loadedUrl = loadedUrl).apply { block() }
        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val drawable: Drawable,
        val loadedUrl: String?,
    )
}
