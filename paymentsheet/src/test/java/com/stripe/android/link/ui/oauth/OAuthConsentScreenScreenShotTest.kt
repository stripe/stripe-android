package com.stripe.android.link.ui.oauth

import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class OAuthConsentScreenScreenShotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries
    )

    @Test
    fun testEmptyState() {
        paparazziRule.snapshot {
            OAuthConsentScreenPreview()
        }
    }
}
