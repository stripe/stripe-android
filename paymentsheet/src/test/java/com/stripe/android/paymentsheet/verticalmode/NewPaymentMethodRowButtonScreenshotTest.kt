package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(TestParameterInjector::class)
internal class NewPaymentMethodRowButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
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
    fun testSelectedState() {
        snapshot(RowState.Selected, paparazziRule)
    }

    @Test
    fun testUnselectedState() {
        snapshot(RowState.Unselected, paparazziRule)
    }

    @Test
    fun testDisabled() {
        snapshot(RowState.Disabled, paparazziRule)
    }

    @Test
    fun testLongTitle() {
        snapshot(RowState.LongTitle, paparazziRule)
    }

    @Test
    fun testThemeBasedIcons(@TestParameter state: RowState) {
        disableLuminanceBasedThemesRule.setEnabled(true)
        snapshot(state, themeBasedIconsPaparazziRule)
    }

    @Test
    fun testSubtitle() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isExpanded = false,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Klarna",
                subtitle = "Buy now or pay later with Klarna.",
                promoText = null,
                iconRequiresTinting = false,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testLongSubtitle() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isExpanded = false,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Klarna",
                subtitle = "A very long subtitle, that you should read all of and you should Buy now or pay later" +
                    " with Klarna.",
                iconRequiresTinting = false,
                promoText = null,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testEmbeddedNewSelected() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isExpanded = false,
                isSelected = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Card",
                subtitle = "Mastercard **** 4444",
                iconRequiresTinting = false,
                promoText = null,
                onClick = {},
                modifier = Modifier,
                appearance = Embedded(Embedded.RowStyle.FlatWithRadio.default),
                trailingContent = {
                    EmbeddedNewPaymentMethodTrailingContent(
                        showChevron = true
                    )
                }
            )
        }
    }

    private fun snapshot(state: RowState, rule: PaparazziRule) {
        rule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = state.isEnabled,
                isExpanded = false,
                isSelected = state.isSelected,
                iconRes = state.iconRes,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = state.title,
                subtitle = null,
                promoText = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    enum class RowState(
        val isSelected: Boolean,
        val isEnabled: Boolean,
        val iconRes: Int,
        val title: String,
    ) {
        Selected(true, true, R.drawable.stripe_ic_paymentsheet_pm_card, "Card"),
        Unselected(false, true, R.drawable.stripe_ic_paymentsheet_pm_card, "Card"),
        Disabled(false, false, R.drawable.stripe_ic_paymentsheet_pm_card, "Card"),
        LongTitle(false, true, R.drawable.stripe_ic_paymentsheet_pm_bank, "The Greatest US Bank Account"),
    }
}
