package com.stripe.android.lpm

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomPaymentMethodPlaygroundType
import com.stripe.android.paymentsheet.example.playground.settings.CustomPaymentMethodsSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DEFAULT_CUSTOM_PAYMENT_METHOD_ID
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultShippingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.samples.ui.shared.PAYMENT_METHOD_ICON_TEST_TAG
import com.stripe.android.paymentsheet.example.samples.ui.shared.PAYMENT_METHOD_SELECTOR_TEST_TAG
import com.stripe.android.test.core.TestParameters
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.paymentsheet.R as PaymentSheetR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@RunWith(TestParameterInjector::class)
internal class TestPaymentOptionIcons : BasePlaygroundTest() {
    @Test
    fun testPaymentOptionIconChanges(@TestParameter transition: IconTransition) {
        val options = listOf(transition.first, transition.second)
        val testParameters = TestParameters.create(
            paymentMethodCode = transition.first.code,
            requiresBrowser = false,
            authorizationAction = null,
        ) { settings ->
            settings[CustomerSettingsDefinition] = CustomerType.GUEST
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
            settings[DefaultShippingAddressSettingsDefinition] = Option.Affirm in options
            settings[SupportedPaymentMethodsSettingsDefinition] = options
                .filter { it != Option.BufoPay }
                .map { it.code }
                .distinct()
                .joinToString(",")
            settings[CustomPaymentMethodsSettingDefinition] = if (Option.BufoPay in options) {
                CustomPaymentMethodPlaygroundType.On
            } else {
                CustomPaymentMethodPlaygroundType.Off
            }
        }

        // Reload only once. The host's iconPainter call site must survive every selection change.
        testDriver.startCustomFlow(testParameters)

        val firstIcon = selectAndCaptureIcon(transition.first)
        val secondIcon = selectAndCaptureIcon(transition.second)
        assertThat(secondIcon.sameAs(firstIcon)).isFalse()

        val restoredIcon = selectAndCaptureIcon(transition.first)
        assertThat(restoredIcon.sameAs(firstIcon)).isTrue()
    }

    private fun selectAndCaptureIcon(option: Option): Bitmap {
        testDriver.selectCustomPaymentOption(option.code) { selectors ->
            option.cardNumber?.let { cardNumber ->
                // Reopening a new-card selection prefills its fields, so replace instead of append.
                selectors.getCardNumber().performTextReplacement(cardNumber)
                selectors.getCardExpiration().performTextReplacement("1234")
                selectors.getCardCvc().performTextReplacement("123")
            }
        }

        val composeRule = rules.compose
        val selectedOption = hasTestTag(PAYMENT_METHOD_SELECTOR_TEST_TAG)
            .and(hasText(option.label, substring = true))
        composeRule.waitUntilWithIdle("host payment option label to contain ${option.label}") {
            composeRule.onAllNodes(selectedOption)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(PAYMENT_METHOD_SELECTOR_TEST_TAG)
            .assertTextContains(option.label, substring = true)

        val icon = composeRule.onNodeWithTag(PAYMENT_METHOD_ICON_TEST_TAG, useUnmergedTree = true)
        var loadedIcon: Bitmap? = null
        composeRule.waitUntilWithIdle("${option.label} icon to finish loading") {
            val bitmap = icon.captureToImage().asAndroidBitmap()
            bitmap.hasVisibleContent().also { loaded ->
                if (loaded) loadedIcon = bitmap
            }
        }
        val bitmap = requireNotNull(loadedIcon)

        option.drawableRes?.let { drawableRes ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val drawable = requireNotNull(context.getDrawable(drawableRes))
            assertThat(bitmap.width).isEqualTo(drawable.intrinsicWidth)
            assertThat(bitmap.height).isEqualTo(drawable.intrinsicHeight)
        }

        return bitmap
    }

    private fun Bitmap.hasVisibleContent(): Boolean {
        // DelegateDrawable starts with an empty 1x1 drawable. Neither that nor a blank failed
        // image load should become the baseline, especially for the downloaded BufoPay logo.
        if (width <= 1 || height <= 1) return false
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.any { it != pixels.first() }
    }

    enum class IconTransition(val first: Option, val second: Option) {
        VisaToMastercard(Option.Visa, Option.Mastercard),
        VisaToCashApp(Option.Visa, Option.CashApp),
        CashAppToAffirm(Option.CashApp, Option.Affirm),
        KlarnaToVisa(Option.Klarna, Option.Visa),
        BufoPayToVisa(Option.BufoPay, Option.Visa),
    }

    enum class Option(
        val code: String,
        val label: String,
        val cardNumber: String?,
        @get:DrawableRes val drawableRes: Int?,
    ) {
        Visa(
            code = "card",
            label = "4242",
            cardNumber = "4242424242424242",
            drawableRes = PaymentSheetR.drawable.stripe_ic_paymentsheet_card_visa_ref,
        ),
        Mastercard(
            code = "card",
            label = "4444",
            cardNumber = "5555555555554444",
            drawableRes = PaymentSheetR.drawable.stripe_ic_paymentsheet_card_mastercard_ref,
        ),
        CashApp(
            code = "cashapp",
            label = "Cash App Pay",
            cardNumber = null,
            drawableRes = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
        ),
        Affirm(
            code = "affirm",
            label = "Affirm",
            cardNumber = null,
            drawableRes = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_affirm,
        ),
        Klarna(
            code = "klarna",
            label = "Klarna",
            cardNumber = null,
            drawableRes = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
        ),
        BufoPay(
            code = DEFAULT_CUSTOM_PAYMENT_METHOD_ID,
            label = "BufoPay",
            cardNumber = null,
            drawableRes = null,
        ),
    }
}
