@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ExpressCheckoutElementContent(
    interactor: ExpressCheckoutElementInteractor,
) {
    ExpressCheckoutElementContent(
        interactor = interactor,
        googlePayButton = { button, onPressed ->
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                allowCreditCards = button.allowCreditCards,
                buttonType = button.googlePayButtonType,
                billingAddressParameters = button.billingAddressParameters,
                isEnabled = true,
                cardBrandFilter = button.cardBrandFilter,
                cardFundingFilter = button.cardFundingFilter,
                additionalEnabledNetworks = button.additionalEnabledNetworks,
                onPressed = onPressed,
            )
        },
    )
}

@VisibleForTesting
@Composable
internal fun ExpressCheckoutElementContent(
    interactor: ExpressCheckoutElementInteractor,
    googlePayButton: @Composable (ExpressButton.GooglePay, () -> Unit) -> Unit,
) {
    val state by interactor.state.collectAsState()

    LaunchedEffect(Unit) {
        interactor.handleViewAction(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)
    }

    val visibleButtons = state.expressButtons.take(
        calculateVisibleButtonCount(
            buttonCount = state.expressButtons.size,
            maxColumns = state.buttonLayout.maxColumns,
            maxRows = state.buttonLayout.maxRows,
        )
    )
    val columnCount = calculateColumnCount(
        buttonCount = visibleButtons.size,
        maxColumns = state.buttonLayout.maxColumns,
        maxRows = state.buttonLayout.maxRows,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val buttonWidth = ((maxWidth - ButtonSpacing * (columnCount - 1)) / columnCount)
            .coerceAtLeast(0.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(ButtonSpacing),
        ) {
            visibleButtons.chunked(columnCount).forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = ButtonSpacing,
                        alignment = Alignment.CenterHorizontally,
                    ),
                ) {
                    rowButtons.forEach { button ->
                        Box(modifier = Modifier.width(buttonWidth)) {
                            ExpressButtonContent(
                                button = button,
                                interactor = interactor,
                                googlePayButton = googlePayButton,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val ButtonSpacing = 12.dp

private fun calculateVisibleButtonCount(
    buttonCount: Int,
    maxColumns: Int?,
    maxRows: Int?,
): Int {
    if (maxColumns == null || maxRows == null) {
        return buttonCount
    }

    val buttonCapacity = maxColumns.toLong() * maxRows.toLong()
    return minOf(buttonCapacity, buttonCount.toLong()).toInt()
}

private fun calculateColumnCount(
    buttonCount: Int,
    maxColumns: Int?,
    maxRows: Int?,
): Int {
    if (buttonCount == 0) {
        return 1
    }

    val configuredColumnCount = maxColumns
        ?: maxRows?.let { (buttonCount + it - 1) / it }
        ?: 1
    return minOf(configuredColumnCount, buttonCount)
}

@Composable
private fun ExpressButtonContent(
    button: ExpressButton,
    interactor: ExpressCheckoutElementInteractor,
    googlePayButton: @Composable (ExpressButton.GooglePay, () -> Unit) -> Unit,
) {
    key(button) {
        when (button) {
            is ExpressButton.GooglePay -> googlePayButton(button) {
                interactor.handleViewAction(
                    ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                        expressButton = button,
                    )
                )
            }
            is ExpressButton.Link -> LinkButton(
                state = button.state,
                enabled = true,
                theme = button.theme,
                linkBrand = button.linkBrand,
                onClick = {
                    interactor.handleViewAction(
                        ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                            expressButton = button,
                        )
                    )
                },
            )
        }
    }
}
