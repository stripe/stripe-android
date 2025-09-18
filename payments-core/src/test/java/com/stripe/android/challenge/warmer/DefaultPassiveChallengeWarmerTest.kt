package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerCompleted
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerContract
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultPassiveChallengeWarmerTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val testPassiveCaptchaParams = PassiveCaptchaParams(
        siteKey = "test_site_key",
        rqData = "test_rq_data"
    )
    private val testPublishableKey = "pk_test_123"
    private val testProductUsage = setOf("PaymentSheet")

    @Test
    fun `register should create activity result launcher with correct contract`() = runTest {
        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<PassiveChallengeWarmerContract>(),
                any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
            )
        ).thenReturn(mockLauncher)

        val warmer = DefaultPassiveChallengeWarmer()

        warmer.register(mockActivityResultCaller)

        verify(mockActivityResultCaller).registerForActivityResult(
            any<PassiveChallengeWarmerContract>(),
            any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
        )
    }

    @Test
    fun `start should launch with correct args when registered`() = runTest {
        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()
        val argsCaptor = argumentCaptor<PassiveChallengeWarmerContract.Args>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<PassiveChallengeWarmerContract>(),
                any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
            )
        ).thenReturn(mockLauncher)

        val warmer = DefaultPassiveChallengeWarmer()

        warmer.register(mockActivityResultCaller)

        warmer.start(
            passiveCaptchaParams = testPassiveCaptchaParams,
            publishableKey = testPublishableKey,
            productUsage = testProductUsage
        )

        verify(mockLauncher).launch(argsCaptor.capture())
        val capturedArgs = argsCaptor.firstValue
        assertThat(capturedArgs.passiveCaptchaParams).isEqualTo(testPassiveCaptchaParams)
        assertThat(capturedArgs.publishableKey).isEqualTo(testPublishableKey)
        assertThat(capturedArgs.productUsage).isEqualTo(testProductUsage)
    }

    @Test
    fun `unregister should unregister launcher and clear launcher reference`() = runTest {
        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<PassiveChallengeWarmerContract>(),
                any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
            )
        ).thenReturn(mockLauncher)

        val warmer = DefaultPassiveChallengeWarmer()

        warmer.register(mockActivityResultCaller)

        // Call unregister
        warmer.unregister()

        // Should unregister the launcher
        verify(mockLauncher).unregister()

        // Starting after unregister should not launch
        warmer.start(
            passiveCaptchaParams = testPassiveCaptchaParams,
            publishableKey = testPublishableKey,
            productUsage = testProductUsage
        )

        // Should not launch anything since launcher was cleared
        verify(mockLauncher, never()).launch(any())
    }

    @Test
    fun `multiple register calls should unregister previous launcher`() = runTest {
        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val firstMockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()
        val secondMockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<PassiveChallengeWarmerContract>(),
                any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
            )
        ).thenReturn(firstMockLauncher, secondMockLauncher)

        val warmer = DefaultPassiveChallengeWarmer()

        // Register first time
        warmer.register(mockActivityResultCaller)

        // Register second time (should unregister first launcher)
        warmer.register(mockActivityResultCaller)

        // First launcher should have been unregistered
        verify(firstMockLauncher).unregister()

        // Start should use the latest launcher
        warmer.start(
            passiveCaptchaParams = testPassiveCaptchaParams,
            publishableKey = testPublishableKey,
            productUsage = testProductUsage
        )

        verify(secondMockLauncher).launch(any())
        verify(firstMockLauncher, never()).launch(any())
    }
}
