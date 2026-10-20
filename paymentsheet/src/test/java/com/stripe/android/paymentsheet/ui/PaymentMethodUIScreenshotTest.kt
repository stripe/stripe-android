package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(TestParameterInjector::class)
internal class PaymentMethodUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @get:Rule
    val themeBasedIconsPaparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        listOf(PaymentSheetAppearance.CrazyAppearance),
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @get:Rule
    val disableLuminanceBasedThemesRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.disableLuminanceBasedThemes,
        isEnabled = false,
    )

    @Test
    fun testSelectedStateForTabMode() {
        snapshot(TabState.Selected, paparazziRule)
    }

    @Test
    fun testUnselectedStateForTabMode() {
        snapshot(TabState.Unselected, paparazziRule)
    }

    @Test
    fun testUnselectedStateWithBadgeForTabMode() {
        snapshot(TabState.UnselectedWithBadge, paparazziRule)
    }

    @Test
    fun testDisabledForTabMode() {
        snapshot(TabState.Disabled, paparazziRule)
    }

    @Test
    fun testDisabledWithBadgeForTabMode() {
        snapshot(TabState.DisabledWithBadge, paparazziRule)
    }

    @Test
    fun testLongTitleForTabMode() {
        snapshot(TabState.LongTitle, paparazziRule)
    }

    @Test
    fun testThemeBasedIcons(@TestParameter state: TabState) {
        disableLuminanceBasedThemesRule.setEnabled(true)
        snapshot(state, themeBasedIconsPaparazziRule)
    }

    private fun snapshot(state: TabState, rule: PaparazziRule) {
        rule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = state.iconRes,
                iconUrl = null,
                imageLoader = mock(),
                title = state.title,
                isSelected = state.isSelected,
                isEnabled = state.isEnabled,
                iconRequiresTinting = true,
                promoBadge = state.promoBadge,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    enum class TabState(
        val isSelected: Boolean,
        val isEnabled: Boolean,
        val iconRes: Int,
        val title: String,
        val promoBadge: String?,
    ) {
        Selected(true, true, R.drawable.stripe_ic_paymentsheet_pm_card, "Card", null),
        Unselected(false, true, R.drawable.stripe_ic_paymentsheet_pm_card, "Card", null),
        UnselectedWithBadge(false, true, R.drawable.stripe_ic_paymentsheet_pm_bank, "Bank", "$5"),
        Disabled(false, false, R.drawable.stripe_ic_paymentsheet_pm_card, "Card", null),
        DisabledWithBadge(false, false, R.drawable.stripe_ic_paymentsheet_pm_bank, "Bank", "$5"),
        LongTitle(false, false, R.drawable.stripe_ic_paymentsheet_pm_bank, "The Greatest US Bank Account", null),
    }
}
