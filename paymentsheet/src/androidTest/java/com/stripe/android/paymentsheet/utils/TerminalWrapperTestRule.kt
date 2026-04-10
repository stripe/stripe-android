package com.stripe.android.paymentsheet.utils

import android.content.Context
import com.stripe.android.common.taptoadd.TerminalWrapper
import com.stripe.android.tta.testing.TerminalTestDelegate
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TerminalWrapperTestRule(val enabled: Boolean) : TestWatcher() {
    private var _delegate: TerminalTestDelegate
    val delegate: TerminalTestDelegate
        get() = _delegate

    init {
        _delegate = TerminalTestDelegate(scenario = TerminalTestDelegate.Scenario.withoutMocks())

        if (!enabled) {
            _delegate.setScenario(
                TerminalTestDelegate.Scenario(
                    readerSupportResult = ReaderSupportResult.NotSupported(
                        error = IllegalStateException("Not testable!"),
                    )
                )
            )
        }
    }

    override fun starting(description: Description?) {
        if (enabled) {
            _delegate = TerminalTestDelegate()
            TerminalWrapper.overrideWrapper = FakeTerminalWrapper(delegate)
        }

        super.starting(description)
    }

    override fun finished(description: Description?) {
        if (enabled) {
            TerminalWrapper.overrideWrapper = null

            if (delegate.shouldValidate) {
                delegate.validate()
            }
        }

        super.finished(description)
    }

    private class FakeTerminalWrapper(
        private val delegate: TerminalTestDelegate
    ) : TerminalWrapper {
        override fun isInitialized(): Boolean {
            return delegate.isInitialized()
        }

        override fun initTerminal(
            context: Context,
            tokenProvider: ConnectionTokenProvider,
            listener: TerminalListener
        ) {
            delegate.initTerminal(context, tokenProvider, listener)
        }

        override fun getInstance(): Terminal {
            return delegate.getInstance()
        }
    }
}
