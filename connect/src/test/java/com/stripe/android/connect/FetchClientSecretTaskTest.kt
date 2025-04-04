package com.stripe.android.connect

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FetchClientSecretTaskTest {

    @Test
    fun testJava() = runTest {
        val task = TestFetchClientSecretTask()
        assertThat(task.invoke()).isNull()
        task.result = "foo"
        assertThat(task.invoke()).isEqualTo("foo")
    }
}
