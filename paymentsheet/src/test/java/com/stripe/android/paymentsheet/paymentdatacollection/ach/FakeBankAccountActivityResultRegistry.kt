package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.Turbine
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract

internal class FakeBankAccountActivityResultRegistry : ActivityResultRegistry() {
    val launchCalls = Turbine<CollectBankAccountContract.Args>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        launchCalls.add(input as CollectBankAccountContract.Args)
    }

    fun ensureAllEventsConsumed() {
        launchCalls.ensureAllEventsConsumed()
    }
}
