package com.stripe.android.financialconnections.model

import Alignment
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction
import org.junit.Test

internal class GenericErrorPaneTest {

    @Test
    fun `parses every field of a full payload`() {
        val pane = apiError().genericErrorPane()

        assertThat(pane?.heading).isEqualTo("There was a problem accessing your account")
        assertThat(pane?.subheading)
            .isEqualTo("Please try again and be sure to select **Profile information**.")
        assertThat(pane?.primaryCta).isEqualTo("Try again")
        assertThat(pane?.primaryCtaAction).isEqualTo(PrimaryCtaAction.RestartAuthFlow)
        assertThat(pane?.iconUrl).isEqualTo("https://b.stripecdn.com/icon.png")
        assertThat(pane?.imageUrl).isEqualTo("https://b.stripecdn.com/image.png")
    }

    @Test
    fun `returns null when the opt-in flag is absent`() {
        val error = apiError(overrides = mapOf("use_generic_error_pane" to null))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `returns null when the opt-in flag is explicitly false`() {
        val error = apiError(overrides = mapOf("use_generic_error_pane" to "false"))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `returns null when there are no extra fields at all`() {
        val error = InvalidRequestException(stripeError = StripeError(message = "Nope."))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `returns null for an error that didn't come from the API`() {
        assertThat(IllegalStateException("Nope.").genericErrorPane()).isNull()
    }

    @Test
    fun `returns null when the heading is missing`() {
        val error = apiError(overrides = mapOf("generic_error_pane_heading" to null))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `returns null when the subheading is missing`() {
        val error = apiError(overrides = mapOf("generic_error_pane_subheading" to null))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `returns null when the primary CTA is missing`() {
        val error = apiError(overrides = mapOf("generic_error_pane_primary_cta" to null))

        assertThat(error.genericErrorPane()).isNull()
    }

    @Test
    fun `an unknown CTA action still renders a pane, with no action`() {
        val error = apiError(
            overrides = mapOf("generic_error_pane_primary_cta_action" to "some_future_action")
        )

        val pane = error.genericErrorPane()

        assertThat(pane).isNotNull()
        assertThat(pane?.primaryCtaAction).isNull()
        assertThat(pane?.heading).isEqualTo("There was a problem accessing your account")
    }

    @Test
    fun `a missing CTA action parses as no action`() {
        val error = apiError(
            overrides = mapOf("generic_error_pane_primary_cta_action" to null)
        )

        assertThat(error.genericErrorPane()?.primaryCtaAction).isNull()
    }

    @Test
    fun `the icon and image are both optional`() {
        val error = apiError(
            overrides = mapOf(
                "generic_error_pane_icon_url" to null,
                "generic_error_pane_image_url" to null,
            )
        )

        val pane = error.genericErrorPane()

        assertThat(pane).isNotNull()
        assertThat(pane?.iconUrl).isNull()
        assertThat(pane?.imageUrl).isNull()
    }

    @Test
    fun `mapping to a header converts markdown in the subheading to html`() {
        val header = requireNotNull(apiError().genericErrorPane()).toHeader()

        assertThat(header.subtitle)
            .isEqualTo("Please try again and be sure to select <b>Profile information</b>.")
    }

    @Test
    fun `mapping to a header centers it and carries the icon`() {
        val header = requireNotNull(apiError().genericErrorPane()).toHeader()

        assertThat(header.title).isEqualTo("There was a problem accessing your account")
        assertThat(header.alignment).isEqualTo(Alignment.Center)
        assertThat(header.icon?.default).isEqualTo("https://b.stripecdn.com/icon.png")
    }

    @Test
    fun `mapping to a header leaves the icon out when the server sends none`() {
        val error = apiError(overrides = mapOf("generic_error_pane_icon_url" to null))

        assertThat(requireNotNull(error.genericErrorPane()).toHeader().icon).isNull()
    }

    /**
     * Builds the exception the SDK produces for a failed API request carrying a generic error pane.
     * A `null` override drops that key, standing in for a field the server didn't send.
     */
    private fun apiError(
        overrides: Map<String, String?> = emptyMap()
    ): InvalidRequestException {
        val extraFields = mutableMapOf(
            "reason" to "missing_required_data",
            "use_generic_error_pane" to "true",
            "generic_error_pane_heading" to "There was a problem accessing your account",
            "generic_error_pane_subheading" to
                "Please try again and be sure to select **Profile information**.",
            "generic_error_pane_primary_cta" to "Try again",
            "generic_error_pane_primary_cta_action" to "restart_auth_flow",
            "generic_error_pane_icon_url" to "https://b.stripecdn.com/icon.png",
            "generic_error_pane_image_url" to "https://b.stripecdn.com/image.png",
        )

        overrides.forEach { (key, value) ->
            if (value == null) extraFields.remove(key) else extraFields[key] = value
        }

        return InvalidRequestException(
            stripeError = StripeError(
                message = "Required data permissions were not granted.",
                type = "invalid_request_error",
                extraFields = extraFields,
            ),
            statusCode = 400,
        )
    }
}
