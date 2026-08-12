package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import androidx.activity.result.ActivityResultLauncher

internal interface UpdatedTaxAmountLauncher {
    fun launch(args: UpdatedTaxAmountContract.Args)
}

internal class DefaultUpdatedTaxAmountLauncher(
    private val activityResultLauncher: ActivityResultLauncher<UpdatedTaxAmountContract.Args>,
) : UpdatedTaxAmountLauncher {
    override fun launch(args: UpdatedTaxAmountContract.Args) {
        activityResultLauncher.launch(args)
    }
}

internal fun interface UpdatedTaxAmountLauncherFactory {
    fun create(
        activityResultLauncher: ActivityResultLauncher<UpdatedTaxAmountContract.Args>,
    ): UpdatedTaxAmountLauncher
}

internal object DefaultUpdatedTaxAmountLauncherFactory : UpdatedTaxAmountLauncherFactory {
    override fun create(
        activityResultLauncher: ActivityResultLauncher<UpdatedTaxAmountContract.Args>,
    ): UpdatedTaxAmountLauncher {
        return DefaultUpdatedTaxAmountLauncher(activityResultLauncher)
    }
}
