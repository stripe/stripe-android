package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
    fun loadPaymentOptionWithIconUrl_usesIconFromUrl() = runTest(testDispatcher) {
        val paymentOptionIcon = createPaymentOptionForIconTesting(
            iconUrl = workingUrl,
            iconRes = R.drawable.stripe_ic_paymentsheet_link,
            scope = this,
            dispatcher = testDispatcher,
        ).icon()

        advanceUntilIdle()

        assertThat(paymentOptionIcon.current).isInstanceOf<BitmapDrawable>()
        assertThat((paymentOptionIcon.current as BitmapDrawable).bitmap).isEqualTo(simpleBitmap)
    }

    @Test
    fun loadPaymentOptionWithIconUrl_failsToLoad_usesIconFromRes() = runTest(testDispatcher) {
        val paymentOptionIcon = createPaymentOptionForIconTesting(
            iconUrl = brokenUrl,
            iconRes = R.drawable.stripe_ic_paymentsheet_link,
            scope = this,
            dispatcher = testDispatcher,
        ).icon()

        advanceUntilIdle()

        assertThat(paymentOptionIcon.current).isInstanceOf<VectorDrawable>()
    }

    @Test
    fun loadPaymentOptionWithIconUrl_failsToLoad_missingIconRes_usesEmptyDrawable() = runTest(testDispatcher) {
        val paymentOptionIcon = createPaymentOptionForIconTesting(
            iconUrl = brokenUrl,
            iconRes = null,
            scope = this,
            dispatcher = testDispatcher,
        ).icon()

        advanceUntilIdle()

        assertThat(paymentOptionIcon.current).isInstanceOf<ShapeDrawable>()
        assertThat(paymentOptionIcon.current).isEqualTo(PaymentSelection.IconLoader.emptyDrawable)
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_usesIconFromRes() = runTest(testDispatcher) {
        val paymentOptionIcon = createPaymentOptionForIconTesting(
            iconUrl = null,
            iconRes = R.drawable.stripe_ic_paymentsheet_link,
            scope = this,
            dispatcher = testDispatcher,
        ).icon()

        advanceUntilIdle()

        assertThat(paymentOptionIcon.current).isInstanceOf<VectorDrawable>()
    }

    @Test
    fun loadPaymentOptionWithoutIconUrl_missingIconRes_usesEmptyDrawable() = runTest(testDispatcher) {
        val paymentOptionIcon = createPaymentOptionForIconTesting(
            iconUrl = null,
            iconRes = null,
            scope = this,
            dispatcher = testDispatcher,
        ).icon()

        advanceUntilIdle()

        assertThat(paymentOptionIcon.current).isInstanceOf<ShapeDrawable>()
        assertThat(paymentOptionIcon.current).isEqualTo(PaymentSelection.IconLoader.emptyDrawable)
    }

    private suspend fun createPaymentOptionForIconTesting(
        iconUrl: String?,
        iconRes: Int?,
        scope: CoroutineScope,
        dispatcher: TestDispatcher,
    ): PaymentOption {
        val iconLoader = PaymentSelection.IconLoader(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = mock<StripeImageLoader>().also {
                whenever(it.load(eq(workingUrl))).thenReturn(Result.success(simpleBitmap))
            }.also {
                whenever(it.load(eq(brokenUrl))).thenReturn(Result.failure(Throwable()))
            },
        )
        return PaymentOption(
            drawableResourceId = iconRes ?: 0,
            lightThemeIconUrl = iconUrl,
            darkThemeIconUrl = null,
            label = "unused",
            imageLoader = iconLoader::loadPaymentOption,
            delegateDrawableScope = scope,
            delegateDrawableDispatcher = dispatcher,
        )
    }
}
