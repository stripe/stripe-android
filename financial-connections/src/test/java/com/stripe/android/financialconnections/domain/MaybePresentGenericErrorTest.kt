package com.stripe.android.financialconnections.domain

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import org.junit.Test

internal class MaybePresentGenericErrorTest {

    private val contentRepository = GenericErrorContentRepository(SavedStateHandle())
    private val navigationManager = TestNavigationManager()

    private val maybePresentGenericError = MaybePresentGenericError(
        contentRepository = contentRepository,
        navigationManager = navigationManager,
    )

    @Test
    fun `navigates to the generic error pane, replacing the pane that failed`() {
        val handled = maybePresentGenericError(
            error = errorWithGenericPane(),
            referrer = Pane.ACCOUNT_PICKER,
        )

        assertThat(handled).isTrue()
        navigationManager.assertNavigatedTo(
            destination = Destination.GenericError,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
    }

    @Test
    fun `stores the parsed pane for the screen to render`() {
        maybePresentGenericError(error = errorWithGenericPane(), referrer = Pane.ACCOUNT_PICKER)

        val pane = contentRepository.get()?.pane
        assertThat(pane?.heading).isEqualTo("There was a problem accessing your account")
        assertThat(pane?.primaryCta).isEqualTo("Try again")
        assertThat(pane?.primaryCtaAction).isEqualTo(PrimaryCtaAction.RestartAuthFlow)
    }

    @Test
    fun `leaves errors without a generic pane alone`() {
        val handled = maybePresentGenericError(
            error = InvalidRequestException(stripeError = StripeError(message = "Nope.")),
            referrer = Pane.ACCOUNT_PICKER,
        )

        assertThat(handled).isFalse()
        assertThat(navigationManager.emittedIntents).isEmpty()
        assertThat(contentRepository.get()).isNull()
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
