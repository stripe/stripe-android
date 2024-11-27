package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import java.util.Locale

internal class PromoBadgeScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier.padding(16.dp)
    )

    @Test
    fun testPromoBadge() {
        paparazziRule.snapshot {
            PromoBadge(
                text = "$5",
                tinyMode = false,
            )
        }
    }

    @Test
    fun testPromoBadgeInTinyMode() {
        paparazziRule.snapshot {
            PromoBadge(
                text = "$5",
                tinyMode = true,
            )
        }
    }

    @Test
    fun testPromoBadgeInTinyModeWithNonEnglish() {
        withLocale(Locale.GERMAN) {
            paparazziRule.snapshot {
                PromoBadge(
                    text = "$5",
                    tinyMode = true,
                )
            }
        }
    }
}

private fun withLocale(locale: Locale, block: () -> Unit) {
    val original = Locale.getDefault()
    Locale.setDefault(locale)

    try {
        block()
    } finally {
        Locale.setDefault(original)
    }
}
