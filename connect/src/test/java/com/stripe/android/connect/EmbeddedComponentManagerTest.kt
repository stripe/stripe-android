package com.stripe.android.connect

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.grantPermissions(Manifest.permission.CAMERA)

        assertTrue(embeddedComponentManager.requestCameraPermission(testActivity)!!)
    }

    @Test
    fun `requestCameraPermission returns correct response when user responds to camera permission`() = runTest {
        val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
        EmbeddedComponentManager.onActivityCreate(testActivity)

        // simulate a permissions denial
        val resultFalseAsync = async {
            embeddedComponentManager.requestCameraPermission(testActivity)
        }
        advanceUntilIdle() // make sure we advance up to the point where we're waiting for the permissionsFlow
        EmbeddedComponentManager.permissionsFlow.emit(false)

        val resultFalse = resultFalseAsync.await()
        assertNotNull(resultFalse)
        assertFalse(resultFalse)

        // simulate a permissions grant
        val resultTrueAsync = async {
            embeddedComponentManager.requestCameraPermission(testActivity)
        }
        advanceUntilIdle() // make sure we advance up to the point where we're waiting for the permissionsFlow
        EmbeddedComponentManager.permissionsFlow.emit(true)

        val resultTrue = resultTrueAsync.await()
        assertNotNull(resultTrue)
        assertTrue(resultTrue)
    }

    @Test
    fun `requestCameraPermission returns null when not initialized in Activity onCreate`() = runTest {
        val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)

        assertNull(embeddedComponentManager.requestCameraPermission(testActivity))
    }

    @Test
    fun `chooseFile returns correct response`() = runTest {
        EmbeddedComponentManager.onActivityCreate(testActivity)
        val resultAsync = async {
            embeddedComponentManager.chooseFile(testActivity, Intent())
        }
        advanceUntilIdle()
        val expected = arrayOf(Uri.parse("content://test"))
        // Simulate a file being chosen.
        EmbeddedComponentManager.chooseFileResultFlow.emit(
            EmbeddedComponentManager.ActivityResult(testActivity, expected)
        )
        val actual = resultAsync.await()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `presentFinancialConnections returns correct result`() = runTest {
        EmbeddedComponentManager.onActivityCreate(testActivity)
        val resultAsync = async {
            embeddedComponentManager.presentFinancialConnections(testActivity, "secret", "id")
        }
        advanceUntilIdle()
        val expected = FinancialConnectionsSheetResult.Canceled
        // Simulate financial connections
        EmbeddedComponentManager.financialConnectionsResults.emit(
            EmbeddedComponentManager.ActivityResult(testActivity, expected)
        )
        val actual = resultAsync.await()

        assertThat(actual).isEqualTo(expected)
    }
}
