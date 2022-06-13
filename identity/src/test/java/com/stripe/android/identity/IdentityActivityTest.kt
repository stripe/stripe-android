package com.stripe.android.identity

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.navigation.IdentityFragmentFactory
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
import com.stripe.android.identity.utils.InjectableActivityScenario
import com.stripe.android.identity.utils.injectableActivityScenario
import com.stripe.android.identity.utils.isNavigatedUpTo
import com.stripe.android.identity.viewmodel.ConsentFragmentViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
internal class IdentityActivityTest {
    private val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
    private val mockAppSettingsOpenable = mock<AppSettingsOpenable>()
    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()
    private val mockIdentityViewModelFactory = mock<IdentityViewModel.IdentityViewModelFactory>()
    private val mockFallbackUrlLauncher = mock<FallbackUrlLauncher>()

    private val testIdentityFragmentFactory = IdentityFragmentFactory(
        mockCameraPermissionEnsureable,
        mockAppSettingsOpenable,
        mockVerificationFlowFinishable,
        IdentityScanViewModel.IdentityScanViewModelFactory(),
        IdentityUploadViewModel.FrontBackUploadViewModelFactory(mock()),
        ConsentFragmentViewModel.ConsentFragmentViewModelFactory(mock(), mock()),
        mockIdentityViewModelFactory,
        mockFallbackUrlLauncher
    )

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationArgs }.thenReturn(ARGS)
        on { identityFragmentFactory }.thenReturn(testIdentityFragmentFactory)
    }

    @Before
    fun setUpViewModelFactory() {
        whenever(mockIdentityViewModelFactory.create(IdentityViewModel::class.java)).thenReturn(
            mockIdentityViewModel
        )
    }

    @Test
    fun `clicking navigation button on consent finishes with Canceled`() {
        testActivity { _, toolBar, navUpButton, navController, injectableActivityScenario ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_close_24
            )

            navUpButton.callOnClick()

            assertThat(injectableActivityScenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(
                injectableActivityScenario.getResult().resultData.extras?.get(
                    IdentityVerificationSheet.VerificationFlowResult.EXTRA
                )
            ).isEqualTo(
                IdentityVerificationSheet.VerificationFlowResult.Canceled
            )
        }
    }

    @Test
    fun `clicking back button on consent finishes with Canceled`() {
        testActivity { identityActivity, toolBar, _, navController, injectableActivityScenario ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_close_24
            )

            identityActivity.onBackPressed()

            assertThat(injectableActivityScenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(
                injectableActivityScenario.getResult().resultData.extras?.get(
                    IdentityVerificationSheet.VerificationFlowResult.EXTRA
                )
            ).isEqualTo(
                IdentityVerificationSheet.VerificationFlowResult.Canceled
            )
        }
    }

    @Test
    fun `clicking navigation button on errorWithFailedReason finishes with Failed`() {
        testActivity { _, toolBar, navUpButton, navController, injectableActivityScenario ->
            val failedReason = Throwable()
            navController.navigate(
                R.id.errorFragment,
                bundleOf(
                    ErrorFragment.ARG_FAILED_REASON to failedReason
                )
            )

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.errorFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_close_24
            )

            navUpButton.callOnClick()

            assertThat(injectableActivityScenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)

            val flowResult = injectableActivityScenario.getResult().resultData.extras?.get(
                IdentityVerificationSheet.VerificationFlowResult.EXTRA
            )
            assertThat(flowResult).isInstanceOf(IdentityVerificationSheet.VerificationFlowResult.Failed::class.java)
            assertThat((flowResult as IdentityVerificationSheet.VerificationFlowResult.Failed).throwable).isEqualTo(
                failedReason
            )
        }
    }

    @Test
    fun `clicking back button on errorWithFailedReason finishes with Failed`() {
        testActivity { identityActivity, toolBar, _, navController, injectableActivityScenario ->
            val failedReason = Throwable()
            navController.navigate(
                R.id.errorFragment,
                bundleOf(
                    ErrorFragment.ARG_FAILED_REASON to failedReason
                )
            )

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.errorFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_close_24
            )

            identityActivity.onBackPressed()

            assertThat(injectableActivityScenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)

            val flowResult = injectableActivityScenario.getResult().resultData.extras?.get(
                IdentityVerificationSheet.VerificationFlowResult.EXTRA
            )
            assertThat(flowResult).isInstanceOf(IdentityVerificationSheet.VerificationFlowResult.Failed::class.java)
            assertThat((flowResult as IdentityVerificationSheet.VerificationFlowResult.Failed).throwable).isEqualTo(
                failedReason
            )
        }
    }

    @Test
    fun `consent, upload, confirmation - clicking navigation button navigates up with argument`() {
        testActivity { _, toolBar, navUpButton, navController, _ ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            navController.navigate(
                R.id.IDUploadFragment,
                bundleOf(
                    ARG_SHOULD_SHOW_TAKE_PHOTO to true,
                    ARG_SHOULD_SHOW_CHOOSE_PHOTO to true
                )
            )
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDUploadFragment)
            navController.navigate(R.id.confirmationFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.confirmationFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_arrow_back_24
            )

            navUpButton.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDUploadFragment)
            assertThat(navController.isNavigatedUpTo()).isEqualTo(true)
        }
    }

    @Test
    fun `consent, upload, confirmation - clicking back button navigates up with argument`() {
        testActivity { identityActivity, toolBar, _, navController, _ ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            navController.navigate(
                R.id.IDUploadFragment,
                bundleOf(
                    ARG_SHOULD_SHOW_TAKE_PHOTO to true,
                    ARG_SHOULD_SHOW_CHOOSE_PHOTO to true
                )
            )
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDUploadFragment)
            navController.navigate(R.id.confirmationFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.confirmationFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_arrow_back_24
            )

            identityActivity.onBackPressed()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDUploadFragment)
            assertThat(navController.isNavigatedUpTo()).isEqualTo(true)
        }
    }

    @Test
    fun `consent, scan, confirmation - clicking navigation button navigates up without argument`() {
        testActivity { _, toolBar, navUpButton, navController, _ ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            navController.navigate(R.id.IDScanFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDScanFragment)
            navController.navigate(R.id.confirmationFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.confirmationFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_arrow_back_24
            )

            navUpButton.callOnClick()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDScanFragment)
            assertThat(navController.isNavigatedUpTo()).isEqualTo(false)
        }
    }

    @Test
    fun `consent, scan, confirmation - clicking back button navigates up without argument`() {
        testActivity { identityActivity, toolBar, _, navController, _ ->
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.consentFragment)
            navController.navigate(R.id.IDScanFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDScanFragment)
            navController.navigate(R.id.confirmationFragment)
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.confirmationFragment)

            assertThat(
                Shadows.shadowOf(
                    toolBar.navigationIcon
                ).createdFromResId
            ).isEqualTo(
                R.drawable.ic_baseline_arrow_back_24
            )

            identityActivity.onBackPressed()

            assertThat(navController.currentDestination?.id).isEqualTo(R.id.IDScanFragment)
            assertThat(navController.isNavigatedUpTo()).isEqualTo(false)
        }
    }

    @Test
    fun `when activity is recreated finishes with result`() {
        injectableActivityScenario<IdentityActivity> {
            injectActivity {
                viewModelFactory = mockIdentityViewModelFactory
            }
        }.launch(
            IdentityVerificationSheetContract().createIntent(
                context = ApplicationProvider.getApplicationContext(),
                input = ARGS
            )
        ).onActivity {
            verify(mockIdentityViewModel).retrieveAndBufferVerificationPage()
            assertThat(it.supportFragmentManager.fragments.size).isEqualTo(1)
        }.recreate().onActivity {
            assertThat(it.supportFragmentManager.fragments.size).isEqualTo(0)
        }
    }

    private fun testActivity(
        testBlock:
            (IdentityActivity, Toolbar, ImageButton, NavController, InjectableActivityScenario<IdentityActivity>) -> Unit
    ) {
        val injectableActivityScenario = injectableActivityScenario<IdentityActivity> {
            injectActivity {
                viewModelFactory = mockIdentityViewModelFactory
            }
        }.launch(
            IdentityVerificationSheetContract().createIntent(
                context = ApplicationProvider.getApplicationContext(),
                input = ARGS
            )
        )
        injectableActivityScenario.onActivity { identityActivity ->
            val toolbar = identityActivity.findViewById<Toolbar>(R.id.top_app_bar)
            toolbar.navigationContentDescription = NAV_UP_BUTTON_CONTENT_DESCRIPTION
            val outViews = arrayListOf<View>()
            toolbar.findViewsWithText(
                outViews,
                NAV_UP_BUTTON_CONTENT_DESCRIPTION,
                View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION
            )

            testBlock(
                identityActivity,
                toolbar,
                outViews[0] as ImageButton,
                identityActivity.navController,
                injectableActivityScenario
            )
        }
    }

    private companion object {
        const val NAV_UP_BUTTON_CONTENT_DESCRIPTION = "navUpContentDescription"
        const val VERIFICATION_SESSION_ID = "id_1234"
        const val EAK = "eak_12345"
        val BRAND_LOGO: Uri = Uri.parse("logoUri")
        val ARGS = IdentityVerificationSheetContract.Args(
            VERIFICATION_SESSION_ID,
            EAK,
            BRAND_LOGO,
            DUMMY_INJECTOR_KEY
        )
    }
}

// A material themed application is needed to inflate MaterialToolbar in IdentityActivity
internal class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_MaterialComponents_DayNight_NoActionBar)
    }
}
