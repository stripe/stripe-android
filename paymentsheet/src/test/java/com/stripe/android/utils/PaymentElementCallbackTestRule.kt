package com.stripe.android.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PaymentElementCallbackTestRule : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        PaymentElementCallbackReferences.clear()
    }

    override fun finished(description: Description) {
        PaymentElementCallbackReferences.clear()
        super.finished(description)
    }
}

/** For callback consumers; lifecycle tests should register with an explicit owner instead. */
internal fun PaymentElementCallbackReferences.registerForTest(key: String, callbacks: PaymentElementCallbacks) {
    register(
        key = key,
        owner = TestLifecycleOwner(
            initialState = Lifecycle.State.INITIALIZED,
            coroutineDispatcher = UnconfinedTestDispatcher(),
        ),
        callbacks = callbacks,
    )
}
