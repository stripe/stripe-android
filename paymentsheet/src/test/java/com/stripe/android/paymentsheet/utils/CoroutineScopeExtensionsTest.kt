package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

class CoroutineScopeExtensionsTest {
    @Test
    fun `childScope cancellation does not cancel the parent`() {
        val parentScope = TestScope(UnconfinedTestDispatcher() + SupervisorJob())
        val childScope = parentScope.childScope(Dispatchers.Unconfined)

        childScope.cancel()

        assertThat(parentScope.isActive).isTrue()
    }

    @Test
    fun `parent cancellation cancels the childScope`() {
        val parentScope = TestScope(UnconfinedTestDispatcher() + SupervisorJob())
        val childScope = parentScope.childScope(Dispatchers.Unconfined)

        parentScope.cancel()

        assertThat(childScope.isActive).isFalse()
    }
}
