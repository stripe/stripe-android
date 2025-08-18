package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.wallet.toDefaultPaymentUI
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.Locale
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance
import org.junit.Rule
import org.junit.Test

private enum class LinkButtonAppearance(private val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    TestSurfaceBackgroundAppearance(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(surface = Color.Red),
            colorsDark = PaymentSheet.Colors.configureDefaultDark(surface = Color.Red),
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}

private enum class LinkButtonThemes(val buttonThemes: PaymentSheet.ButtonThemes) : PaparazziConfigOption {

    DefaultTheme(
        buttonThemes = PaymentSheet.ButtonThemes(
            link = LinkButtonTheme.DEFAULT
        )
    ),

    WhiteTheme(
        buttonThemes = PaymentSheet.ButtonThemes(
            link = LinkButtonTheme.WHITE
        )
    );

    override fun initialize() {
        currentTheme = buttonThemes.link
    }

    override fun reset() {
        currentTheme = LinkButtonTheme.DEFAULT
    }

    companion object {
        var currentTheme: LinkButtonTheme = LinkButtonTheme.DEFAULT
    }
}

internal class LinkButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val localesPaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        Locale.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val surfacePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        LinkButtonAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val themesPaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        LinkButtonThemes.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testNewUser() {
        paparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Default, enabled = true, onClick = { })
        }
    }

    @Test
    fun testNewUserInDifferentLocales() {
        localesPaparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Default, enabled = true, onClick = { })
        }
    }

    @Test
    fun testNewUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Default, enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUser() {
        paparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Email("jaynewstrom@test.com"), enabled = true, onClick = { })
        }
    }

    @Test
    fun testExistingUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Email("jaynewstrom@test.com"), enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUserWithLongEmail() {
        paparazziRule.snapshot {
            LinkButton(
                state = LinkButtonState.Email(email = "jaynewstrom12345678987654321@test.com"),
                enabled = true,
                onClick = { }
            )
        }
    }

    @Test
    fun testExistingUserWithLongEmailDisabled() {
        paparazziRule.snapshot {
            LinkButton(
                state = LinkButtonState.Email("jaynewstrom12345678987654321@test.com"),
                enabled = false,
                onClick = { }
            )
        }
    }

    @Test
    fun testRoundedCornerSurfaceColor() {
        surfacePaparazziRule.snapshot {
            LinkButton(state = LinkButtonState.Default, enabled = true, onClick = { })
        }
    }

    @Test
    fun testPaymentMethodDisplayed() {
        paparazziRule.snapshot {
            LinkButton(
                state = LinkButtonState.DefaultPayment(
                    paymentUI = DisplayablePaymentDetails(
                        defaultPaymentType = "CARD",
                        defaultCardBrand = "visa",
                        last4 = "4242"
                    ).toDefaultPaymentUI(true)!!,
                ),
                enabled = true,
                onClick = { }
            )
        }
    }
}
