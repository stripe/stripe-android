package com.stripe.android.common.taptoadd

import android.content.Context
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener

internal class TestTerminalWrapper private constructor(
    private val isInitialized: Boolean,
    private val terminalInstance: Terminal,
) : TerminalWrapper {
    private val isInitializedCalls = Turbine<Unit>()
    private val initTerminalCalls = Turbine<InitTerminalCall>()

    override fun isInitialized(): Boolean {
        isInitializedCalls.add(Unit)

        return isInitialized
    }

    override fun initTerminal(context: Context, tokenProvider: ConnectionTokenProvider, listener: TerminalListener) {
        initTerminalCalls.add(
            InitTerminalCall(
                context = context,
                tokenProvider = tokenProvider,
                listener = listener,
            )
        )
    }

    override fun getInstance(): Terminal {
        return terminalInstance
    }

    class InitTerminalCall(
        val context: Context,
        val tokenProvider: ConnectionTokenProvider,
        val listener: TerminalListener,
    )

    class Scenario(
        val wrapper: TerminalWrapper,
        val isInitializedCalls: ReceiveTurbine<Unit>,
        val initTerminalCalls: ReceiveTurbine<InitTerminalCall>,
    )

    companion object {
        suspend fun test(
            isInitialized: Boolean,
            terminalInstance: Terminal,
            block: suspend Scenario.() -> Unit
        ) {
            val wrapper = TestTerminalWrapper(isInitialized, terminalInstance)

            block(
                Scenario(
                    wrapper = wrapper,
                    isInitializedCalls = wrapper.isInitializedCalls,
                    initTerminalCalls = wrapper.initTerminalCalls,
                )
            )

            wrapper.isInitializedCalls.ensureAllEventsConsumed()
            wrapper.initTerminalCalls.ensureAllEventsConsumed()
        }
    }
}
