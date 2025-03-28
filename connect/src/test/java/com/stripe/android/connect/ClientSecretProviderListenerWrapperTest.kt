package com.stripe.android.connect

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClientSecretProviderListenerWrapperTest {

    private val wrapper = TestClientSecretProviderListenerWrapper()

    @Test
    fun test() = runTest {
        assertThat(wrapper.provideClientSecret()).isNull()
        wrapper.result = "foo"
        assertThat(wrapper.provideClientSecret()).isEqualTo("foo")
    }
}
