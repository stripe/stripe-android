package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.model.PaymentMethod

/**
 * A playground configuration that makes a single payment method type available in PaymentSheet.
 * Each scenario sets a merchant, currency, and amount the payment method supports, and turns off automatic payment
 * methods so it's the only one offered.
 */
internal data class LpmScenario(
    val paymentMethodType: PaymentMethod.Type,
    val displayName: String,
    val merchant: Merchant,
    val currency: Currency,
    val amount: String = DEFAULT_AMOUNT,
) {
    val code: String = paymentMethodType.code

    val summary: String = "${merchant.name} merchant · $amount ${currency.displayName}"

    fun applyTo(settings: PlaygroundSettings) {
        // Merchant updates reset the currency, so set the merchant first.
        settings[MerchantSettingsDefinition] = merchant
        settings[CurrencySettingsDefinition] = currency
        settings[AmountSettingsDefinition] = amount
        settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT
        settings[AutomaticPaymentMethodsSettingsDefinition] = false
        settings[SupportedPaymentMethodsSettingsDefinition] = code
        settings[DelayedPaymentMethodsSettingsDefinition] = true
        // Offer only this payment method: no saved payment methods, EPMs, CPMs, or payment method configuration
        settings[CustomerSettingsDefinition] = CustomerType.GUEST
        settings[ExternalPaymentMethodSettingsDefinition] = ExternalPaymentMethodType.Off
        settings[CustomPaymentMethodsSettingDefinition] = CustomPaymentMethodPlaygroundType.Off
        settings[PaymentMethodConfigurationSettingsDefinition] = ""
        settings[CustomStripeApiDefinition] = ""
    }

    fun isAppliedTo(settings: Map<PlaygroundSettingDefinition<*>, Any?>): Boolean {
        return settings[AutomaticPaymentMethodsSettingsDefinition] == false &&
            settings[SupportedPaymentMethodsSettingsDefinition] == code &&
            settings[MerchantSettingsDefinition] == merchant &&
            settings[CurrencySettingsDefinition] == currency
    }

    companion object {
        private const val DEFAULT_AMOUNT = "5099"
        private const val LARGE_AMOUNT = "10000"
        private const val VERY_LARGE_AMOUNT = "1000000"

        val all: List<LpmScenario> = listOf(
            LpmScenario(PaymentMethod.Type.Affirm, "Affirm", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.AfterpayClearpay, "Afterpay / Clearpay", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.Alipay, "Alipay", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.Alma, "Alma", Merchant.FR, Currency.EUR),
            LpmScenario(PaymentMethod.Type.AmazonPay, "Amazon Pay", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.AuBecsDebit, "AU BECS Direct Debit", Merchant.AU, Currency.AUD),
            LpmScenario(PaymentMethod.Type.BacsDebit, "Bacs Direct Debit", Merchant.GB, Currency.GBP),
            LpmScenario(PaymentMethod.Type.Bancontact, "Bancontact", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Billie, "Billie", Merchant.DE, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Bizum, "Bizum", Merchant.FR, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Blik, "BLIK", Merchant.FR, Currency.PLN),
            LpmScenario(PaymentMethod.Type.Boleto, "Boleto", Merchant.BR, Currency.BRL),
            LpmScenario(PaymentMethod.Type.Card, "Card", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.CashAppPay, "Cash App Pay", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.Crypto, "Crypto", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.Eps, "EPS", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Fpx, "FPX", Merchant.MY, Currency.MYR),
            LpmScenario(PaymentMethod.Type.GCash, "GCash", Merchant.US, Currency.PHP, LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.GoPay, "GoPay", Merchant.US, Currency.IDR, VERY_LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.GrabPay, "GrabPay", Merchant.SG, Currency.SGD),
            LpmScenario(PaymentMethod.Type.Ideal, "iDEAL", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.KakaoPay, "Kakao Pay", Merchant.US, Currency.KRW),
            LpmScenario(PaymentMethod.Type.Klarna, "Klarna", Merchant.FR, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Konbini, "Konbini", Merchant.JP, Currency.JPY),
            LpmScenario(PaymentMethod.Type.KrCard, "Korean Card", Merchant.US, Currency.KRW),
            LpmScenario(PaymentMethod.Type.MbWay, "MB WAY", Merchant.FR, Currency.EUR),
            LpmScenario(PaymentMethod.Type.MobilePay, "MobilePay", Merchant.FR, Currency.DKK),
            LpmScenario(PaymentMethod.Type.MoMo, "MoMo", Merchant.US, Currency.VND, VERY_LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.Mondu, "Mondu", Merchant.MONDU, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Multibanco, "Multibanco", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.NaverPay, "Naver Pay", Merchant.US, Currency.KRW),
            LpmScenario(
                PaymentMethod.Type.NgBankTransfer,
                "Bank Transfer (NG)",
                Merchant.NG,
                Currency.NGN,
                VERY_LARGE_AMOUNT
            ),
            LpmScenario(PaymentMethod.Type.NgCard, "Naira Card", Merchant.NG, Currency.NGN, VERY_LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.NgUssd, "USSD", Merchant.NG, Currency.NGN, VERY_LARGE_AMOUNT),
            LpmScenario(
                PaymentMethod.Type.NgWallet,
                "Naira Wallet",
                Merchant.NG_WALLET,
                Currency.NGN,
                VERY_LARGE_AMOUNT
            ),
            LpmScenario(PaymentMethod.Type.Oxxo, "OXXO", Merchant.MX, Currency.MXN),
            LpmScenario(PaymentMethod.Type.PayByBank, "Pay by Bank", Merchant.GB, Currency.GBP),
            LpmScenario(PaymentMethod.Type.Payco, "PAYCO", Merchant.US, Currency.KRW),
            LpmScenario(PaymentMethod.Type.PayNow, "PayNow", Merchant.SG, Currency.SGD),
            LpmScenario(PaymentMethod.Type.PayPal, "PayPal", Merchant.FR, Currency.EUR),
            LpmScenario(PaymentMethod.Type.PayPay, "PayPay", Merchant.JP, Currency.JPY),
            LpmScenario(PaymentMethod.Type.Pix, "Pix", Merchant.US, Currency.BRL),
            LpmScenario(PaymentMethod.Type.PromptPay, "PromptPay", Merchant.TH, Currency.THB),
            LpmScenario(PaymentMethod.Type.P24, "Przelewy24", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Qris, "QRIS", Merchant.US, Currency.IDR, VERY_LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.RevolutPay, "Revolut Pay", Merchant.GB, Currency.GBP),
            LpmScenario(PaymentMethod.Type.Satispay, "Satispay", Merchant.IT, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Scalapay, "Scalapay", Merchant.IT, Currency.EUR, LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.SepaDebit, "SEPA Debit", Merchant.US, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Sequra, "seQura", Merchant.ES, Currency.EUR, LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.ShopeePay, "ShopeePay", Merchant.US, Currency.IDR, VERY_LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.Sunbit, "Sunbit", Merchant.US, Currency.USD, LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.Swish, "Swish", Merchant.FR, Currency.SEK),
            LpmScenario(PaymentMethod.Type.TouchNGo, "Touch 'n Go", Merchant.US, Currency.MYR),
            LpmScenario(PaymentMethod.Type.TrueMoney, "TrueMoney", Merchant.US, Currency.THB, LARGE_AMOUNT),
            LpmScenario(PaymentMethod.Type.Twint, "TWINT", Merchant.GB, Currency.CHF),
            LpmScenario(PaymentMethod.Type.USBankAccount, "US Bank Account", Merchant.US, Currency.USD),
            LpmScenario(PaymentMethod.Type.WeChatPay, "WeChat Pay", Merchant.CN, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Wero, "Wero", Merchant.DE, Currency.EUR),
            LpmScenario(PaymentMethod.Type.Zip, "Zip", Merchant.AU, Currency.AUD),
        ).sortedBy { it.displayName.lowercase() }
    }
}
