package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class BacsMandateConfirmationFormScreenshotTest {
    @get:Rule
    val paparazziSingleVariantRule = PaparazziRule(
        arrayOf(SystemAppearance.LightTheme),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        arrayOf(FontSize.LargeFont)
    )

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values()
    )

    @Test
    fun testFullForm() {
        paparazziSingleVariantRule.snapshot {
            BacsMandateConfirmationFormView(
                state = BacsMandateConfirmationViewState(
                    accountNumber = "00012345",
                    sortCode = "10-88-00",
                    email = "email@email.com",
                    nameOnAccount = "John Doe",
                    payer = resolvableString(R.string.stripe_paymentsheet_bacs_notice_default_payer),
                    debitGuaranteeAsHtml = resolvableString(
                        R.string.stripe_paymentsheet_bacs_guarantee_format,
                        resolvableString(R.string.stripe_paymentsheet_bacs_guarantee_url),
                        resolvableString(R.string.stripe_paymentsheet_bacs_guarantee)
                    ),
                    supportAddressAsHtml = resolvableString(
                        R.string.stripe_paymentsheet_bacs_support_address_format,
                        resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_one),
                        resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_two),
                        resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email),
                        resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email)
                    )
                ),
                viewActionHandler = {}
            )
        }
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
}
