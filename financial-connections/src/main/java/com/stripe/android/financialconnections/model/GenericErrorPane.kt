package com.stripe.android.financialconnections.model

import Alignment
import FinancialConnectionsGenericInfoScreen.Header
import android.os.Parcelable
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.MarkdownParser
import kotlinx.parcelize.Parcelize

/**
 * A fully server-controlled error screen.
 *
 * Unlike every other error pane, the server owns all of the content here. The API error shape
 * isn't flexible enough to carry a screen definition, so the backend tucks it into the
 * `extra_fields` object of the error instead. Any endpoint can opt in by setting
 * `use_generic_error_pane`, so parsing is intentionally endpoint-agnostic.
 */
@Parcelize
internal data class GenericErrorPane(
    val heading: String,
    val subheading: String,
    val primaryCta: String,
    /**
     * `null` when the server sends an action this version of the SDK doesn't know how to handle.
     */
    val primaryCtaAction: PrimaryCtaAction?,
    val iconUrl: String?,
    val imageUrl: String?,
) : Parcelable {

    /**
     * What the primary button does.
     */
    internal enum class PrimaryCtaAction(val value: String) {
        /**
         * Create a new auth session and send the user back through the institution's OAuth flow.
         */
        RestartAuthFlow("restart_auth_flow"),
    }

    /**
     * Maps the text and icon onto the server-driven-UI header model, so the pane can be rendered by
     * the shared [GenericHeader] composable.
     *
     * The image isn't part of this: it gets the full-bleed prepane treatment rather than the plain
     * one the generic body applies, so the screen renders it separately from [imageUrl].
     */
    fun toHeader() = Header(
        title = heading,
        // The server sends markdown, but the renderer expects HTML. This conversion normally
        // happens during deserialization via MarkdownToHtmlSerializer, which we bypass by
        // building this model ourselves.
        subtitle = MarkdownParser.toHtml(subheading),
        icon = iconUrl?.let { Image(default = it) },
        alignment = Alignment.Center,
    )
}

/**
 * Extracts a [GenericErrorPane] from an API error, or returns `null` if the error doesn't opt into
 * one or is missing content we need to render it. In both cases callers fall back to the standard
 * error handling rather than showing a broken screen.
 *
 * Note that every `extra_fields` value arrives as a `String` regardless of its JSON type (see
 * `StripeErrorJsonParser`), so the opt-in flag is compared against `"true"`.
 */
internal fun Throwable.genericErrorPane(): GenericErrorPane? {
    val extraFields = (this as? StripeException)?.stripeError?.extraFields ?: return null

    if (extraFields[FIELD_USE_GENERIC_ERROR_PANE] != true.toString()) return null

    val heading = extraFields[FIELD_HEADING] ?: return null
    val subheading = extraFields[FIELD_SUBHEADING] ?: return null
    val primaryCta = extraFields[FIELD_PRIMARY_CTA] ?: return null

    return GenericErrorPane(
        heading = heading,
        subheading = subheading,
        primaryCta = primaryCta,
        primaryCtaAction = GenericErrorPane.PrimaryCtaAction.entries.firstOrNull {
            it.value == extraFields[FIELD_PRIMARY_CTA_ACTION]
        },
        iconUrl = extraFields[FIELD_ICON_URL],
        imageUrl = extraFields[FIELD_IMAGE_URL],
    )
}

private const val FIELD_USE_GENERIC_ERROR_PANE = "use_generic_error_pane"
private const val FIELD_HEADING = "generic_error_pane_heading"
private const val FIELD_SUBHEADING = "generic_error_pane_subheading"
private const val FIELD_PRIMARY_CTA = "generic_error_pane_primary_cta"
private const val FIELD_PRIMARY_CTA_ACTION = "generic_error_pane_primary_cta_action"
private const val FIELD_ICON_URL = "generic_error_pane_icon_url"
private const val FIELD_IMAGE_URL = "generic_error_pane_image_url"
