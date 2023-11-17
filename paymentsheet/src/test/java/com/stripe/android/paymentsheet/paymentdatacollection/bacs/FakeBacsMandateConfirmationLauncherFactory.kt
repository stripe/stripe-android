package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.activity.result.ActivityResultLauncher

internal class FakeBacsMandateConfirmationLauncherFactory(
    private val launcher: BacsMandateConfirmationLauncher = FakeBacsMandateConfirmationLauncher()
) : BacsMandateConfirmationLauncherFactory {
    override fun create(
        activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    ): BacsMandateConfirmationLauncher {
        return launcher
    }
}
