@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview

data class AppearanceSettings(
    val fontSettings: FontSettings = FontSettings(),
    val colorsSettings: ColorsSettings = ColorsSettings(),
    val themeSettings: PaymentMethodMessagingElement.Appearance.Theme = PaymentMethodMessagingElement.Appearance.Theme.LIGHT
) {

    fun toAppearance(): PaymentMethodMessagingElement.Appearance {
        val font = PaymentMethodMessagingElement.Appearance.Font()
            .fontSizeSp(fontSettings.fontSize)
            .fontFamily(fontSettings.fontFamily)
            .letterSpacingSp(fontSettings.letterSpacing)
            .fontWeight(fontSettings.fontWeight)
        val colors = PaymentMethodMessagingElement.Appearance.Colors()
            .textColor(colorsSettings.textColor.color.toArgb())
            .infoIconColor(colorsSettings.iconColor.color.toArgb())
        return PaymentMethodMessagingElement.Appearance()
            .font(font)
            .colors(colors)
            .theme(themeSettings)
    }
}

data class ColorsSettings(
    val iconColor: ColorInfo = ColorInfo(Color.Black, "Black"),
    val textColor: ColorInfo = ColorInfo(Color.Black, "Black")
)

data class ColorInfo(
    val color: Color,
    val name: String
)

data class FontSettings(
    val fontFamily: Int? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val letterSpacing: Float? = null,
    val label: String = "Normal"
)

data class PublishableKeySetting(
    val key: String,
    val label: String
)

val pkList = listOf(
    PublishableKeySetting(
        key = "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C",
        label = "US"
    ),
    PublishableKeySetting(
        key = "pk_test_51KmkHbGoesj9fw9QAZJlz1qY4dns8nFmLKc7rXiWKAIj8QU7NPFPwSY1h8mqRaFRKQ9njs9pVJoo2jhN6ZKSDA4h00mjcbGF7b",
        label = "GB"
    ),
    PublishableKeySetting(
        key = "pk_test_51JtgfQKG6vc7r7YCU0qQNOkDaaHrEgeHgGKrJMNfuWwaKgXMLzPUA1f8ZlCNPonIROLOnzpUnJK1C1xFH3M3Mz8X00Q6O4GfUt",
        label = "FR"
    ),
    PublishableKeySetting(
        key = "pk_test_51KaoFxCPXw4rvZpfi7MgGvQHAyqydlZgq7qfazb65457ApNZVN12LdVmiZh0bmDfgBEDUlXtSM72F9rPweMN0QJP00hVaYXMkx",
        label = "AU"
    ),
    PublishableKeySetting(
        key = "",
        label = "Blank"
    )
)