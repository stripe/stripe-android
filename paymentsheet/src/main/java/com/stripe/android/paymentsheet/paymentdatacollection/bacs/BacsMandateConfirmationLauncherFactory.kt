package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.activity.result.ActivityResultLauncher

internal interface BacsMandateConfirmationLauncherFactory {
    fun create(
        activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    ): BacsMandateConfirmationLauncher
}

internal object DefaultBacsMandateConfirmationLauncherFactory :
    BacsMandateConfirmationLauncherFactory {
    override fun create(
        activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    ): BacsMandateConfirmationLauncher {
        return DefaultBacsMandateConfirmationLauncher(activityResultLauncher)
    }
}
