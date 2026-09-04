package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal object CheckoutLpmScenarios {
    val group = group(
        "lpms",
        "LPMs",
        group(
            "north_america",
            "North America",
            regionalLeaf(
                "us_common", "US common", Merchant.US, Currency.USD,
                PaymentMethod.Type.Card, PaymentMethod.Type.USBankAccount, PaymentMethod.Type.Link,
                PaymentMethod.Type.CashAppPay, PaymentMethod.Type.Klarna,
            ),
            regionalLeaf(
                "us_alternatives", "US alternatives", Merchant.US, Currency.USD,
                PaymentMethod.Type.Card, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay,
                PaymentMethod.Type.AmazonPay, PaymentMethod.Type.Crypto, PaymentMethod.Type.Sunbit,
            ),
            regionalLeaf(
                "mexico", "Mexico", Merchant.MX, Currency.MXN,
                PaymentMethod.Type.Card, PaymentMethod.Type.Oxxo,
            ),
        ),
        group(
            "europe",
            "Europe",
            regionalLeaf(
                "euro_bank_methods", "Euro bank methods", Merchant.FR, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Ideal, PaymentMethod.Type.Bancontact,
                PaymentMethod.Type.SepaDebit, PaymentMethod.Type.Eps,
            ),
            regionalLeaf(
                "france_alternatives", "France / EU alternatives", Merchant.FR, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Alma, PaymentMethod.Type.MobilePay,
            ),
            regionalLeaf(
                "germany", "Germany", Merchant.DE, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Billie, PaymentMethod.Type.Wero,
            ),
            regionalLeaf(
                "italy", "Italy", Merchant.IT, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Satispay,
            ),
            regionalLeaf(
                "spain", "Spain", Merchant.ES, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Sequra,
            ),
            regionalLeaf(
                "poland", "Poland", Merchant.FR, Currency.PLN,
                PaymentMethod.Type.Card, PaymentMethod.Type.Blik, PaymentMethod.Type.P24,
            ),
            regionalLeaf(
                "sweden", "Sweden", Merchant.FR, Currency.SEK,
                PaymentMethod.Type.Card, PaymentMethod.Type.Swish,
            ),
            regionalLeaf(
                "uk_bank_debit", "UK bank debit", Merchant.GB, Currency.GBP,
                PaymentMethod.Type.Card, PaymentMethod.Type.BacsDebit,
            ),
            regionalLeaf(
                "gbp_wallets", "GBP wallets", Merchant.GB, Currency.GBP,
                PaymentMethod.Type.Card, PaymentMethod.Type.PayPal, PaymentMethod.Type.RevolutPay,
            ),
            regionalLeaf(
                "switzerland", "Switzerland", Merchant.GB, Currency.CHF,
                PaymentMethod.Type.Card, PaymentMethod.Type.Twint,
            ),
            regionalLeaf(
                "portugal", "Portugal", Merchant.FR, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Multibanco,
            ),
        ),
        group(
            "asia_pacific",
            "Asia-Pacific",
            regionalLeaf(
                "australia", "Australia", Merchant.AU, Currency.AUD,
                PaymentMethod.Type.Card, PaymentMethod.Type.AuBecsDebit,
            ),
            regionalLeaf(
                "singapore", "Singapore", Merchant.SG, Currency.SGD,
                PaymentMethod.Type.Card, PaymentMethod.Type.GrabPay, PaymentMethod.Type.PayNow,
            ),
            regionalLeaf(
                "malaysia", "Malaysia", Merchant.MY, Currency.MYR,
                PaymentMethod.Type.Card, PaymentMethod.Type.Fpx,
            ),
            regionalLeaf(
                "thailand", "Thailand", Merchant.TH, Currency.THB,
                PaymentMethod.Type.Card, PaymentMethod.Type.PromptPay,
            ),
            regionalLeaf(
                "japan", "Japan", Merchant.JP, Currency.JPY,
                PaymentMethod.Type.Card, PaymentMethod.Type.Konbini, PaymentMethod.Type.PayPay,
            ),
            regionalLeaf(
                "korea", "Korea", Merchant.US, Currency.KRW,
                PaymentMethod.Type.Card, PaymentMethod.Type.KrCard,
                PaymentMethod.Type.NaverPay, PaymentMethod.Type.Payco,
            ),
            regionalLeaf(
                "china", "China", Merchant.CN, Currency.EUR,
                PaymentMethod.Type.Card, PaymentMethod.Type.WeChatPay,
            ),
            regionalLeaf(
                "alipay", "Alipay", Merchant.US, Currency.USD,
                PaymentMethod.Type.Card, PaymentMethod.Type.Klarna,
                PaymentMethod.Type.Affirm, PaymentMethod.Type.Alipay,
            ),
        ),
        group(
            "latin_america",
            "Latin America",
            regionalLeaf(
                "brazil", "Brazil", Merchant.BR, Currency.BRL,
                PaymentMethod.Type.Card, PaymentMethod.Type.Boleto,
            ),
        ),
    )

    private fun regionalLeaf(
        key: String,
        name: String,
        merchant: Merchant,
        currency: Currency,
        vararg paymentMethods: PaymentMethod.Type,
    ) = leaf(key, name) {
        regional(merchant, currency, *paymentMethods)
    }
}
