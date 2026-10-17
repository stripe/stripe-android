package com.stripe.android.financialconnections.domain

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.FinancialConnectionsErrorRepository
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class RealHandleErrorTest {

    private val errorRepository = FinancialConnectionsErrorRepository(SavedStateHandle())
    private val genericErrorContentRepository = GenericErrorContentRepository(SavedStateHandle())
    private val navigationManager = TestNavigationManager()
    private val analyticsTracker = TestFinancialConnectionsAnalyticsTracker()

    private val handleError = RealHandleError(
        errorRepository = errorRepository,
        analyticsTracker = analyticsTracker,
        nativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
        logger = Logger.noop(),
        navigationManager = navigationManager,
        maybePresentGenericError = MaybePresentGenericError(
            contentRepository = genericErrorContentRepository,
            navigationManager = navigationManager,
        ),
    )

    @Test
    fun `a server-driven pane takes precedence over the SDK's own error screen`() = runTest {
        handleError(
            extraMessage = "Error retrieving accounts",
            error = errorWithGenericPane(),
            pane = Pane.ACCOUNT_PICKER,
            displayErrorScreen = true,
        )

        navigationManager.assertNavigatedTo(
            destination = Destination.GenericError,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
        assertThat(errorRepository.get()).isNull()
    }

    @Test
    fun `a plain error still goes to the SDK's own error screen`() = runTest {
        val error = InvalidRequestException(stripeError = StripeError(message = "Nope."))

        handleError(
            extraMessage = "Error retrieving accounts",
            error = error,
            pane = Pane.ACCOUNT_PICKER,
            displayErrorScreen = true,
        )

        navigationManager.assertNavigatedTo(
            destination = Destination.Error,
            pane = Pane.ACCOUNT_PICKER,
        )
        assertThat(errorRepository.get()?.error).isEqualTo(error)
    }

    @Test
    fun `nothing is presented when the caller doesn't want an error screen`() = runTest {
        handleError(
            extraMessage = "Error retrieving accounts",
            error = errorWithGenericPane(),
            pane = Pane.ACCOUNT_PICKER,
            displayErrorScreen = false,
        )

        assertThat(navigationManager.emittedIntents).isEmpty()
        assertThat(genericErrorContentRepository.get()).isNull()
    }

    private fun errorWithGenericPane() = InvalidRequestException(
        stripeError = StripeError(
            extraFields = mapOf(
                "use_generic_error_pane" to "true",
                "generic_error_pane_heading" to "There was a problem accessing your account",
                "generic_error_pane_subheading" to "Please try again.",
                "generic_error_pane_primary_cta" to "Try again",
                "generic_error_pane_primary_cta_action" to "restart_auth_flow",
            )
        ),
        statusCode = 400,
    )
}
