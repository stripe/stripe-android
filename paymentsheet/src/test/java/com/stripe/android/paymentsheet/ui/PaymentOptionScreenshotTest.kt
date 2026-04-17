
package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class PaymentOptionScreenshotTest {

    @get:Rule
    val cardArtFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableCardArt,
        isEnabled = false
    )

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun testEnabled() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testDisabled() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = false,
        )
    }

    @Test
    fun testSelected() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testSelectedAndDisabled() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = false,
        )
    }

    @Test
    fun testModifying() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testDefaultEditing() {
        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = true,
            isEnabled = true,
        )
    }

    @Test
    fun testWithCardArt() {
        cardArtFeatureFlagRule.setEnabled(true)

        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
            cardArtUrl = SAMPLE_CARD_ART_URL,
        )
    }

    @Test
    fun testWithCardArt_selected() {
        cardArtFeatureFlagRule.setEnabled(true)

        createSavedPaymentMethodTabScreenshot(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
            cardArtUrl = SAMPLE_CARD_ART_URL,
        )
    }

    @Test
    fun testEnabledWithCardArtFlagEnabled() {
        cardArtFeatureFlagRule.setEnabled(true)

        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testDisabledWithCardArtFlagEnabled() {
        cardArtFeatureFlagRule.setEnabled(true)

        createSavedPaymentMethodTabScreenshot(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = false,
        )
    }

    @Test
    fun testSelectedWithCardArtFlagEnabled() {
        cardArtFeatureFlagRule.setEnabled(true)

        createSavedPaymentMethodTabScreenshot(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    private fun createSavedPaymentMethodTabScreenshot(
        isSelected: Boolean,
        shouldShowModifyBadge: Boolean,
        shouldShowDefaultBadge: Boolean,
        isEnabled: Boolean,
        cardArtUrl: String? = null,
    ) {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                isSelected = isSelected,
                shouldShowModifyBadge = shouldShowModifyBadge,
                shouldShowDefaultBadge = shouldShowDefaultBadge,
                isEnabled = isEnabled,
                viewWidth = 160.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
                cardArtUrl = cardArtUrl,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    private companion object {
        const val SAMPLE_CARD_ART_URL =
            "https://b.stripecdn.com/cardart/assets/pfE0FkDGaiFhdoOj9to8po-ZLiJhetgfdKELIZCj3xA"
    }
}
