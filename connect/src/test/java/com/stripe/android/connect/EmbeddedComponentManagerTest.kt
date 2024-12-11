package com.stripe.android.connect

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
class EmbeddedComponentManagerTest {

    private lateinit var configuration: EmbeddedComponentManager.Configuration
    private lateinit var mockFetchClientSecretCallback: FetchClientSecretCallback
    private lateinit var embeddedComponentManager: EmbeddedComponentManager
    private lateinit var testActivity: ComponentActivity

    @Before
    fun setup() {
        configuration = EmbeddedComponentManager.Configuration("test_publishable_key")
        mockFetchClientSecretCallback = mock()
        embeddedComponentManager = EmbeddedComponentManager(configuration, mockFetchClientSecretCallback)
        testActivity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
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

    @Test
    fun `requestCameraPermission returns true when camera permission is already granted`() = runTest {
        val shadowApplication = Shadows.shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.grantPermissions(Manifest.permission.CAMERA)

        assertTrue(embeddedComponentManager.requestCameraPermission(testActivity)!!)
    }

    @Test
    fun `requestCameraPermission returns false when camera permission is denied`() = runTest(UnconfinedTestDispatcher()) {
        val shadowApplication = Shadows.shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)

        EmbeddedComponentManager.onActivityCreate(testActivity)
        EmbeddedComponentManager.permissionsFlow.tryEmit(false)

        assertFalse(embeddedComponentManager.requestCameraPermission(testActivity)!!)
    }

    @Test
    fun `requestCameraPermission returns true when camera permission is granted after request`() = runTest {
        val shadowApplication = Shadows.shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)

        EmbeddedComponentManager.onActivityCreate(testActivity)
        EmbeddedComponentManager.permissionsFlow.tryEmit(true)

        assertTrue(embeddedComponentManager.requestCameraPermission(testActivity)!!)
    }
}
