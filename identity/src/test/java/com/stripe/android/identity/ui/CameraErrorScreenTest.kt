package com.stripe.android.identity.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.DocumentUploadDestination
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.stripe.android.camera.R as CameraR

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
internal class CameraErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockNavController = mock<NavController>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { screenTracker } doReturn mock()
    }

    @Test
    fun `upload button opens the upload screen when live capture is not required`() {
        // Given the camera error screen for a session that allows uploads
        setContent(requireLiveCapture = false)
        composeRule.onNodeWithTag(ErrorTitleTag)
            .assertTextEquals(context.getString(CameraR.string.stripe_error_camera_title))

        // When the user taps the upload button
        composeRule.onNodeWithTag(ErrorBottomButtonTag).performClick()

        // Then the upload screen opens
        verify(mockNavController).navigate(
            eq(DocumentUploadDestination.routeWithArgs),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `no button is shown when live capture is required`() {
        // Given the camera error screen for a session that requires live capture
        setContent(requireLiveCapture = true)

        // Then the screen explains the error and offers no way to continue
        composeRule.onNodeWithTag(ErrorTitleTag)
            .assertTextEquals(context.getString(CameraR.string.stripe_error_camera_title))
        composeRule.onNodeWithTag(ErrorTopButtonTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ErrorBottomButtonTag).assertDoesNotExist()
    }

    private fun setContent(requireLiveCapture: Boolean) {
        composeRule.setContent {
            CameraErrorScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                requireLiveCapture = requireLiveCapture
            )
        }
    }
}
