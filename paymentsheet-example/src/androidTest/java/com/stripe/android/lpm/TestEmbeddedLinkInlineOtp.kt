package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentelement.embedded.linkinline.resetEmbeddedLinkInlineAuthRegistry
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition.FormSheetAction
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedLinkInlineOtpSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedLinkInlineOtpSettingsDefinition.InlineOtp
import com.stripe.android.paymentsheet.example.playground.settings.LinkDisplaySetting
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.LinkTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Inline Link verification in the Embedded Payment Element, for the playground's returning Link customer.
 */
@RunWith(AndroidJUnit4::class)
internal class TestEmbeddedLinkInlineOtp : BasePlaygroundTest() {
    private val parameters = TestParameters.create(
        paymentMethodCode = "link",
        authorizationAction = null,
    ) { settings ->
        settings[MerchantSettingsDefinition] = Merchant.US
        settings[SupportedPaymentMethodsSettingsDefinition] = "card,link"
        settings[LinkSettingsDefinition] = LinkDisplaySetting.Automatic
        settings[LinkTypeSettingsDefinition] = LinkType.Native
        // A returning Link customer, without saved payment methods.
        settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
        settings[EmbeddedFormSheetActionSettingDefinition] = FormSheetAction.Continue
        settings[EmbeddedLinkInlineOtpSettingsDefinition] = InlineOtp.On
    }

    @Before
    fun resetInlineLinkVerification() {
        // Codes sent by earlier tests in this process would otherwise keep the card from showing.
        resetEmbeddedLinkInlineAuthRegistry()
    }

    @Test
    fun testCodeTypedInTheCard() {
        testDriver.confirmEmbeddedWithLink(
            testParameters = parameters,
            beforeCheckout = {
                inlineVerification.waitUntilVisible()
                inlineVerification.waitUntilCodeIsSent()
                inlineVerification.tapCode()
                inlineVerification.typeCode(CODE)

                // Verifying opens the Link wallet to pick what to pay with.
                pressLinkWalletButton()
                waitUntilLinkIsSelected()
                inlineVerification.assertIsNotVisible()
            },
        )
    }

    @Test
    fun testCodeEnteredInTheDialogWhenTheServerForcesIt() {
        testDriver.confirmEmbeddedWithLink(
            testParameters = parameters.copyPlaygroundSettings { settings ->
                settings[EmbeddedLinkInlineOtpSettingsDefinition] = InlineOtp.ForceDialog
            },
            beforeCheckout = {
                inlineVerification.waitUntilVisible()
                inlineVerification.waitUntilCodeIsSent()
                inlineVerification.tapCode()
                inlineVerification.enterCodeInDialog(CODE)

                pressLinkWalletButton()
                waitUntilLinkIsSelected()
            },
        )
    }

    @Test
    fun testClosedCardVerifiesInTheDialogBeforePaying() {
        testDriver.confirmEmbeddedWithLink(
            testParameters = parameters,
            beforeCheckout = {
                inlineVerification.waitUntilVisible()
                inlineVerification.waitUntilCodeIsSent()
                inlineVerification.close()
                inlineVerification.waitUntilMissing()

                // The Link row stays first, and only selects Link.
                selectLink()
                waitUntilLinkIsSelected()
            },
            afterCheckout = {
                // Paying with Link verifies in the dialog first, with the code the card sent.
                inlineVerification.enterCodeInDialog(CODE)
                pressLinkWalletButton()
            },
        )
    }

    private companion object {
        /**
         * Test mode's verification code.
         */
        const val CODE = "000000"
    }
}
