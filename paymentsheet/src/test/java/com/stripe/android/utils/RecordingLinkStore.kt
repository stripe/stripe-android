package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.link.account.LinkStore
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

internal object RecordingLinkStore {
    fun noOp(): LinkStore {
        return mock()
    }

    suspend fun test(
        hasUsedLink: Boolean = false,
        test: suspend Scenario.() -> Unit
    ) {
        val markAsUsedCalls = Turbine<Unit>()
        val hasUsedLinkCalls = Turbine<Unit>()

        val linkStore = mock<LinkStore> {
            on { markLinkAsUsed() } doAnswer {
                markAsUsedCalls.add(Unit)
            }

            on { hasUsedLink() } doAnswer {
                hasUsedLinkCalls.add(Unit)

                hasUsedLink
            }
        }

        test(
            Scenario(
                linkStore = linkStore,
                markAsUsedCalls = markAsUsedCalls,
            )
        )

        markAsUsedCalls.ensureAllEventsConsumed()
    }

    data class Scenario(
        val linkStore: LinkStore,
        val markAsUsedCalls: ReceiveTurbine<Unit>,
    )
}
