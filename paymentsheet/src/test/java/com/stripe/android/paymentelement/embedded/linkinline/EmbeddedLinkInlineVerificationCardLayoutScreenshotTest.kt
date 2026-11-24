package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.screenshottesting.LayoutDirection
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.elements.OTPElementFactory
import org.junit.Rule
import org.junit.Test

/**
 * The card in right-to-left languages and at font sizes up to the largest setting.
 */
internal class EmbeddedLinkInlineVerificationCardLayoutScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        LayoutDirection.entries,
        CardFontScale.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun testCard() {
        paparazziRule.snapshot {
            EmbeddedLinkInlineVerificationCard(
                state = EmbeddedLinkInlineVerificationCardScreenshotTest.CARD_STATE,
                otpElement = OTPElementFactory.create(),
                linkBrand = LinkBrand.Link,
                rowStyle = RowStyle.FlatWithRadio.default,
                isTyping = false,
                focusRegionModifier = Modifier,
                onCodeTapped = {},
                onDone = {},
                onTypingInterrupted = {},
                onClose = {},
                onSwitchAccount = {},
                onAction = {},
            )
        }
    }

    enum class CardFontScale(private val fontScale: Float) : PaparazziConfigOption {
        DefaultFont(fontScale = 1f),
        LargeFont(fontScale = 1.5f),
        LargestFont(fontScale = 2f);

        override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
            return deviceConfig.copy(fontScale = fontScale)
        }
    }
}
