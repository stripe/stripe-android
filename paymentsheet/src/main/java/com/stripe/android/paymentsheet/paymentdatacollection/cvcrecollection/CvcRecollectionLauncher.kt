package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.elements.Appearance

internal interface CvcRecollectionLauncher {
    fun launch(
        data: CvcRecollectionData,
        appearance: Appearance,
        isLiveMode: Boolean
    )
}

internal class DefaultCvcRecollectionLauncher(
    private val activityResultLauncher: ActivityResultLauncher<CvcRecollectionContract.Args>
) : CvcRecollectionLauncher {
    override fun launch(data: CvcRecollectionData, appearance: Appearance, isLiveMode: Boolean) {
        activityResultLauncher.launch(
            CvcRecollectionContract.Args(
                lastFour = data.lastFour ?: "",
                cardBrand = data.brand,
                appearance = appearance,
                isTestMode = isLiveMode.not()
            )
        )
    }
}
