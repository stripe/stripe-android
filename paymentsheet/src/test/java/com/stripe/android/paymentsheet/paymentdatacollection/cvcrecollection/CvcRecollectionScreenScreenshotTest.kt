package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@OptIn(AppearanceAPIAdditionsPreview::class)
class CvcRecollectionScreenScreenshotTest {
    private val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    private val scopedThemePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(paparazziRule)
        .around(scopedThemePaparazziRule)

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            TestCvcRecollectionScreen()
        }
    }

    @Test
    fun testAutomaticTheme() {
        snapshotWithAppearance(PaymentSheet.Appearance())
    }

    @Test
    fun testAlwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysLight),
        )
    }

    @Test
    fun testAlwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysDark),
        )
    }

    @Test
    fun testCustomAppearanceTheme() {
        snapshotWithAppearance(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                lastFour = "4242",
                isTestMode = false,
                viewActionHandler = {},
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
            )
        }
    }

    @Test
    fun testFilledTestMode() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                lastFour = "4242",
                isTestMode = true,
                viewActionHandler = {},
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
            )
        }
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        scopedThemePaparazziRule.snapshot {
            PaymentElementTheme(appearance = appearance) {
                Surface(color = MaterialTheme.colors.surface) {
                    TestCvcRecollectionScreen()
                }
            }
        }
    }

    @Composable
    private fun TestCvcRecollectionScreen() {
        CvcRecollectionScreen(
            lastFour = "4242",
            isTestMode = false,
            cvcState = CvcState(
                cardBrand = CardBrand.Visa,
                cvc = ""
            ),
            viewActionHandler = {}
        )
    }
}
