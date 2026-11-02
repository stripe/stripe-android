package com.stripe.android.link.ui.nofundingsources

import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class NoFundingSourcesScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            NoFundingSourcesScreenPreview()
        }
    }

    @Test
    fun testCannotContinueWithoutLink() {
        paparazziRule.snapshot {
            NoFundingSourcesScreenCannotContinueWithoutLinkPreview()
        }
    }
}
