package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.elements.Appearance

internal interface BacsMandateConfirmationLauncher {
    fun launch(
        data: BacsMandateData,
        appearance: Appearance
    )
}

internal class DefaultBacsMandateConfirmationLauncher(
    private val activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
) : BacsMandateConfirmationLauncher {
    override fun launch(
        data: BacsMandateData,
        appearance: Appearance
    ) {
        activityResultLauncher.launch(
            BacsMandateConfirmationContract.Args(
                email = data.email,
                nameOnAccount = data.name,
                sortCode = data.sortCode,
                accountNumber = data.accountNumber,
                appearance = appearance
            )
        )
    }
}
