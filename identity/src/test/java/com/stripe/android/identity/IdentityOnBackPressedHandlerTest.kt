package com.stripe.android.identity

import android.os.Build
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.ConsentDestination.CONSENT
import com.stripe.android.identity.navigation.DocSelectionDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_SHOULD_FAIL
import com.stripe.android.identity.navigation.InitialLoadingDestination
import com.stripe.android.identity.navigation.OTPDestination
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class IdentityOnBackPressedHandlerTest {
    private val mockFlowFinishable = mock<VerificationFlowFinishable>()
    private val mockNavController = mock<NavController>()
    private val mockAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()
    private val errorCause = MutableLiveData<Throwable>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory } doReturn mockAnalyticsRequestFactory
        on { verificationPage } doReturn mock()
        on { errorCause } doReturn errorCause
    }

    private val handler = IdentityOnBackPressedHandler(
        ApplicationProvider.getApplicationContext(),
        mockFlowFinishable,
        mockNavController,
        mockIdentityViewModel
    )

    @Test
    fun testBackPressWhenSubmitting() {
        whenever(mockIdentityViewModel.isSubmitting()).thenReturn(true)
        handler.handleOnBackPressed()

        verifyNoInteractions(mockFlowFinishable)
        verifyNoInteractions(mockNavController)
        verifyNoInteractions(mockNavController)
    }

    @Test
    fun testBackPressOnFirstPage() {
        val loadingNavBackStackEntry = mock<NavBackStackEntry> {
            on { destination } doReturn NavDestination("").also {
                it.route = InitialLoadingDestination.ROUTE.route
            }
        }

        val mockDestination = mock<NavDestination> {
            on { route } doReturn CONSENT
        }

        handler.updateState(
            destination = mockDestination,
            args = null
        )

        whenever(mockNavController.previousBackStackEntry).thenReturn(
            loadingNavBackStackEntry
        )

        handler.handleOnBackPressed()

        verify(mockAnalyticsRequestFactory).verificationCanceled(
            eq(false),
            eq(CONSENT.routeToScreenName()),
            anyOrNull(),
            anyOrNull()
        )
        verify(mockFlowFinishable).finishWithResult(
            eq(IdentityVerificationSheet.VerificationFlowResult.Canceled)
        )
    }

    @Test
    fun testBackPressOnConfirmationPage() {
        val mockDestination = mock<NavDestination> {
            on { route } doReturn ConfirmationDestination.ROUTE.route
        }

        handler.updateState(
            destination = mockDestination,
            args = null
        )

        handler.handleOnBackPressed()
        verify(mockIdentityViewModel).sendSucceededAnalyticsRequestForNative()
        verify(mockFlowFinishable).finishWithResult(eq(IdentityVerificationSheet.VerificationFlowResult.Completed))
    }

    @Test
    fun testBackPressOnConsentPageWithTypeDocument() {
        val initialNavBackStackEntry = mock<NavBackStackEntry> {
            on { destination } doReturn NavDestination("").also {
                it.route = InitialLoadingDestination.ROUTE.route
            }
        }

        whenever(mockNavController.previousBackStackEntry).thenReturn(
            initialNavBackStackEntry
        )

        val mockDestination = mock<NavDestination> {
            on { route } doReturn ConsentDestination.ROUTE.route
        }

        handler.updateState(
            destination = mockDestination,
            args = null
        )

        handler.handleOnBackPressed()

        verify(mockAnalyticsRequestFactory).verificationCanceled(
            eq(false),
            eq(CONSENT.routeToScreenName()),
            anyOrNull(),
            anyOrNull()
        )
        verify(mockFlowFinishable).finishWithResult(
            eq(IdentityVerificationSheet.VerificationFlowResult.Canceled)
        )
    }

    @Test
    fun testBackPressOnConsentPageWithTypePhoneV() {
        val initialNavBackStackEntry = mock<NavBackStackEntry> {
            on { destination } doReturn NavDestination("").also {
                it.route = OTPDestination.ROUTE.route
            }
        }

        whenever(mockNavController.previousBackStackEntry).thenReturn(
            initialNavBackStackEntry
        )

        val mockDestination = mock<NavDestination> {
            on { route } doReturn ConsentDestination.ROUTE.route
        }

        handler.updateState(
            destination = mockDestination,
            args = null
        )

        handler.handleOnBackPressed()

        verifyNoMoreInteractions(mockFlowFinishable)

        val cancelDialog = ShadowAlertDialog.getShownDialogs().first()
        assertNotNull(cancelDialog)
    }

    @Test
    fun testBackPressOnErrorPageWithArgShouldFail() {
        val mockDestination = mock<NavDestination> {
            on { route } doReturn ErrorDestination.ROUTE.route
        }

        val exception = Exception("test")
        errorCause.value = exception

        handler.updateState(
            destination = mockDestination,
            args = bundleOf(
                ARG_SHOULD_FAIL to true
            )
        )

        handler.handleOnBackPressed()

        verify(mockAnalyticsRequestFactory).verificationFailed(
            eq(false),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(exception)
        )
        verify(mockFlowFinishable).finishWithResult(
            argWhere {
                it is IdentityVerificationSheet.VerificationFlowResult.Failed && it.throwable == exception
            }
        )
    }

    @Test
    fun testBackPressOnErrorPageWithArgShouldNotFail() {
        val mockDestination = mock<NavDestination> {
            on { route } doReturn ErrorDestination.ROUTE.route
        }

        handler.updateState(
            destination = mockDestination,
            args = bundleOf(
                ARG_SHOULD_FAIL to false
            )
        )
        handler.handleOnBackPressed()
        verify(mockNavController).navigateUp()
    }

    @Test
    fun testBackPressOnOtherScreen() {
        val mockDestination = mock<NavDestination> {
            on { route } doReturn DocSelectionDestination.ROUTE.route
        }

        handler.updateState(
            destination = mockDestination,
            args = null
        )
        handler.handleOnBackPressed()
        verify(mockNavController).navigateUp()
    }
}
