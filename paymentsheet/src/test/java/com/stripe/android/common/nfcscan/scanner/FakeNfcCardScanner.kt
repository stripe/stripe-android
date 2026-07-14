package com.stripe.android.common.nfcscan.scanner

import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeNfcCardScanner(
    stateFlow: Flow<NfcCardScanner.State> = emptyFlow(),
) : NfcCardScanner {
    val startCalls = Turbine<AppCompatActivity>()

    override val state: Flow<NfcCardScanner.State> = stateFlow

    override fun start(activity: AppCompatActivity) {
        startCalls.add(activity)
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }
}
