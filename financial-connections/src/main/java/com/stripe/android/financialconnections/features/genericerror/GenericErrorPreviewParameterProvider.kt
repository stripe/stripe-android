package com.stripe.android.financialconnections.features.genericerror

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.model.GenericErrorPane
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction

internal class GenericErrorPreviewParameterProvider : PreviewParameterProvider<GenericErrorPane> {
    override val values = sequenceOf(
        restartAuthFlow(),
        noImage(),
        unknownCtaAction(),
    )

    /**
     * Matches the design mock.
     */
    private fun restartAuthFlow() = canonical()

    /**
     * The image isn't always returned by the server.
     */
    private fun noImage() = canonical().copy(imageUrl = null)

    /**
     * An action we don't know how to handle falls back to "Select another bank".
     */
    private fun unknownCtaAction() = canonical().copy(primaryCtaAction = null)

    private fun canonical() = GenericErrorPane(
        heading = "There was a problem accessing your account",
        subheading = "Please try again and be sure to select **Profile information**.",
        primaryCta = "Try again",
        primaryCtaAction = PrimaryCtaAction.RestartAuthFlow,
        iconUrl = "$ASSETS_URL/BrandIcon--wellsfargo-4x.png",
        imageUrl = "$ASSETS_URL/ErrorAsset--ownership-wellsfargo-2x.png",
    )
}

private const val ASSETS_URL = "https://b.stripecdn.com/connections-statics-srv/assets"
