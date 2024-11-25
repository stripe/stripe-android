package com.stripe.android.connect

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(PrivateBetaConnectSDK::class)
class EmbeddedComponentManagerTest {

    private lateinit var configuration: EmbeddedComponentManager.Configuration
    private lateinit var mockFetchClientSecretCallback: FetchClientSecretCallback
    private lateinit var embeddedComponentManager: EmbeddedComponentManager

    @Before
    fun setup() {
        configuration = EmbeddedComponentManager.Configuration("test_publishable_key")
        mockFetchClientSecretCallback = mock()
        embeddedComponentManager = EmbeddedComponentManager(configuration, mockFetchClientSecretCallback)
    }

    @Test
    fun `fetchClientSecret should return client secret when callback provides it`() = runTest {
        val expectedSecret = "test_client_secret"

        whenever(mockFetchClientSecretCallback.fetchClientSecret(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<FetchClientSecretCallback.ClientSecretResultCallback>(0)
            callback.onResult(expectedSecret)
        }

        val result = embeddedComponentManager.fetchClientSecret()

        assertEquals(expectedSecret, result)
        verify(mockFetchClientSecretCallback).fetchClientSecret(any())
    }

    @Test
    fun `fetchClientSecret should return null when callback provides null`() = runTest {
        whenever(mockFetchClientSecretCallback.fetchClientSecret(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<FetchClientSecretCallback.ClientSecretResultCallback>(0)
            callback.onResult(null)
        }

        val result = embeddedComponentManager.fetchClientSecret()

        assertNull(result)
        verify(mockFetchClientSecretCallback).fetchClientSecret(any())
    }
}
