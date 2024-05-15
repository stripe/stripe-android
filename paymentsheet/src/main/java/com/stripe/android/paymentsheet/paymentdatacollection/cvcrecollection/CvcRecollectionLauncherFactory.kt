package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.activity.result.ActivityResultLauncher

internal interface CvcRecollectionLauncherFactory {
    fun create(
        activityResultLauncher: ActivityResultLauncher<CvcRecollectionContract.Args>
    ): CvcRecollectionLauncher
}

internal object DefaultCvcRecollectionLauncherFactory : CvcRecollectionLauncherFactory {
    override fun create(
        activityResultLauncher: ActivityResultLauncher<CvcRecollectionContract.Args>
    ): CvcRecollectionLauncher {
        return DefaultCvcRecollectionLauncher(activityResultLauncher)
    }
}
