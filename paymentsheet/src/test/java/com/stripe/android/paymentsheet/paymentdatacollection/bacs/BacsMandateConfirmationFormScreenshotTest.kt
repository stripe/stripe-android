package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
class BacsMandateConfirmationFormScreenshotTest {
    @get:Rule
    val paparazziSingleVariantRule = PaparazziRule()

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @get:Rule
    val scopedThemePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun testFullForm() {
        paparazziSingleVariantRule.snapshot {
            FullForm()
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
    fun testDetails() {
        paparazziSingleVariantRule.snapshot {
            BacsMandateDetails(
                email = "email@email.com",
                nameOnAccount = "John Doe",
                accountNumber = "00012345",
                sortCode = "10-88-00"
            )
        }
    }

    @Test
    fun testDetailsRow() {
        paparazziRule.snapshot {
            BacsMandateDetailsRow(
                label = "Sort code",
                value = "10-88-00"
            )
        }
    }

    @Test
    fun testItemWithNoHtml() {
        paparazziRule.snapshot {
            BacsMandateItem(
                text = "An email will be sent to email@email.com."
            )
        }
    }

    @Test
    fun testItemWithHtml() {
        paparazziRule.snapshot {
            BacsMandateItem(
                text = "An email will be sent to <a href=\"\">email@email.com</a>.",
                isHtml = true
            )
        }
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        scopedThemePaparazziRule.snapshot {
            PaymentElementTheme(appearance = appearance) {
                Surface(color = MaterialTheme.colors.surface) {
                    FullForm()
                }
            }
        }
    }

    @Composable
    private fun FullForm() {
        BacsMandateConfirmationFormView(
            state = BacsMandateConfirmationViewState(
                accountNumber = "00012345",
                sortCode = "10-88-00",
                email = "email@email.com",
                nameOnAccount = "John Doe",
                payer = R.string.stripe_paymentsheet_bacs_notice_default_payer.resolvableString,
                debitGuaranteeAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_guarantee_format,
                    R.string.stripe_paymentsheet_bacs_guarantee_url.resolvableString,
                    R.string.stripe_paymentsheet_bacs_guarantee.resolvableString
                ),
                supportAddressAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_support_address_format,
                    R.string.stripe_paymentsheet_bacs_support_default_address_line_one.resolvableString,
                    R.string.stripe_paymentsheet_bacs_support_default_address_line_two.resolvableString,
                    R.string.stripe_paymentsheet_bacs_support_default_email.resolvableString,
                    R.string.stripe_paymentsheet_bacs_support_default_email.resolvableString
                )
            ),
            viewActionHandler = {}
        )
    }
}
