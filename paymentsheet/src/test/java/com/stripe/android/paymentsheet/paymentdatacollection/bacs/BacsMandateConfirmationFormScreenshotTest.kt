package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.utils.screenshots.PaparazziRule
import org.junit.Rule
import org.junit.Test

class BacsMandateConfirmationFormScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testState() {
        paparazziRule.snapshot {
            BacsMandateConfirmationTheme {
                BacsMandateConfirmationFormView(
                    state = BacsMandateConfirmationViewState(
                        accountNumber = "00012345",
                        sortCode = "10-88-00",
                        email = "email@email.com",
                        nameOnAccount = "John Doe",
                        debitGuaranteeAsHtml = "<a href=\"\">Direct Debit Guarantee</a>",
                        supportAddressAsHtml = "Stripe, 7th Floor The Bower Warehouse" +
                            "<br>207-211 Old St, London EC1V 9NR" +
                            "<br><a href=\"\">support@stripe.com</a>"
                    ),
                    viewActionHandler = {}
                )
            }
        }
    }
}
