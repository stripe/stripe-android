package com.stripe.android.link.ui.oauth

import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.PaparazziTest
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(PaparazziTest::class)
class OAuthConsentScreenScreenShotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries
    )

    @Test
    fun testContent() {
        paparazziRule.snapshot {
            OAuthConsentScreenPreview()
        }
    }
}
