package com.stripe.android.connect.manager

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.ClientSecretProvider
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.core.Logger
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
class EmbeddedComponentCoordinatorTest {

    private lateinit var publishableKey: String
    private lateinit var mockClientSecretProvider: ClientSecretProvider
    private lateinit var coordinator: EmbeddedComponentCoordinator
    private lateinit var testActivityController: ActivityController<ComponentActivity>
    private val testActivity: ComponentActivity get() = testActivityController.get()

    @Before
    fun setup() {
        publishableKey = "test_publishable_key"
        mockClientSecretProvider = mock()
        coordinator =
            EmbeddedComponentCoordinator(
                publishableKey = publishableKey,
                clientSecretProvider = mockClientSecretProvider,
                logger = Logger.noop(),
                appearance = Appearance.default(),
                customFonts = emptyList(),
            )
        testActivityController = Robolectric.buildActivity(ComponentActivity::class.java).create()
    }

    @Test
    fun `onActivityCreate registers Activity in mappings and unregisters on destroy`() {
        EmbeddedComponentCoordinator.onActivityCreate(testActivity)
        assertThat(EmbeddedComponentCoordinator.requestPermissionLaunchers[testActivity]).isNotNull()
        assertThat(EmbeddedComponentCoordinator.chooseFileLaunchers[testActivity]).isNotNull()
        assertThat(EmbeddedComponentCoordinator.financialConnectionsSheets[testActivity]).isNotNull()
        testActivityController.destroy()
        assertThat(EmbeddedComponentCoordinator.requestPermissionLaunchers[testActivity]).isNull()
        assertThat(EmbeddedComponentCoordinator.chooseFileLaunchers[testActivity]).isNull()
        assertThat(EmbeddedComponentCoordinator.financialConnectionsSheets[testActivity]).isNull()
    }

    @Test
    fun `fetchClientSecret should return client secret when callback provides it`() = runTest {
        val expectedSecret = "test_client_secret"

        wheneverBlocking { mockClientSecretProvider.provideClientSecret() }.doReturn(expectedSecret)

        val result = coordinator.fetchClientSecret()

        assertEquals(expectedSecret, result)
    }

    @Test
    fun `fetchClientSecret should return null when callback provides null`() = runTest {
        wheneverBlocking { mockClientSecretProvider.provideClientSecret() }.doReturn(null)

        val result = coordinator.fetchClientSecret()

        assertNull(result)
        verify(mockClientSecretProvider).provideClientSecret()
    }

    @Test
    fun `requestCameraPermission returns true when camera permission is already granted`() = runTest {
        val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.grantPermissions(Manifest.permission.CAMERA)

        assertTrue(coordinator.requestCameraPermission(testActivity))
    }

    @Test
    fun `requestCameraPermission returns correct response when user responds to camera permission`() = runTest {
        val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext() as Application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
        EmbeddedComponentCoordinator.onActivityCreate(testActivity)

        // simulate a permissions denial
        val resultFalseAsync = async {
            coordinator.requestCameraPermission(testActivity)
        }
        advanceUntilIdle() // make sure we advance up to the point where we're waiting for the permissionsFlow
        EmbeddedComponentCoordinator.permissionsFlow.emit(false)

        val resultFalse = resultFalseAsync.await()
        assertNotNull(resultFalse)
        assertFalse(resultFalse)

        // simulate a permissions grant
        val resultTrueAsync = async {
            coordinator.requestCameraPermission(testActivity)
        }
        advanceUntilIdle() // make sure we advance up to the point where we're waiting for the permissionsFlow
        EmbeddedComponentCoordinator.permissionsFlow.emit(true)

        val resultTrue = resultTrueAsync.await()
        assertNotNull(resultTrue)
        assertTrue(resultTrue)
    }

    @Test
    fun `chooseFile returns correct response`() = runTest {
        EmbeddedComponentCoordinator.onActivityCreate(testActivity)
        val resultAsync = async {
            coordinator.chooseFile(testActivity, Intent())
        }
        advanceUntilIdle()
        val expected = arrayOf(Uri.parse("content://test"))
        // Simulate a file being chosen.
        EmbeddedComponentCoordinator.chooseFileResultFlow.emit(
            EmbeddedComponentCoordinator.ActivityResult(testActivity, expected)
        )
        val actual = resultAsync.await()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `presentFinancialConnections returns correct result`() = runTest {
        EmbeddedComponentCoordinator.onActivityCreate(testActivity)
        val resultAsync = async {
            coordinator.presentFinancialConnections(testActivity, "secret", "id")
        }
        advanceUntilIdle()
        val expected = FinancialConnectionsSheetResult.Canceled
        // Simulate financial connections
        EmbeddedComponentCoordinator.financialConnectionsResults.emit(
            EmbeddedComponentCoordinator.ActivityResult(testActivity, expected)
        )
        val actual = resultAsync.await()

        assertThat(actual).isEqualTo(expected)
    }
}
