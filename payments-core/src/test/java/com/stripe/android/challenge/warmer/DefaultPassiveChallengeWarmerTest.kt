package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerCompleted
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerContract
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val lifecycleOwner = TestLifecycleOwner()

        warmer.register(mockActivityResultCaller, lifecycleOwner)

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
        val lifecycleOwner = TestLifecycleOwner()

        warmer.register(mockActivityResultCaller, lifecycleOwner)

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
    fun `onDestroy should unregister launcher and clear reference`() = runTest {
        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockLauncher = mock<ActivityResultLauncher<PassiveChallengeWarmerContract.Args>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                any<PassiveChallengeWarmerContract>(),
                any<ActivityResultCallback<PassiveChallengeWarmerCompleted>>()
            )
        ).thenReturn(mockLauncher)

        val warmer = DefaultPassiveChallengeWarmer()
        val lifecycleOwner = TestLifecycleOwner(
            coroutineDispatcher = UnconfinedTestDispatcher()
        )

        warmer.register(mockActivityResultCaller, lifecycleOwner)

        // Trigger lifecycle destroy
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Should unregister the launcher
        verify(mockLauncher).unregister()

        // Starting after destroy should not launch
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
        val firstLifecycleOwner = TestLifecycleOwner()
        val secondLifecycleOwner = TestLifecycleOwner()

        // Register first time
        warmer.register(mockActivityResultCaller, firstLifecycleOwner)

        // Register second time (should unregister first launcher)
        warmer.register(mockActivityResultCaller, secondLifecycleOwner)

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
