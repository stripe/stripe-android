package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.LinkAccount
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

internal object RecordingLinkPaymentLauncher {
    fun noOp(): LinkPaymentLauncher {
        return mock()
    }

    suspend fun test(test: suspend Scenario.() -> Unit) {
        val registerCalls = Turbine<RegisterCall>()
        val unregisterCalls = Turbine<Unit>()
        val presentCalls = Turbine<PresentCall>()

        val launcher = mock<LinkPaymentLauncher> {
            on { register(any<ActivityResultCaller>(), any()) } doAnswer { invocation ->
                val arguments = invocation.arguments

                registerCalls.add(
                    @Suppress("UNCHECKED_CAST")
                    RegisterCall(
                        activityResultCaller = arguments[0] as ActivityResultCaller,
                        callback = arguments[1] as (LinkActivityResult) -> Unit,
                    )
                )
            }

            on { unregister() } doAnswer {
                unregisterCalls.add(Unit)
            }

            on { present(any(), anyOrNull()) } doAnswer { invocation ->
                val arguments = invocation.arguments

                presentCalls.add(
                    PresentCall(
                        configuration = arguments[0] as LinkConfiguration,
                        linkAccount = arguments[1] as? LinkAccount
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
        val launcher: LinkPaymentLauncher,
        val registerCalls: ReceiveTurbine<RegisterCall>,
        val unregisterCalls: ReceiveTurbine<Unit>,
        val presentCalls: ReceiveTurbine<PresentCall>,
    )

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val callback: (LinkActivityResult) -> Unit,
    )

    data class PresentCall(
        val configuration: LinkConfiguration,
        val linkAccount: LinkAccount?
    )
}
