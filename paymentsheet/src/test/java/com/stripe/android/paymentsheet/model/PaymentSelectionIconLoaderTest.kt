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
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentSelectionIconLoaderTest {

    private val workingUrl = "working url"
    private val brokenUrl = "broken url"
    private val simpleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
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

    private fun runScenario(
        iconUrl: String?,
        iconRes: Int?,
        block: Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        val drawable = PaymentSelection.IconLoader(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = mock<StripeImageLoader>().also {
                whenever(it.load(eq(workingUrl))).thenReturn(Result.success(simpleBitmap))
            }.also {
                whenever(it.load(eq(brokenUrl))).thenReturn(Result.failure(Throwable()))
            },
        ).load(
            drawableResourceId = iconRes ?: 0,
            lightThemeIconUrl = iconUrl,
            darkThemeIconUrl = null,
        )
        advanceUntilIdle()
        Scenario(drawable = drawable).apply { block() }
    }

    private class Scenario(
        val drawable: Drawable
    )
}
