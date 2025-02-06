package com.stripe.android.link.ui

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.Locale
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance
import org.junit.Rule
import org.junit.Test

private enum class LinkButtonAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    TestSurfaceBackgroundAppearance(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.defaultLight.copy(
                surface = Color.RED,
            ),
            colorsDark = PaymentSheet.Colors.defaultDark.copy(
                surface = Color.RED,
            ),
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
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

    @Test
    fun testNewUser() {
        paparazziRule.snapshot {
            LinkButton(email = null, enabled = true, onClick = { })
        }
    }

    @Test
    fun testNewUserInDifferentLocales() {
        localesPaparazziRule.snapshot {
            LinkButton(email = null, enabled = true, onClick = { })
        }
    }

    @Test
    fun testNewUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(email = null, enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUser() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom@test.com", enabled = true, onClick = { })
        }
    }

    @Test
    fun testExistingUserDisabled() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom@test.com", enabled = false, onClick = { })
        }
    }

    @Test
    fun testExistingUserWithLongEmail() {
        paparazziRule.snapshot {
            LinkButton(email = "jaynewstrom12345678987654321@test.com", enabled = true, onClick = { })
        }
    }

    @Test
    fun testRoundedCornerSurfaceColor() {
        surfacePaparazziRule.snapshot {
            LinkButton(email = null, enabled = true, onClick = { })
        }
    }
}
