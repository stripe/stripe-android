package com.stripe.android.common.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PaymentElementActivityResultCallerTest {
    @Test
    fun `on register callback, registry owner should register callback properly`() {
        val registry = mock<ActivityResultRegistry> {
            on {
                register(
                    any<String>(),
                    any<TestActivityResultContract>(),
                    any<ActivityResultCallback<String>>(),
                )
            } doReturn mock()
        }

        val activityResultRegistryOwner = mock<ActivityResultRegistryOwner> {
            on { activityResultRegistry } doReturn registry
        }

        val caller = PaymentElementActivityResultCaller("FlowController", activityResultRegistryOwner)

        val contract = TestActivityResultContract()
        val callback = ActivityResultCallback<String> {}

        caller.registerForActivityResult(
            contract,
            callback
        )

        verify(registry).register(
            "FlowController_${TestActivityResultContract::class.java.name}",
            contract,
            callback,
        )
    }

    @Test
    fun `on register callback with registry, registry owner should register callback properly`() {
        val registry = mock<ActivityResultRegistry> {
            on {
                register(
                    any<String>(),
                    any<TestActivityResultContract>(),
                    any<ActivityResultCallback<String>>(),
                )
            } doReturn mock()
        }

        val activityResultRegistryOwner = mock<ActivityResultRegistryOwner>()

        val caller = PaymentElementActivityResultCaller("FlowController", activityResultRegistryOwner)

        val contract = TestActivityResultContract()
        val callback = ActivityResultCallback<String> {}

        caller.registerForActivityResult(
            contract,
            registry,
            callback
        )

        verify(registry).register(
            "FlowController_${TestActivityResultContract::class.java.name}",
            contract,
            callback,
        )
    }

    private class TestActivityResultContract : ActivityResultContract<String, String>() {
        override fun createIntent(context: Context, input: String): Intent {
            throw NotImplementedError("Should not be called!")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String {
            throw NotImplementedError("Should not be called!")
        }
    }
}
