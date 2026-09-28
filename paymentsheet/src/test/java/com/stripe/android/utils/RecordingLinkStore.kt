package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import com.stripe.android.link.account.LinkStore

internal object RecordingLinkStore {
    fun noOp(): LinkStore {
        return FakeLinkStore()
    }

    suspend fun test(
        hasUsedLink: Boolean = false,
        test: suspend Scenario.() -> Unit
    ) {
        val fakeLinkStore = FakeLinkStore(hasUsedLink)

        test(
            Scenario(
                linkStore = fakeLinkStore,
                markAsUsedCalls = fakeLinkStore.markAsUsedCalls,
            )
        )

        fakeLinkStore.ensureAllEventsConsumed()
    }

    data class Scenario(
        val linkStore: LinkStore,
        val markAsUsedCalls: ReceiveTurbine<Unit>,
    )
}
