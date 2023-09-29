package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.screenshots.PaparazziRule
import org.junit.Rule
import org.junit.Test

class BacsMandateConfirmationFormScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testState() {
        paparazziRule.snapshot {
            StripeTheme {
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
    }
}
