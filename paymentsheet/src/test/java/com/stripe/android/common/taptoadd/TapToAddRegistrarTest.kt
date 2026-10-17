package com.stripe.android.common.taptoadd

import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.testing.DummyActivityResultCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddRegistrarTest {
    @Test
    fun `init registers confirmation handler with activity result caller and lifecycle owner`() = runTest {
        FakeConfirmationHandler.test(
            initialState = ConfirmationHandler.State.Idle,
        ) {
            val activityResultCaller = DummyActivityResultCaller.noOp()
            val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)

            TapToAddRegistrar(
                confirmationHandler = handler,
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
            )

            with(registerTurbine.awaitItem()) {
                assertThat(activityResultCaller).isEqualTo(activityResultCaller)
                assertThat(lifecycleOwner).isEqualTo(lifecycleOwner)
            }
        }
    }
}
