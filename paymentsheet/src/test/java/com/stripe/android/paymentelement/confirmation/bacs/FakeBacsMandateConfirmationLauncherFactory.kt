package com.stripe.android.paymentelement.confirmation.bacs

import androidx.activity.result.ActivityResultLauncher
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher

internal class FakeBacsMandateConfirmationLauncherFactory : BacsMandateConfirmationLauncherFactory {
    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun create(
        activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    ): BacsMandateConfirmationLauncher {
        _calls.add(Call(activityResultLauncher))

        return FakeBacsMandateConfirmationLauncher()
    }

    class Call(
        val activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    )
}
