package com.stripe.android.link

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.link.ui.LinkLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class LinkButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        arrayOf(SystemAppearance.LightTheme),
        FontSize.values(),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            LinkButton(label = LinkLabel.Default, enabled = true, onClick = {})
        }
    }

    @Test
    fun testDefaultDisabled() {
        paparazziRule.snapshot {
            LinkButton(label = LinkLabel.Default, enabled = false, onClick = {})
        }
    }

    @Test
    fun testEmail() {
        paparazziRule.snapshot {
            LinkButton(label = LinkLabel.Email(email = "jaynewstrom@test.com"), enabled = true, onClick = {})
        }
    }

    @Test
    fun testEmailDisabled() {
        paparazziRule.snapshot {
            LinkButton(label = LinkLabel.Email(email = "jaynewstrom@test.com"), enabled = false, onClick = {})
        }
    }

    @Test
    fun testEmailWithLongEmail() {
        paparazziRule.snapshot {
            LinkButton(
                label = LinkLabel.Email(email = "jaynewstrom12345678987654321@test.com"),
                enabled = true,
                onClick = {},
            )
        }
    }

    @Test
    fun testEmailWithLongEmailDisabled() {
        paparazziRule.snapshot {
            LinkButton(
                label = LinkLabel.Email(email = "jaynewstrom12345678987654321@test.com"),
                enabled = false,
                onClick = {},
            )
        }
    }

    @Test
    fun testCard() {
        paparazziRule.snapshot {
            LinkButton(
                label = LinkLabel.Card(
                    icon = CardBrand.Visa.icon,
                    lastFourDigits = "3155",
                ),
                enabled = false,
                onClick = {},
            )
        }
    }

    @Test
    fun testCardDisabled() {
        paparazziRule.snapshot {
            LinkButton(
                label = LinkLabel.Card(
                    icon = CardBrand.Visa.icon,
                    lastFourDigits = "3155",
                ),
                enabled = false,
                onClick = {},
            )
        }
    }
}
