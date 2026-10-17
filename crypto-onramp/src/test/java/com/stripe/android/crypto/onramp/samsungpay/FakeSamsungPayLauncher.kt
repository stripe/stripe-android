package com.stripe.android.crypto.onramp.samsungpay

import android.content.Context
import app.cash.turbine.Turbine
import com.stripe.android.crypto.onramp.model.OnrampConfiguration

internal class FakeSamsungPayLauncher(
    var status: SamsungPayStatus = SamsungPayStatus.Ready,
    var result: SamsungPayResult? = null,
) : SamsungPayLauncher {
    val statusCalls = Turbine<Unit>()
    val presentCalls = Turbine<SamsungPayPresentation>()
    val destroyCalls = Turbine<Unit>()

    private var resultCallback: ((SamsungPayResult) -> Unit)? = null

    override fun getStatus(callback: (SamsungPayStatus) -> Unit) {
        statusCalls.add(Unit)
        callback(status)
    }

    override fun present(
        presentation: SamsungPayPresentation,
        callback: (SamsungPayResult) -> Unit,
    ) {
        presentCalls.add(presentation)
        resultCallback = callback
        result?.let(callback)
    }

    override fun destroy() {
        destroyCalls.add(Unit)
    }

    fun complete(result: SamsungPayResult) {
        requireNotNull(resultCallback)(result)
    }

    fun ensureAllEventsConsumed() {
        statusCalls.ensureAllEventsConsumed()
        presentCalls.ensureAllEventsConsumed()
        destroyCalls.ensureAllEventsConsumed()
    }
}

internal class FakeSamsungPayLauncherFactory(
    private val launcher: SamsungPayLauncher,
) : SamsungPayLauncher.Factory {
    val createCalls = Turbine<CreateCall>()

    override fun create(
        context: Context,
        configuration: OnrampConfiguration.SamsungPayConfig,
        merchantDisplayName: String,
    ): SamsungPayLauncher {
        createCalls.add(CreateCall(context, configuration, merchantDisplayName))
        return launcher
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
    }

    data class CreateCall(
        val context: Context,
        val configuration: OnrampConfiguration.SamsungPayConfig,
        val merchantDisplayName: String,
    )
}
