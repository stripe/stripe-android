package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.shoppay.ShopPayActivityResult
import com.stripe.android.shoppay.ShopPayLauncher
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

internal object RecordingShopPayLauncher {
    fun noOp(): ShopPayLauncher {
        return mock()
    }

    suspend fun test(test: suspend Scenario.() -> Unit) {
        val registerCalls = Turbine<RegisterCall>()
        val unregisterCalls = Turbine<Unit>()
        val presentCalls = Turbine<PresentCall>()

        val launcher = mock<ShopPayLauncher> {
            on { register(any<ActivityResultCaller>(), any()) } doAnswer { invocation ->
                val arguments = invocation.arguments

                registerCalls.add(
                    @Suppress("UNCHECKED_CAST")
                    RegisterCall(
                        activityResultCaller = arguments[0] as ActivityResultCaller,
                        callback = arguments[1] as (ShopPayActivityResult) -> Unit,
                    )
                )
            }

            on { unregister() } doAnswer {
                unregisterCalls.add(Unit)
            }

            on { present(any()) } doAnswer { invocation ->
                presentCalls.add(
                    PresentCall(
                        confirmationUrl = invocation.arguments[0] as String
                    )
                )
            }
        }

        test(
            Scenario(
                launcher = launcher,
                registerCalls = registerCalls,
                unregisterCalls = unregisterCalls,
                presentCalls = presentCalls,
            )
        )

        registerCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
        presentCalls.ensureAllEventsConsumed()
    }

    data class Scenario(
        val launcher: ShopPayLauncher,
        val registerCalls: ReceiveTurbine<RegisterCall>,
        val unregisterCalls: ReceiveTurbine<Unit>,
        val presentCalls: ReceiveTurbine<PresentCall>,
    )

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val callback: (ShopPayActivityResult) -> Unit,
    )

    data class PresentCall(
        val confirmationUrl: String,
    )
}
