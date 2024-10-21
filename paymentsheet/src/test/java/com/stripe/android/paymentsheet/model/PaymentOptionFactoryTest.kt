package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PaymentOptionFactoryTest {

    private val workingUrl = "working url"
    private val brokenUrl = "broken url"
    private val simpleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    private val testDispatcher = StandardTestDispatcher()

    private val factory = PaymentOptionFactory(
        ApplicationProvider.getApplicationContext<Context>().resources,
        StripeImageLoader(ApplicationProvider.getApplicationContext()),
        ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun `create() with GooglePay should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.GooglePay
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
    }

    @Test
    fun `create() with card PaymentMethod should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
    }

    @Test
    fun `create() with card params should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
    }

    @Test
    fun `create() with saved card params with known brand from wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(CardBrand.Visa),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
    }

    @Test
    fun `create() with saved card params with unknown brand from Link wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(),
                walletType = PaymentSelection.Saved.WalletType.Link
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
    }

    @Test
    fun `create() with saved card params without last 4 digits from Link wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(last4 = null),
                walletType = PaymentSelection.Saved.WalletType.Link
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link)
        assertThat(paymentOption.label).isEqualTo("Link")
    }

    @Test
    fun `create() with saved card params with unknown brand from Google wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
    }

    @Test
    fun `create() with saved card params without last 4 digits from Google wallet should return expected object`() {
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                paymentMethod = card(last4 = null),
                walletType = PaymentSelection.Saved.WalletType.GooglePay
            )
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
    }

    private fun card(
        brand: CardBrand = CardBrand.Unknown,
        last4: String? = "4242"
    ): PaymentMethod {
        return PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(PaymentMethod.Card(last4 = last4, brand = brand, displayBrand = brand.code))
            .build()
    }

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
        assertThat(paymentOptionIcon.current).isEqualTo(PaymentOptionFactory.emptyDrawable)
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
        assertThat(paymentOptionIcon.current).isEqualTo(PaymentOptionFactory.emptyDrawable)
    }

    private suspend fun createPaymentOptionForIconTesting(
        iconUrl: String?,
        iconRes: Int?,
        scope: CoroutineScope,
        dispatcher: TestDispatcher,
    ): PaymentOption {
        val factory = PaymentOptionFactory(
            ApplicationProvider.getApplicationContext<Context>().resources,
            imageLoader = mock<StripeImageLoader>().also {
                whenever(it.load(eq(workingUrl))).thenReturn(Result.success(simpleBitmap))
            }.also {
                whenever(it.load(eq(brokenUrl))).thenReturn(Result.failure(Throwable()))
            },
            ApplicationProvider.getApplicationContext(),
        )
        return PaymentOption(
            drawableResourceId = iconRes ?: 0,
            lightThemeIconUrl = iconUrl,
            darkThemeIconUrl = null,
            label = "unused",
            imageLoader = factory::loadPaymentOption,
            delegateDrawableScope = scope,
            delegateDrawableDispatcher = dispatcher,
        )
    }
}
