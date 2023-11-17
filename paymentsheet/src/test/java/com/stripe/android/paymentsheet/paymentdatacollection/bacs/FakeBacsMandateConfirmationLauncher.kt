package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.paymentsheet.PaymentSheet

internal class FakeBacsMandateConfirmationLauncher : BacsMandateConfirmationLauncher {
    override fun launch(
        data: BacsMandateData,
        appearance: PaymentSheet.Appearance?
    ) {
        // No-op
    }
}
