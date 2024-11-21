package com.stripe.android.paymentelement.confirmation.bacs

import androidx.activity.result.ActivityResultLauncher
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher

internal class FakeBacsMandateConfirmationLauncherFactory : BacsMandateConfirmationLauncherFactory {
    private val calls = Turbine<Call>()

    override fun create(
        activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    ): BacsMandateConfirmationLauncher {
        calls.add(Call(activityResultLauncher))

        return FakeBacsMandateConfirmationLauncher()
    }

    class Call(
        val activityResultLauncher: ActivityResultLauncher<BacsMandateConfirmationContract.Args>
    )

    class Scenario(
        val factory: BacsMandateConfirmationLauncherFactory,
        private val calls: ReceiveTurbine<Call>,
    ) {
        suspend fun awaitCreateBacsLauncherCall(): Call {
            return calls.awaitItem()
        }
    }

    companion object {
        suspend fun test(
            block: suspend Scenario.() -> Unit
        ) {
            val factory = FakeBacsMandateConfirmationLauncherFactory()
            Scenario(
                factory = factory,
                calls = factory.calls,
            ).apply {
                block(this)
                factory.calls.ensureAllEventsConsumed()
            }
        }
    }
}
