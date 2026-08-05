package com.stripe.android.crypto.onramp.example

import androidx.compose.ui.graphics.Color
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAppearance

internal object OnrampConfigurationFactory {
    @Suppress("MagicNumber")
    fun create(): OnrampConfiguration {
        return OnrampConfiguration()
            .merchantDisplayName(merchantDisplayName = MERCHANT_NAME)
            .publishableKey(publishableKey = PUBLISHABLE_KEY)
            .appearance(
                appearance = LinkAppearance()
                    .lightColors(
                        LinkAppearance.Colors()
                            .primary(Color(0xFF635BFF))
                            .contentOnPrimary(Color.White)
                            .borderSelected(Color.Black)
                    )
                    .darkColors(
                        LinkAppearance.Colors()
                            .primary(Color(0xFF9886E6))
                            .contentOnPrimary(Color(0xFF222222))
                            .borderSelected(Color.White)
                    )
                    .style(LinkAppearance.Style.ALWAYS_DARK)
                    .primaryButton(LinkAppearance.PrimaryButton())
            )
            .googlePayConfig(
                GooglePayPaymentMethodLauncher.Config(
                    environment = GooglePayEnvironment.Test,
                    merchantCountryCode = "US",
                    merchantName = MERCHANT_NAME,
                    billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                        isRequired = true,
                        format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                        isPhoneNumberRequired = false
                    ),
                    existingPaymentMethodRequired = false
                )
            )
            .samsungPayConfig(
                OnrampConfiguration.SamsungPayConfig(
                    serviceId = SAMSUNG_PAY_SERVICE_ID,
                )
            )
    }
}

private const val MERCHANT_NAME = "Onramp Example"
private const val SAMSUNG_PAY_SERVICE_ID = "fc8bfdf3366c4c3ebe69e3"

@Suppress("MaxLineLength")
private const val PUBLISHABLE_KEY =
    "pk_live_51K9W3OHMaDsveWq0HThYw9J0urQA6s2ROY8lLdCypHrcAG38NC5lu55BHjRlxqNUDHhgYFKgAdhKQmjI1cJhRH2o00cqnSo4aR"
