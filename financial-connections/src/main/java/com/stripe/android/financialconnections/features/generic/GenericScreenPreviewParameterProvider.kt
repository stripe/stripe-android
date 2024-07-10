package com.stripe.android.financialconnections.features.generic

import Alignment
import FinancialConnectionsGenericInfoScreen
import FinancialConnectionsGenericInfoScreen.Body
import FinancialConnectionsGenericInfoScreen.Footer
import FinancialConnectionsGenericInfoScreen.Header
import Size
import VerticalAlignment
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class GenericScreenPreviewParameterProvider : PreviewParameterProvider<GenericScreenState> {

    override val values = sequenceOf(
        canonical(),
        twoButtons(),
        modal()
    )

    private fun canonical() = GenericScreenState(
        inModal = false,
        screen = FinancialConnectionsGenericInfoScreen(
            id = "sampleScreen1",
            header = header(),
            body = body(),
            footer = footer(),
            options = options()
        )
    )

    private fun twoButtons() = GenericScreenState(
        inModal = false,
        screen = FinancialConnectionsGenericInfoScreen(
            id = "sampleScreen1",
            header = header(),
            body = body(),
            footer = footer().copy(
                secondaryCta = Footer.GenericInfoAction(
                    id = "secondaryCta1",
                    label = "Secondary Action",
                    icon = null,
                )
            ),
            options = options()
        )
    )

    private fun modal() = canonical().copy(inModal = true)

    private fun options() = FinancialConnectionsGenericInfoScreen.Options(
        fullWidthContent = true,
        verticalAlignment = VerticalAlignment.Default
    )

    private fun footer() = Footer(
        disclaimer = "Sample Disclaimer",
        primaryCta = Footer.GenericInfoAction(
            id = "primaryCta1",
            label = "Primary Action",
            icon = null,
        ),
        secondaryCta = null,
        belowCta = null
    )

    private fun body() = Body(
        entries = listOf(
            Body.Entry.Text(
                id = "1",
                text = "Sample Text",
                alignment = Alignment.Center,
                size = Size.Medium
            )
        )
    )

    private fun header() = Header(
        title = "Sample Title",
        subtitle = "Sample Subtitle",
        icon = null,
        alignment = Alignment.Center
    )
}
