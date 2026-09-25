package com.stripe.android.lpm

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DEFAULT_BILLING_ADDRESS_PHONE
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkDisplaySetting
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.TestParameters

internal fun createLinkBankSignUpTestParameters(
    email: String,
    supportedPaymentMethods: List<PaymentMethod.Type>,
): TestParameters {
    return createLinkBankTestParameters(
        paymentMethodCode = "card",
        authorizationAction = null,
        saveForFutureUseCheckboxVisible = true,
        email = email,
        phone = DEFAULT_BILLING_ADDRESS_PHONE,
        supportedPaymentMethods = supportedPaymentMethods,
    )
}

internal fun createLinkBankPaymentTestParameters(
    email: String,
    phone: String?,
    supportedPaymentMethods: List<PaymentMethod.Type>,
): TestParameters {
    return createLinkBankTestParameters(
        paymentMethodCode = "link",
        authorizationAction = AuthorizeAction.AuthorizePayment(requiresBrowser = true),
        saveForFutureUseCheckboxVisible = false,
        email = email,
        phone = phone,
        supportedPaymentMethods = supportedPaymentMethods,
    )
}

private fun createLinkBankTestParameters(
    paymentMethodCode: String,
    authorizationAction: AuthorizeAction?,
    saveForFutureUseCheckboxVisible: Boolean,
    email: String,
    phone: String?,
    supportedPaymentMethods: List<PaymentMethod.Type>,
): TestParameters {
    return TestParameters.create(
        paymentMethodCode = paymentMethodCode,
        requiresBrowser = true,
        authorizationAction = authorizationAction,
        saveForFutureUseCheckboxVisible = saveForFutureUseCheckboxVisible,
        playgroundSettingsBlock = { settings ->
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[AutomaticPaymentMethodsSettingsDefinition] = false
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(
                email = email,
                phone = phone,
            )
            settings[LinkSettingsDefinition] = LinkDisplaySetting.Automatic
            settings[SupportedPaymentMethodsSettingsDefinition] = supportedPaymentMethods.joinToString(",")
        }
    )
}
